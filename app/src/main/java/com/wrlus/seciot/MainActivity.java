package com.wrlus.seciot;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import com.wrlus.seciot.agent.StatusCallback;
import com.wrlus.seciot.model.PortResponse;
import com.wrlus.seciot.model.VersionResponse;
import com.wrlus.seciot.msg.Msg;
import com.wrlus.seciot.util.DeviceHelper;

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
    private TextView textViewFridaVersion, textViewFrpVersion;
    private Button btnFridaManage, btnFrpcManage, btnRefresh;
    private String abi = "Unknown";
    private String fridaVersion = "Unknown", frpVersion = "Unknown";
    private boolean isFridaServerInstalled = false, isFrpcInstalled = false, isFridaServerStarted = false, isFrpcStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.bindWidget();
        this.bindWidgetEvent();
        this.getProductCpuAbi();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case R.id.btnRefresh:
                this.getFridaVersionOnServer();
                this.getFrpsVersionOnServer();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.getFridaVersionOnServer();
        this.getFrpsVersionOnServer();
    }

    public void bindWidget() {
        switchStatus = findViewById(R.id.switchStatus);
        imageStatus = findViewById(R.id.imageStatus);
        textViewFridaVersion = findViewById(R.id.textViewFridaVersion);
        textViewFrpVersion = findViewById(R.id.textViewFrpVersion);
        btnFridaManage = findViewById(R.id.btnFridaManage);
        btnFrpcManage = findViewById(R.id.btnFrpcManage);
        btnRefresh = new Button(this);
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
//                    TODO: 实现服务的停止和清理
                    switchStatus.setChecked(true);
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

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case Msg.GET_FRIDA_VERSION_SUCCESS:
                fridaVersion = (String) msg.obj;
                Log.i("FridaVersion", fridaVersion);
                textViewFridaVersion.setText("frida server "+fridaVersion+"-"+abi+" 缺失");
                this.setProgress(R.id.progressBarFridaInstall, 0);
                this.checkFridaInstallation();
                break;
            case Msg.GET_FRIDA_VERSION_FAILED:
                textViewFridaVersion.setText("frida server 不可用");
                Toast.makeText(MainActivity.this, "无法连接到服务器，请检查网络设置。", Toast.LENGTH_SHORT).show();
                this.setProgress(R.id.progressBarFridaInstall, 0);
                break;
            case Msg.GET_FRP_VERSION_SUCCESS:
                frpVersion = (String) msg.obj;
                Log.i("FrpVerion", frpVersion);
                textViewFrpVersion.setText("frp client "+frpVersion+"-"+abi+" 缺失");
                this.setProgress(R.id.progressBarFrpInstall, 0);
                this.checkFrpcInstallation();
                break;
            case Msg.GET_FRP_VERSION_FAILED:
                textViewFrpVersion.setText("frp client 不可用");
                Toast.makeText(MainActivity.this, "无法连接到服务器，请检查网络设置。", Toast.LENGTH_SHORT).show();
                this.setProgress(R.id.progressBarFrpInstall, 0);
                break;
            case Msg.DOWNLOAD_FRIDA_SUCCESS:
                this.setProgress(R.id.progressBarFridaInstall, 0.5);
                this.installFrida( (File) msg.obj);
                break;
            case Msg.DOWNLOAD_FRIDA_FAILED:
                Toast.makeText(MainActivity.this, "无法连接到服务器，请检查网络设置。", Toast.LENGTH_SHORT).show();
                break;
            case Msg.DOWNLOAD_FRP_SUCCESS:
                this.setProgress(R.id.progressBarFrpInstall, 0.33);
                this.installFrpc( (File) msg.obj);
                break;
            case Msg.DOWNLOAD_FRP_FAILED:
                Toast.makeText(MainActivity.this, "无法连接到服务器，请检查网络设置。", Toast.LENGTH_SHORT).show();
                break;
            case Msg.BIND_REMOTE_PORT_SUCCESS:
                this.realStartFrpc( (Integer) msg.obj);
                break;
            case Msg.BIND_REMOTE_PORT_FAILED:
                Toast.makeText(MainActivity.this, "无法连接到服务器，请检查网络设置。", Toast.LENGTH_SHORT).show();
                break;
            case Msg.GET_REMOTE_PORT_SUCCESS:
                this.realStartFrpc( (Integer) msg.obj);
                break;
            case Msg.GET_REMOTE_PORT_FAILED:
                this.bindRemotePort();
                break;
            case Msg.UNBIND_REMOTE_PORT_SUCCESS:
//                TODO: Stop Frpc
                break;
            case Msg.UNBIND_REMOTE_PORT_FAILED:
                Toast.makeText(MainActivity.this, "无法连接到服务器，请检查网络设置。", Toast.LENGTH_SHORT).show();
                break;
            default:
                Log.e("MessageHandler", "Unknown message: msg.what = "+msg.what);
        }
        return true;
    }

    public void getProductCpuAbi() {
        String abi = DeviceHelper.getProductCpuAbi();
        if (abi == null) {
            Toast.makeText(this, "Get product cpu abi failed", Toast.LENGTH_SHORT).show();
        } else if (abi.contains("arm64")) {
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
        FridaServerAgent.getFridaVersionOnServer(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = handler.obtainMessage(Msg.GET_FRIDA_VERSION_FAILED, e);
                handler.sendMessage(msg);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
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
        FrpcAgent.getFrpsVersionOnServer(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = handler.obtainMessage(Msg.GET_FRIDA_VERSION_FAILED, e);
                handler.sendMessage(msg);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
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

    public void checkFridaInstallation() {
        if (FridaServerAgent.checkFridaServerInstallation(fridaVersion)) {
            textViewFridaVersion.setText("frida server "+fridaVersion+"-"+abi+" 就绪");
            this.setProgress(R.id.progressBarFridaInstall, 1);
            isFridaServerInstalled = true;
            if (isFrpcInstalled) {
                imageStatus.setImageResource(R.mipmap.status_success);
            }
        } else {
            textViewFridaVersion.setText("frida server "+fridaVersion+"-"+abi+" 缺失");
            this.setProgress(R.id.progressBarFridaInstall, 0);
            isFridaServerInstalled = false;
            imageStatus.setImageResource(R.mipmap.status_error);
        }
    }

    public void downloadFridaServer() {
        FridaServerAgent.downloadFridaServer(fridaVersion, abi, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = handler.obtainMessage(Msg.DOWNLOAD_FRIDA_FAILED, e);
                handler.sendMessage(msg);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
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
        FridaServerAgent.installFridaServer(downloadFile, fridaVersion, new StatusCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.this.setProgress(R.id.progressBarFridaInstall, 1);
                        MainActivity.this.checkFridaInstallation();
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
                        MainActivity.this.setProgress(R.id.progressBarFridaInstall, 0);
                        Toast.makeText(MainActivity.this, "frida server 安装失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    public void startFrida() {
        FridaServerAgent.startFridaServer(this, fridaVersion);
    }

    public void removeFrida() {
        FridaServerAgent.removeFridaServer(fridaVersion, new StatusCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.this.checkFridaInstallation();
                        MainActivity.this.setProgress(R.id.progressBarFridaInstall, 0);
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

    public void checkFrpcInstallation() {
        if (FrpcAgent.checkFrpcInstallation(frpVersion)) {
            textViewFrpVersion.setText("frp client "+frpVersion+"-"+abi+" 就绪");
            this.setProgress(R.id.progressBarFrpInstall, 1);
            isFrpcInstalled = true;
            if (isFridaServerInstalled) {
                imageStatus.setImageResource(R.mipmap.status_success);
            }
        } else {
            textViewFrpVersion.setText("frp client "+frpVersion+"-"+abi+" 缺失");
            this.setProgress(R.id.progressBarFrpInstall, 0);
            isFrpcInstalled = false;
            imageStatus.setImageResource(R.mipmap.status_error);
        }
    }

    public void downloadFrpc() {
        FrpcAgent.downloadFrp(frpVersion, abi, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = handler.obtainMessage(Msg.DOWNLOAD_FRP_FAILED, e);
                handler.sendMessage(msg);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
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
        FrpcAgent.installFrpc(downloadFile, frpVersion, new StatusCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.this.checkFrpcInstallation();
                        MainActivity.this.setProgress(R.id.progressBarFrpInstall, 1);
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
                        MainActivity.this.setProgress(R.id.progressBarFrpInstall, 0);
                        Toast.makeText(MainActivity.this, "frp client 安装失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    public void startFrpc() {
        getRemotePort();
    }

    public void bindRemotePort() {
        FrpcAgent.bindRemotePort(getClientId(), new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = handler.obtainMessage(Msg.BIND_REMOTE_PORT_FAILED, e);
                handler.sendMessage(msg);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
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
        FrpcAgent.getRemotePort(getClientId(), new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = handler.obtainMessage(Msg.GET_REMOTE_PORT_FAILED, e);
                handler.sendMessage(msg);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
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

    public void realStartFrpc(int port) {
        FrpcAgent.startFrpc(this, frpVersion, port);
    }

    public void unBindRemotePort() {
        FrpcAgent.unBindRemotePort(getClientId(), new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = handler.obtainMessage(Msg.UNBIND_REMOTE_PORT_FAILED, e);
                handler.sendMessage(msg);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
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
        FrpcAgent.removeFrpc(frpVersion, new StatusCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.this.checkFrpcInstallation();
                        MainActivity.this.setProgress(R.id.progressBarFrpInstall, 0);
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

    @Override
    public void setProgress(final int progressBarId, final double percentage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ProgressBar bar = findViewById(progressBarId);
                bar.setProgress((int) (bar.getMax() * percentage));
            }
        });
    }
}
