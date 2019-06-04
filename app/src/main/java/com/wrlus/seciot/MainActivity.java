package com.wrlus.seciot;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.wrlus.seciot.agent.FridaServerAgent;
import com.wrlus.seciot.agent.FrpcAgent;
import com.wrlus.seciot.agent.SecIoTAgent;
import com.wrlus.seciot.agent.StatusCallback;
import com.wrlus.seciot.model.BaseResponse;
import com.wrlus.seciot.model.PortResponse;
import com.wrlus.seciot.model.VersionResponse;
import com.wrlus.seciot.msg.Msg;
import com.wrlus.seciot.util.DeviceHelper;
import com.wrlus.seciot.util.RootShellHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements Handler.Callback, ProgressCallback {
    private Handler handler = new Handler(this);
    private Switch switchStatus;
    private ImageView imageStatus;
    private TextView textViewFridaVersion, textViewFrpVersion, textViewAndroidVer, textViewDeviceName, textViewStructure;
    private Button btnFridaManage, btnFrpcManage;
    private FridaServerAgent fridaServerAgent = FridaServerAgent.getInstance();
    private FrpcAgent frpcAgent = FrpcAgent.getInstance();
    private SecIoTAgent secIoTAgent = SecIoTAgent.getInstance();
    private String abi = "Unknown";
    private String fridaVersion = "Unknown", frpVersion = "Unknown";
    private boolean isFridaServerInstalled = false, isFrpcInstalled = false, isFridaServerStarted = false, isFrpcStarted = false;
    private int port = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindWidget();
        bindWidgetEvent();
        getSystemInfo();
        getFridaVersionOnServer();
        getFrpsVersionOnServer();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        if (item.getItemId() == R.id.btnRefresh) {
            checkAll();
        } else if (item.getItemId() == R.id.btnSettings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.btnAbout) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle(R.string.about);
            dialog.setMessage(R.string.gplv2);
            dialog.setNegativeButton("关闭", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            dialog.show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        configureServer();
        configureFrpsIp();
        checkAll();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RootShellHelper rootShellHelper = RootShellHelper.getInstance();
        try {
            rootShellHelper.exit();
        } catch (IOException e) {
            e.printStackTrace();
        }
        updateDevice(false);
    }

    public void bindWidget() {
        switchStatus = findViewById(R.id.switchStatus);
        imageStatus = findViewById(R.id.imageStatus);
        textViewFridaVersion = findViewById(R.id.textViewFridaVersion);
        textViewFrpVersion = findViewById(R.id.textViewFrpVersion);
        textViewAndroidVer = findViewById(R.id.textViewAndroidVer);
        textViewDeviceName = findViewById(R.id.textViewDeviceName);
        textViewStructure = findViewById(R.id.textViewStructure);
        btnFridaManage = findViewById(R.id.btnFridaManage);
        btnFrpcManage = findViewById(R.id.btnFrpcManage);
        imageStatus.setImageResource(R.mipmap.status_error);
    }

    public void bindWidgetEvent() {
        switchStatus.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (!isFridaServerStarted) {
                        startFrida();
                    }
                    if (!isFrpcStarted) {
                        startFrpc();
                    }
                } else {
                    if (isFridaServerStarted) {
                        stopFrida();
                    }
                    if (isFrpcStarted) {
                        stopFrpc();
                    }
                }
            }
        });
        btnFridaManage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (fridaVersion.equals("Unknown")) {
                    Toast.makeText(MainActivity.this, "frida版本未知，请刷新后重试。", Toast.LENGTH_SHORT).show();
                    return;
                }
                List<String> manageFridaAction = new ArrayList<>();
                manageFridaAction.add("安装 frida server "+fridaVersion+"-"+abi);
                if (isFridaServerInstalled) {
                    manageFridaAction.add("卸载 frida server "+fridaVersion+"-"+abi);
                }
                AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                dialog.setTitle("管理 frida server");
                dialog.setItems(manageFridaAction.toArray(new String[0]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                downloadFridaServer();
                                break;
                            case 1:
                                removeFrida();
                                break;
                            default:
                                break;
                        }
                    }
                });
                dialog.show();
            }
        });
        btnFrpcManage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (frpVersion.equals("Unknown")) {
                    Toast.makeText(MainActivity.this, "frp版本未知，请刷新后重试。", Toast.LENGTH_SHORT).show();
                    return;
                }
                List<String> manageFridaAction = new ArrayList<>();
                manageFridaAction.add("安装 frp client "+frpVersion+"-"+abi);
                if (isFrpcInstalled) {
                    manageFridaAction.add("卸载 frp client "+frpVersion+"-"+abi);
                }
                AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                dialog.setTitle("管理 frp client");
                dialog.setItems(manageFridaAction.toArray(new String[0]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                downloadFrpc();
                                break;
                            case 1:
                                removeFrpc();
                                break;
                            default:
                                break;
                        }
                    }
                });
                dialog.show();
            }
        });
    }

    public void checkAll() {
        if (fridaVersion.equals("Unknown")) {
            getFridaVersionOnServer();
        }
        if (frpVersion.equals("Unknown")) {
            getFrpsVersionOnServer();
        }
        checkFridaInstallation();
        checkFrpcInstallation();
    }

    public void checkFridaInstallation() {
        if (fridaServerAgent.checkFridaServerInstallation(fridaVersion)) {
            String fridaReadyString = getString(R.string.frida_ready);
            fridaReadyString = String.format(fridaReadyString, fridaVersion, abi);
            textViewFridaVersion.setText(fridaReadyString);
            setProgress(R.id.progressBarFridaInstall, 1);
            isFridaServerInstalled = true;
            if (isFrpcInstalled) {
                imageStatus.setImageResource(R.mipmap.status_success);
                switchStatus.setEnabled(true);
            }
        } else {
            String fridaMissingString = getString(R.string.frida_missing);
            fridaMissingString = String.format(fridaMissingString, fridaVersion, abi);
            textViewFridaVersion.setText(fridaMissingString);
            setProgress(R.id.progressBarFridaInstall, 0);
            isFridaServerInstalled = false;
            imageStatus.setImageResource(R.mipmap.status_error);
            switchStatus.setEnabled(false);
        }
    }

    public void checkFrpcInstallation() {
        if (frpcAgent.checkFrpcInstallation(frpVersion)) {
            String frpReadyString = getString(R.string.frp_ready);
            frpReadyString = String.format(frpReadyString, frpVersion, abi);
            textViewFrpVersion.setText(frpReadyString);
            this.setProgress(R.id.progressBarFrpInstall, 1);
            isFrpcInstalled = true;
            if (isFridaServerInstalled) {
                imageStatus.setImageResource(R.mipmap.status_success);
                switchStatus.setEnabled(true);
            }
        } else {
            String frpMissingString = getString(R.string.frp_missing);
            frpMissingString = String.format(frpMissingString, frpVersion, abi);
            textViewFrpVersion.setText(frpMissingString);
            this.setProgress(R.id.progressBarFrpInstall, 0);
            isFrpcInstalled = false;
            imageStatus.setImageResource(R.mipmap.status_error);
            switchStatus.setEnabled(false);
        }
    }

    public String getClientId() {
        SharedPreferences sharedPref = getSharedPreferences("com.wrlus.seciot", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        String clientId = sharedPref.getString("client_id", "Undefined");
        if (clientId == null || clientId.equals("Undefined")) {
            clientId = UUID.randomUUID().toString();
            editor.putString("client_id", clientId);
            editor.apply();
        }
        return clientId;
    }

    public void configureServer() {
        SharedPreferences sharedPref = getSharedPreferences("com.wrlus.seciot", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        String serverUrl = sharedPref.getString("server_url", "Undefined");
        if (serverUrl == null || serverUrl.equals("Undefined")) {
            serverUrl = "http://140.143.53.29:8080/SecIoT";
            editor.putString("server_url", serverUrl);
            editor.apply();
        }
        fridaServerAgent.setAgentServer(serverUrl);
        frpcAgent.setAgentServer(serverUrl);
        secIoTAgent.setAgentServer(serverUrl);
    }

    public void configureFrpsIp() {
        SharedPreferences sharedPref = getSharedPreferences("com.wrlus.seciot", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        String frpsIp = sharedPref.getString("frps_ip", "Undefined");
        if (frpsIp == null || frpsIp.equals("Undefined")) {
            frpsIp = "140.143.53.29";
            editor.putString("frps_ip", frpsIp);
            editor.apply();
        }
        frpcAgent.setFrpsServer(frpsIp);
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case Msg.GET_FRIDA_VERSION_SUCCESS:
                fridaVersion = (String) msg.obj;
                Log.i("FridaVersion", fridaVersion);
                checkAll();
                break;
            case Msg.GET_FRIDA_VERSION_FAILED:
                textViewFridaVersion.setText(R.string.frida_unavaliable);
                Toast.makeText(MainActivity.this, "无法连接到服务器，请检查网络设置。", Toast.LENGTH_SHORT).show();
                this.setProgress(R.id.progressBarFridaInstall, 0);
                break;
            case Msg.GET_FRP_VERSION_SUCCESS:
                frpVersion = (String) msg.obj;
                Log.i("FrpVerion", frpVersion);
                checkAll();
                break;
            case Msg.GET_FRP_VERSION_FAILED:
                textViewFrpVersion.setText(R.string.frp_unavaliable);
                Toast.makeText(MainActivity.this, "无法连接到服务器，请检查网络设置。", Toast.LENGTH_SHORT).show();
                setProgress(R.id.progressBarFrpInstall, 0);
                break;
            case Msg.DOWNLOAD_FRIDA_SUCCESS:
                setProgress(R.id.progressBarFridaInstall, 0.5);
                installFrida( (File) msg.obj);
                break;
            case Msg.DOWNLOAD_FRIDA_FAILED:
                Toast.makeText(MainActivity.this, "无法连接到服务器，请检查网络设置。", Toast.LENGTH_SHORT).show();
                break;
            case Msg.DOWNLOAD_FRP_SUCCESS:
                setProgress(R.id.progressBarFrpInstall, 0.33);
                installFrpc( (File) msg.obj);
                break;
            case Msg.DOWNLOAD_FRP_FAILED:
                Toast.makeText(MainActivity.this, "无法连接到服务器，请检查网络设置。", Toast.LENGTH_SHORT).show();
                break;
            case Msg.BIND_REMOTE_PORT_SUCCESS:
                port = (Integer) msg.obj;
                realStartFrpc( (Integer) msg.obj);
                break;
            case Msg.BIND_REMOTE_PORT_FAILED:
                port = 0;
                Toast.makeText(MainActivity.this, "无法连接到服务器，请检查网络设置。", Toast.LENGTH_SHORT).show();
                break;
            case Msg.GET_REMOTE_PORT_SUCCESS:
                port = (Integer) msg.obj;
                realStartFrpc( (Integer) msg.obj);
                break;
            case Msg.GET_REMOTE_PORT_FAILED:
                port = 0;
                bindRemotePort();
                break;
            case Msg.UNBIND_REMOTE_PORT_SUCCESS:
                port = 0;
                realStopFrpc();
                break;
            case Msg.UNBIND_REMOTE_PORT_FAILED:
                Toast.makeText(MainActivity.this, "无法连接到服务器，请检查网络设置。", Toast.LENGTH_SHORT).show();
                break;
            case Msg.ADD_DEVICE_SUCCESS:
                break;
            case Msg.ADD_DEVICE_FAILED:
                Toast.makeText(MainActivity.this, "无法连接到服务器，请检查网络设置。", Toast.LENGTH_SHORT).show();
                break;
            case Msg.UPDATE_DEVICE_SUCCESS:
                break;
            case Msg.UPDATE_DEVICE_FAILED:
                addDevice();
                break;
            default:
                Log.e("MessageHandler", "Unknown message: msg.what = "+msg.what);
        }
        return true;
    }

    public void getSystemInfo() {
        String androidVerString = getString(R.string.android_ver);
        androidVerString = String.format(androidVerString, DeviceHelper.getAndroidVersion(), DeviceHelper.getAPILevel());
        textViewAndroidVer.setText(androidVerString);

        String deviceNameString = getString(R.string.device_name);
        deviceNameString = String.format(deviceNameString, DeviceHelper.getProductName());
        textViewDeviceName.setText(deviceNameString);

        String[] abis = DeviceHelper.getSupportedAbis();
        String abi = abis[0];
        String deviceAbiString = getString(R.string.device_abi);
        deviceAbiString = String.format(deviceAbiString, abi);
        textViewStructure.setText(deviceAbiString);
        if (abi.contains("arm64")) {
            this.abi = "arm64";
        } else if (abi.contains("arm")) {
            this.abi = "arm";
        } else if (abi.contains("x86_64")) {
            this.abi = "x86_64";
        } else if (abi.contains("x86")) {
            this.abi = "x86";
        } else {
            Toast.makeText(this, "Unknown product cpu abi: "+abi, Toast.LENGTH_SHORT).show();
        }
    }

    public void getFridaVersionOnServer() {
        fridaServerAgent.getFridaVersionOnServer(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = handler.obtainMessage(Msg.GET_FRIDA_VERSION_FAILED, e);
                handler.sendMessage(msg);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String res = response.body().string();
                    Gson gson = new Gson();
                    VersionResponse versionResponse = gson.fromJson(res, VersionResponse.class);
                    if (versionResponse.getStatus() == 0) {
                        Message msg = handler.obtainMessage(Msg.GET_FRIDA_VERSION_SUCCESS, versionResponse.getVersion());
                        handler.sendMessage(msg);
                    } else {
                        Message msg = handler.obtainMessage(Msg.GET_FRIDA_VERSION_FAILED);
                        handler.sendMessage(msg);
                    }
                } else {
                    Message msg = handler.obtainMessage(Msg.GET_FRIDA_VERSION_FAILED);
                    handler.sendMessage(msg);
                }
            }
        });
    }

    public void getFrpsVersionOnServer() {
        frpcAgent.getFrpsVersionOnServer(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = handler.obtainMessage(Msg.GET_FRP_VERSION_FAILED, e);
                handler.sendMessage(msg);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String res = response.body().string();
                    Gson gson = new Gson();
                    VersionResponse versionResponse = gson.fromJson(res, VersionResponse.class);
                    if (versionResponse.getStatus() == 0) {
                        Message msg = handler.obtainMessage(Msg.GET_FRP_VERSION_SUCCESS, versionResponse.getVersion());
                        handler.sendMessage(msg);
                    } else {
                        Message msg = handler.obtainMessage(Msg.GET_FRP_VERSION_FAILED);
                        handler.sendMessage(msg);
                    }
                } else {
                    Message msg = handler.obtainMessage(Msg.GET_FRP_VERSION_FAILED);
                    handler.sendMessage(msg);
                }
            }
        });
    }

    public void downloadFridaServer() {
        fridaServerAgent.downloadFridaServer(fridaVersion, abi, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = handler.obtainMessage(Msg.DOWNLOAD_FRIDA_FAILED, e);
                handler.sendMessage(msg);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    InputStream is = response.body().byteStream();
                    File dlFile = new File(MainActivity.this.getCacheDir().getAbsolutePath()
                            + "/frida-server-"+fridaVersion+"-android-"+abi+".tar.gz");
                    FileOutputStream fos = new FileOutputStream(dlFile);
                    int len;
                    byte[] buffer = new byte[4096];
                    while (-1 != (len = is.read(buffer))) {
                        fos.write(buffer, 0, len);
                        fos.flush();
                    }
                    is.close();
                    fos.close();
                    Message msg = handler.obtainMessage(Msg.DOWNLOAD_FRIDA_SUCCESS, dlFile);
                    handler.sendMessage(msg);
                } else {
                    Message msg = handler.obtainMessage(Msg.DOWNLOAD_FRIDA_FAILED);
                    handler.sendMessage(msg);
                }
            }
        });
    }

    public void installFrida(File downloadFile) {
        fridaServerAgent.installFridaServer(downloadFile, fridaVersion, new StatusCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        checkAll();
                        Toast.makeText(MainActivity.this, "frida server 安装成功", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(int exitCode, Exception e) {
                Log.e("InstallFridaServer", String.valueOf(exitCode));
                if (e != null) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setProgress(R.id.progressBarFridaInstall, 0);
                        Toast.makeText(MainActivity.this, "frida server 安装失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    public void startFrida() {
        fridaServerAgent.startFridaServer(fridaVersion);
        isFridaServerStarted = true;
    }

    public void stopFrida() {
        fridaServerAgent.stopFridaServer();
        isFridaServerStarted = false;
    }

    public void removeFrida() {
        fridaServerAgent.removeFridaServer(fridaVersion, new StatusCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        checkAll();
                        Toast.makeText(MainActivity.this, "frida server 卸载成功", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(int exitCode, Exception e) {
                Log.e("RemoveFridaServer", String.valueOf(exitCode));
                if (e != null) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "frida server 卸载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    public void downloadFrpc() {
        frpcAgent.downloadFrp(frpVersion, abi, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = handler.obtainMessage(Msg.DOWNLOAD_FRP_FAILED, e);
                handler.sendMessage(msg);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    InputStream is = response.body().byteStream();
                    File dlFile = new File(MainActivity.this.getCacheDir().getAbsolutePath()
                            + "/frp_"+frpVersion+"_linux_"+abi+".tar.gz");
                    FileOutputStream fos = new FileOutputStream(dlFile);
                    int len;
                    byte[] buffer = new byte[4096];
                    while (-1 != (len = is.read(buffer))) {
                        fos.write(buffer, 0, len);
                        fos.flush();
                    }
                    is.close();
                    fos.close();
                    Message msg = handler.obtainMessage(Msg.DOWNLOAD_FRP_SUCCESS, dlFile);
                    handler.sendMessage(msg);
                } else {
                    Message msg = handler.obtainMessage(Msg.DOWNLOAD_FRP_FAILED);
                    handler.sendMessage(msg);
                }
            }
        });

    }

    public void installFrpc(File downloadFile) {
        frpcAgent.installFrpc(downloadFile, frpVersion, new StatusCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        checkAll();
                        setProgress(R.id.progressBarFrpInstall, 1);
                        Toast.makeText(MainActivity.this, "frp client 安装成功", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(int exitCode, Exception e) {
                Log.e("InstallFrpc", String.valueOf(exitCode));
                if (e != null) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setProgress(R.id.progressBarFrpInstall, 0);
                        Toast.makeText(MainActivity.this, "frp client 安装失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    public void startFrpc() {
        getRemotePort();
    }

    public void realStartFrpc(int port) {
        frpcAgent.startFrpc(this, frpVersion, port);
        isFrpcStarted = true;
        updateDevice(true);
    }

    public void stopFrpc() {
        unBindRemotePort();
    }

    public void realStopFrpc() {
        frpcAgent.stopFrpc();
        isFrpcStarted = false;
        updateDevice(false);
    }

    public void bindRemotePort() {
        frpcAgent.bindRemotePort(getClientId(), new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = handler.obtainMessage(Msg.BIND_REMOTE_PORT_FAILED, e);
                handler.sendMessage(msg);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String res = response.body().string();
                    Gson gson = new Gson();
                    PortResponse portResponse = gson.fromJson(res, PortResponse.class);
                    if (portResponse.getStatus() == 0) {
                        Message msg = handler.obtainMessage(Msg.BIND_REMOTE_PORT_SUCCESS, portResponse.getPort());
                        handler.sendMessage(msg);
                    } else {
                        Message msg = handler.obtainMessage(Msg.BIND_REMOTE_PORT_FAILED);
                        handler.sendMessage(msg);
                    }
                } else {
                    Message msg = handler.obtainMessage(Msg.BIND_REMOTE_PORT_FAILED);
                    handler.sendMessage(msg);
                }
            }
        });
    }

    public void getRemotePort() {
        frpcAgent.getRemotePort(getClientId(), new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = handler.obtainMessage(Msg.GET_REMOTE_PORT_FAILED, e);
                handler.sendMessage(msg);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String res = response.body().string();
                    Gson gson = new Gson();
                    PortResponse portResponse = gson.fromJson(res, PortResponse.class);
                    if (portResponse.getStatus() == 0) {
                        Message msg = handler.obtainMessage(Msg.GET_REMOTE_PORT_SUCCESS, portResponse.getPort());
                        handler.sendMessage(msg);
                    } else {
                        Message msg = handler.obtainMessage(Msg.GET_REMOTE_PORT_FAILED);
                        handler.sendMessage(msg);
                    }
                } else {
                    Message msg = handler.obtainMessage(Msg.GET_REMOTE_PORT_FAILED);
                    handler.sendMessage(msg);
                }
            }
        });
    }

    public void unBindRemotePort() {
        frpcAgent.unBindRemotePort(getClientId(), new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = handler.obtainMessage(Msg.UNBIND_REMOTE_PORT_FAILED, e);
                handler.sendMessage(msg);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String res = response.body().string();
                    Gson gson = new Gson();
                    PortResponse portResponse = gson.fromJson(res, PortResponse.class);
                    if (portResponse.getStatus() == 0) {
                        Message msg = handler.obtainMessage(Msg.UNBIND_REMOTE_PORT_SUCCESS);
                        handler.sendMessage(msg);
                    } else {
                        Message msg = handler.obtainMessage(Msg.UNBIND_REMOTE_PORT_FAILED);
                        handler.sendMessage(msg);
                    }
                } else {
                    Message msg = handler.obtainMessage(Msg.UNBIND_REMOTE_PORT_FAILED);
                    handler.sendMessage(msg);
                }
            }
        });
    }

    public void removeFrpc() {
        frpcAgent.removeFrpc(frpVersion, new StatusCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        checkAll();
                        Toast.makeText(MainActivity.this, "frp client 卸载成功", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(int exitCode, Exception e) {
                Log.e("RemoveFridaServer", String.valueOf(exitCode));
                if (e != null) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "frp client 卸载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    public void addDevice() {
        secIoTAgent.addDevice(getClientId(), port, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = handler.obtainMessage(Msg.ADD_DEVICE_FAILED, e);
                handler.sendMessage(msg);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String res = response.body().string();
                    Gson gson = new Gson();
                    BaseResponse baseResponse = gson.fromJson(res, BaseResponse.class);
                    if (baseResponse.getStatus() == 0) {
                        Message msg = handler.obtainMessage(Msg.ADD_DEVICE_SUCCESS);
                        handler.sendMessage(msg);
                    } else {
                        Message msg = handler.obtainMessage(Msg.ADD_DEVICE_FAILED);
                        handler.sendMessage(msg);
                    }
                } else {
                    Message msg = handler.obtainMessage(Msg.ADD_DEVICE_FAILED);
                    handler.sendMessage(msg);
                }
            }
        });
    }

    public void updateDevice(boolean isOnline) {
        secIoTAgent.updateDeviceStatus(getClientId(), port, isOnline, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = handler.obtainMessage(Msg.UPDATE_DEVICE_FAILED, e);
                handler.sendMessage(msg);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String res = response.body().string();
                    Gson gson = new Gson();
                    BaseResponse baseResponse = gson.fromJson(res, BaseResponse.class);
                    if (baseResponse.getStatus() == 0) {
                        Message msg = handler.obtainMessage(Msg.UPDATE_DEVICE_SUCCESS);
                        handler.sendMessage(msg);
                    } else {
                        Message msg = handler.obtainMessage(Msg.UPDATE_DEVICE_FAILED);
                        handler.sendMessage(msg);
                    }
                } else {
                    Message msg = handler.obtainMessage(Msg.UPDATE_DEVICE_FAILED);
                    handler.sendMessage(msg);
                }
            }
        });
    }

    @Override
    public void setProgress(final int progressBarId, final double percentage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ProgressBar bar = findViewById(progressBarId);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    bar.setProgress((int) (bar.getMax() * percentage), true);
                } else {
                    bar.setProgress((int) (bar.getMax() * percentage));
                }
            }
        });
    }
}
