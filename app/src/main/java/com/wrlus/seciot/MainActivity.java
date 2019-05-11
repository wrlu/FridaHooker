package com.wrlus.seciot;

import android.content.DialogInterface;
import android.os.Environment;
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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.wrlus.seciot.agent.FridaServerAgent;
import com.wrlus.seciot.agent.FrpcAgent;
import com.wrlus.seciot.model.VersionResponse;
import com.wrlus.seciot.msg.Msg;
import com.wrlus.seciot.util.DeviceHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements Handler.Callback, ProgressCallback {
    private Handler handler = new Handler(this);
    private Switch switchStatus, switchServerConfig;
    private ImageView imageStatus;
    private TextView textViewFridaVersion, textViewFrpVersion;
    private Button btnFridaManage, btnFrpcManage, btnRefresh;
    private CheckBox checkBoxAPICheck, checkBoxConnection, checkBoxDataTransfer, checkBoxFileIO, checkBoxDB;
    private String abi = "Unknown";
    private String fridaVersion = "Unknown", frpVersion = "Unknown";

    static {
        System.loadLibrary("seciot_agent");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.bindWidget();
        this.bindWidgetEvent();
        this.getProductCpuAbi();
        try {
            boolean rootStatus = DeviceHelper.requestRootPermission("whoami");
            Toast.makeText(this, "ROOT状态："+rootStatus, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "ROOT状态：false", Toast.LENGTH_SHORT).show();
        }
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
        switchServerConfig = findViewById(R.id.switchServerConfig);
        imageStatus = findViewById(R.id.imageStatus);
        textViewFridaVersion = findViewById(R.id.textViewFridaVersion);
        textViewFrpVersion = findViewById(R.id.textViewFrpVersion);
        btnFridaManage = findViewById(R.id.btnFridaManage);
        btnFrpcManage = findViewById(R.id.btnFrpcManage);
        btnRefresh = new Button(this);
        checkBoxAPICheck = findViewById(R.id.checkBoxAPICheck);
        checkBoxConnection = findViewById(R.id.checkBoxConnection);
        checkBoxDataTransfer = findViewById(R.id.checkBoxDataTransfer);
        checkBoxFileIO = findViewById(R.id.checkBoxFileIO);
        checkBoxDB = findViewById(R.id.checkBoxDB);
    }

    public void bindWidgetEvent() {
        switchStatus.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

            }
        });
        switchServerConfig.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                checkBoxAPICheck.setEnabled(!isChecked);
                checkBoxConnection.setEnabled(!isChecked);
                checkBoxDataTransfer.setEnabled(!isChecked);
                checkBoxDB.setEnabled(!isChecked);
                checkBoxFileIO.setEnabled(!isChecked);
            }
        });
        btnFridaManage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (fridaVersion.equals("Unknown")) {
                    Toast.makeText(MainActivity.this, "frida版本未知，不能执行此操作，请刷新后重试。", Toast.LENGTH_SHORT).show();
                    return;
                }
                String[] manageFridaAction = { "安装 "+textViewFridaVersion.getText(), "卸载 "+textViewFridaVersion.getText() };
                AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                dialog.setTitle("管理 frida server");
                dialog.setItems(manageFridaAction, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                downloadFridaServer(fridaVersion, abi);
                                break;
                            case 1:
                                removeFrida(fridaVersion);
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
                if (fridaVersion.equals("Unknown")) {
                    Toast.makeText(MainActivity.this, "frp版本未知，不能执行此操作，请刷新后重试。", Toast.LENGTH_SHORT).show();
                    return;
                }
                String[] manageFridaAction = { "安装 "+textViewFrpVersion.getText(), "卸载 "+textViewFrpVersion.getText() };
                AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                dialog.setTitle("管理 frp client");
                dialog.setItems(manageFridaAction, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                downloadFrpc(frpVersion, abi);
                                break;
                            case 1:
                                removeFrpc(frpVersion);
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

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case Msg.GET_FRIDA_VERSION_SUCCESS:
                fridaVersion = (String) msg.obj;
                Log.i("FridaVersion", fridaVersion);
                textViewFridaVersion.setText("frida server "+fridaVersion+"-"+abi);
                break;
            case Msg.GET_FRIDA_VERSION_FAILED:
                textViewFridaVersion.setText("frida server "+fridaVersion+"-"+abi);
                Toast.makeText(MainActivity.this, "无法连接到服务器，请检查网络设置。", Toast.LENGTH_SHORT).show();
                break;
            case Msg.GET_FRP_VERSION_SUCCESS:
                fridaVersion = (String) msg.obj;
                Log.i("FrpVerion", frpVersion);
                textViewFrpVersion.setText("frp client "+frpVersion+"-"+abi);
                break;
            case Msg.GET_FRP_VERSION_FAILED:
                textViewFrpVersion.setText("frp client "+frpVersion+"-"+abi);
                Toast.makeText(MainActivity.this, "无法连接到服务器，请检查网络设置。", Toast.LENGTH_SHORT).show();
                break;
            case Msg.DOWNLOAD_FRIDA_SUCCESS:
                this.setProgress(R.id.progressBarFridaInstall, 0.25);
                this.installFrida( (File) msg.obj, fridaVersion);
                break;
            case Msg.DOWNLOAD_FRIDA_FAILED:
                Toast.makeText(MainActivity.this, "无法连接到服务器，请检查网络设置。", Toast.LENGTH_SHORT).show();
                break;
            case Msg.DOWNLOAD_FRP_SUCCESS:
                this.setProgress(R.id.progressBarFrpInstall, 0.25);
                this.installFrpc( (File) msg.obj, frpVersion);
                break;
            case Msg.DOWNLOAD_FRP_FAILED:
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

    public void downloadFridaServer(String version, final String abi) {
        FridaServerAgent.downloadFridaServer(version, abi, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = handler.obtainMessage(Msg.DOWNLOAD_FRIDA_FAILED, e);
                handler.sendMessage(msg);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    InputStream is = response.body().byteStream();
                    File dlFile = new File(Environment.getDownloadCacheDirectory().getAbsolutePath()
                            + "/frida-server-"+fridaVersion+"-android-"+abi+".xz");
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

    public void installFrida(File downloadFile, String version) {
        FridaServerAgent.installFridaServer(downloadFile, version);
    }

    public void removeFrida(String version) {
        FridaServerAgent.removeFridaServer(version);
    }

    public void downloadFrpc(String version, final String abi) {
        FrpcAgent.downloadFrp(version, abi, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = handler.obtainMessage(Msg.DOWNLOAD_FRP_FAILED, e);
                handler.sendMessage(msg);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    InputStream is = response.body().byteStream();
                    File dlFile = new File(Environment.getDownloadCacheDirectory().getAbsolutePath()
                            + "/frp_"+fridaVersion+"_linux_"+abi+".tar.gz");
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

    public void installFrpc(File downloadFile, String version) {
        FrpcAgent.installFrpc(downloadFile, version);
    }

    public void removeFrpc(String version) {
        FrpcAgent.removeFrpc(version);
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
