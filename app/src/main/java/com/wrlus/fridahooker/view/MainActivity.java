package com.wrlus.fridahooker.view;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.os.Bundle;
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

import com.wrlus.fridahooker.R;
import com.wrlus.fridahooker.agent.FridaAgent;
import com.wrlus.fridahooker.agent.StatusCallback;
import com.wrlus.fridahooker.util.Msg;
import com.wrlus.fridahooker.util.DeviceHelper;
import com.wrlus.fridahooker.util.LogUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements Handler.Callback, ProgressCallback {
    private static final String TAG = "MainActivity";
    private static final String localFridaVersion = "12.7.16";
    private String abi = "Unknown";
    private String fridaVersion = localFridaVersion;
    private boolean isProductSupported = false;
    private boolean isFridaServerInstalled = false;
    private volatile boolean isFridaServerRunning = false;

    private final Handler handler = new Handler(this);
    private final FridaAgent fridaAgent = FridaAgent.getInstance(this);

    private Switch switchStatus;
    private ImageView imageStatus;
    private TextView textViewFridaVersion, textViewAndroidVer, textViewDeviceName, textViewStructure;
    private Button btnFridaManage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUi();
        getSystemInfo();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item){
        if (item.getItemId() == R.id.btnRefresh) {
            checkFridaInstallation();
            checkFridaRunning();
        } else if (item.getItemId() == R.id.btnSettings) {
            LogUtil.d(TAG, "准备进入设置页面");
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
        checkFridaInstallation();
        checkFridaRunning();
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case Msg.DOWNLOAD_FRIDA_SUCCESS:
                setProgress(R.id.progressBarFridaInstall, 0.5);
                installFrida((File) msg.obj);
                break;
            case Msg.DOWNLOAD_FRIDA_FAILED:
                LogUtil.t(this, "无法获取frida server压缩包。", msg.obj);
                break;
            default:
                LogUtil.e(TAG, "Receive odd message: "+msg.what);
                break;
        }
        return true;
    }

    protected void initUi() {
        switchStatus = findViewById(R.id.switchStatus);
        imageStatus = findViewById(R.id.imageStatus);
        textViewFridaVersion = findViewById(R.id.textViewFridaVersion);
        textViewAndroidVer = findViewById(R.id.textViewAndroidVer);
        textViewDeviceName = findViewById(R.id.textViewDeviceName);
        textViewStructure = findViewById(R.id.textViewStructure);
        btnFridaManage = findViewById(R.id.btnFridaManage);
        imageStatus.setImageResource(R.mipmap.status_error);
        switchStatus.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (!isFridaServerRunning) {
                        startFrida();
                    }
                } else {
                    if (isFridaServerRunning) {
                        stopFrida();
                    }
                }
            }
        });
        btnFridaManage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isProductSupported) {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                    dialog.setTitle(R.string.warnings);
                    dialog.setMessage(R.string.unsupported_device);
                    dialog.setNegativeButton("关闭", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
                    dialog.show();
                    return;
                }
                List<String> manageFridaAction = new ArrayList<>();
                manageFridaAction.add("安装预置的 frida server "+fridaVersion+"-"+abi);
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
                                getLocalFrida();
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
    }

    protected void getSystemInfo() {
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
        LogUtil.d(TAG, "系统版本："+androidVerString);
        LogUtil.d(TAG, "设备名称："+deviceNameString);
        LogUtil.d(TAG, "体系结构："+deviceAbiString);
        if (!DeviceHelper.checkAPILevel()) {
            LogUtil.t(this, "暂不支持该系统版本", "Unsupport API Level = "+DeviceHelper.getAPILevel());
            return;
        }
        if (abi.contains("arm64")) {
            this.abi = "arm64";
            isProductSupported = true;
        } else if (abi.contains("arm")) {
            this.abi = "arm";
            isProductSupported = true;
        } else if (abi.contains("x86_64")) {
            this.abi = "x86_64";
            isProductSupported = true;
        } else if (abi.contains("x86")) {
            this.abi = "x86";
            isProductSupported = true;
        } else {
            LogUtil.t(this, "暂不支持此设备", "Unsupport ABI = "+abi);
        }
    }

    protected void checkFridaInstallation() {
        boolean isInstalled = fridaAgent.checkFridaInstallation(fridaVersion);
        imageStatus.setImageResource(isInstalled ? R.mipmap.status_success : R.mipmap.status_error);
        String fridaStatusString = getString(isInstalled ? R.string.frida_ready : R.string.frida_missing);
        fridaStatusString = String.format(fridaStatusString, fridaVersion, abi);
        textViewFridaVersion.setText(fridaStatusString);
        setProgress(R.id.progressBarFridaInstall, isInstalled ? 1 : 0 );
        switchStatus.setEnabled(isInstalled);
        isFridaServerInstalled = isInstalled;
    }

    protected void checkFridaRunning() {
        fridaAgent.checkFridaRunning(new StatusCallback() {
            @Override
            public void onSuccess() {
                isFridaServerRunning = true;
                runOnUiThread(()->{
                    switchStatus.setChecked(true);
                });
            }

            @Override
            public void onFailure(Throwable e) {
                isFridaServerRunning = false;
                runOnUiThread(()->{
                    switchStatus.setChecked(false);
                });
            }
        });

    }

    protected void getLocalFrida() {
        final String filename = "frida-server-" + fridaVersion + "-android-" + abi + ".xz";
        final AssetManager assetManager = getAssets();
        try {
            File targetFile = fridaAgent.extractXZ(assetManager.open(filename), getCacheDir().getAbsolutePath()
                    + "/frida-server-"+fridaVersion+"-android-"+abi);
            LogUtil.d(TAG, "Local frida file path: "+targetFile.getAbsolutePath());
            Message msg = handler.obtainMessage(Msg.DOWNLOAD_FRIDA_SUCCESS, targetFile);
            handler.sendMessage(msg);
        } catch (IOException e) {
            Message msg = handler.obtainMessage(Msg.DOWNLOAD_FRIDA_FAILED, e);
            handler.sendMessage(msg);
        }
    }

    protected void installFrida(File downloadFile) {
        boolean isSuccess = fridaAgent.installFrida(downloadFile, fridaVersion);
        if (isSuccess) {
            LogUtil.t(this, "frida server "+fridaVersion+" 安装成功", null);
        } else {
            LogUtil.t(this, "frida server "+fridaVersion+" 安装失败", null);
        }
        checkFridaInstallation();
    }

    protected void startFrida() {
        boolean isSuccess = fridaAgent.startFrida(fridaVersion);
        if (isSuccess) {
            LogUtil.t(this, "frida server "+fridaVersion+" 已经启动", null);
        } else {
            LogUtil.t(this, "frida server "+fridaVersion+" 启动失败", null);
        }
        checkFridaRunning();
    }

    protected void stopFrida() {
        boolean isSuccess = fridaAgent.stopFrida(fridaVersion, abi);
        if (isSuccess) {
            LogUtil.t(this, "frida server "+fridaVersion+" 已经停止", null);
        } else {
            LogUtil.t(this, "frida server "+fridaVersion+" 未能停止", null);
        }
        checkFridaRunning();
    }

    protected void removeFrida() {
        boolean isSuccess = fridaAgent.removeFrida(fridaVersion);
        if (isSuccess) {
            LogUtil.t(this, "frida server "+fridaVersion+" 卸载成功", null);
        } else {
            LogUtil.t(this, "frida server "+fridaVersion+" 卸载失败", null);
        }
        checkFridaInstallation();
    }

    @Override
    public void setProgress(final int progressBarId, final double percentage) {
        runOnUiThread(()->{
            ProgressBar bar = findViewById(progressBarId);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                bar.setProgress((int) (bar.getMax() * percentage), true);
            } else {
                bar.setProgress((int) (bar.getMax() * percentage));
            }
        });
    }

    protected boolean checkPermission(String permission) {
        boolean permissionAccessApproved =
                ActivityCompat.checkSelfPermission(this,
                        permission) ==
                        PackageManager.PERMISSION_GRANTED;

        if (!permissionAccessApproved) {
            ActivityCompat.requestPermissions(this, new String[]{
                    permission
            }, 0);
        }
        return permissionAccessApproved;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            LogUtil.d(TAG, permissions[0] + " Approved.");
        }
    }
}
