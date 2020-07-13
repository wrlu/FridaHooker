package com.wrlus.fridahooker.view;

import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.wrlus.fridahooker.R;
import com.wrlus.fridahooker.agent.FridaAgent;
import com.wrlus.fridahooker.agent.StatusCallback;
import com.wrlus.fridahooker.config.Config;
import com.wrlus.fridahooker.util.Msg;
import com.wrlus.fridahooker.util.DeviceHelper;
import com.wrlus.fridahooker.util.LogUtil;

public class MainActivity extends AppCompatActivity implements Handler.Callback, ProgressCallback {
    private static final String TAG = "MainActivity";
    private static final int READ_REQUEST_CODE = 129;
    private String fridaVersion = Config.LOCAL_FRIDA_VERSION;

    private final Handler handler = new Handler(this);
    private FridaAgent fridaAgent;

    private Switch switchStatus;
    private ImageView imageStatus;
    private TextView textViewFridaVersion;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUi();
        fridaAgent = new FridaAgent(getFilesDir().getAbsolutePath() +
                File.separator + "frida" + File.separator + fridaVersion);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkAll();
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
            checkAll();
        } else if (item.getItemId() == R.id.btnSettings) {
            LogUtil.d(TAG, "准备进入设置页面");
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.btnAbout) {
            makeMessageDialog(R.string.about, R.string.gplv2).show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case Msg.DOWNLOAD_FRIDA_FROM_ASSET_SUCCESS:
            case Msg.DOWNLOAD_FRIDA_FROM_SDCARD_SUCCESS:
                setProgress(R.id.progressBarFridaInstall, 0.5);
                installFrida((File) msg.obj);
                break;
            case Msg.DOWNLOAD_FRIDA_FROM_ASSET_FAILED:
                makeMessageDialog(R.string.warnings, R.string.operation_failed_message).show();
                break;
            case Msg.DOWNLOAD_FRIDA_FROM_SDCARD_FAILED:
                makeMessageDialog("Tips", "Unfinished feature").show();
                break;
            default:
                LogUtil.e(TAG, "Receive odd message: "+msg.what);
                break;
        }
        return true;
    }

    private void initUi() {
        switchStatus = findViewById(R.id.switchStatus);
        imageStatus = findViewById(R.id.imageStatus);
        textViewFridaVersion = findViewById(R.id.textViewFridaVersion);
        TextView textViewAndroidVer = findViewById(R.id.textViewAndroidVer);
        TextView textViewDeviceName = findViewById(R.id.textViewDeviceName);
        TextView textViewStructure = findViewById(R.id.textViewStructure);
        Button btnFridaManage = findViewById(R.id.btnFridaManage);
        progressBar = findViewById(R.id.progressBar);
        imageStatus.setImageResource(R.mipmap.status_error);
        switchStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!fridaAgent.isStarted()) {
                    startFrida();
                }
            } else {
                if (fridaAgent.isStarted()) {
                    stopFrida();
                }
            }
        });
        btnFridaManage.setOnClickListener(v -> {
            if (!fridaAgent.isSupported()) {
                return;
            }
            List<String> manageFridaAction = new ArrayList<>();
            manageFridaAction.add(getString(R.string.install));
            manageFridaAction.add(getString(R.string.install_from_sdcard));
            if (fridaAgent.isInstalled()) {
                manageFridaAction.add(getString(R.string.remove));
            }
            AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
            dialog.setTitle(R.string.manage);
            dialog.setItems(manageFridaAction.toArray(new String[0]), (dialog12, which) -> {
                switch (which) {
                    case 0:
                        getFridaFromAsset();
                        break;
                    case 1:
                        getFridaFromSdcard();
                        break;
                    case 2:
                        removeFrida();
                        break;
                    default:
                        break;
                }
            });
            dialog.show();
        });
        String androidVerString = getString(R.string.android_ver);
        androidVerString = String.format(androidVerString, DeviceHelper.getAndroidVersion(), DeviceHelper.getAPILevel());
        textViewAndroidVer.setText(androidVerString);
        String deviceNameString = getString(R.string.device_name);
        deviceNameString = String.format(deviceNameString, DeviceHelper.getProductName());
        textViewDeviceName.setText(deviceNameString);
        String abi = DeviceHelper.getSupportedAbis()[0];
        String deviceAbiString = getString(R.string.device_abi);
        deviceAbiString = String.format(deviceAbiString, abi);
        textViewStructure.setText(deviceAbiString);
    }

    private void checkAll() {
        progressBar.setVisibility(View.VISIBLE);
        if (fridaAgent == null) {
            progressBar.setVisibility(View.GONE);
            return;
        }
        fridaAgent.checkAll(new StatusCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    if (!fridaAgent.isSupported()) {
                        makeMessageDialog(R.string.warnings, R.string.unsupported_device).show();
                    }
                    imageStatus.setImageResource(fridaAgent.isInstalled() ? R.mipmap.status_success : R.mipmap.status_error);
                    String fridaStatusString = getString(fridaAgent.isInstalled() ? R.string.frida_ready : R.string.frida_missing);
                    fridaStatusString = String.format(fridaStatusString, fridaVersion);
                    textViewFridaVersion.setText(fridaStatusString);
                    setProgress(R.id.progressBarFridaInstall, fridaAgent.isInstalled() ? 1 : 0 );
                    switchStatus.setEnabled(fridaAgent.isInstalled());
                    switchStatus.setChecked(fridaAgent.isStarted());
                    progressBar.setVisibility(View.GONE);
                });
            }

            @Override
            public void onFailure(Throwable e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                });
            }
        });
    }

    private void getFridaFromAsset() {
        if (fridaAgent != null) {
            String abi = fridaAgent.getSystemFridaAbi();
            try {
                File cacheFile = fridaAgent.extractLocalFrida(
                        getAssets().open("frida-server-"+fridaVersion+"-android-"+abi+".xz"),
                        getCacheDir().getAbsolutePath());
                Message msg = handler.obtainMessage(Msg.DOWNLOAD_FRIDA_FROM_ASSET_SUCCESS, cacheFile);
                handler.sendMessage(msg);
            } catch (IOException e) {
                Message msg = handler.obtainMessage(Msg.DOWNLOAD_FRIDA_FROM_ASSET_FAILED, e);
                handler.sendMessage(msg);
            }
        } else {
            Message msg = handler.obtainMessage(Msg.DOWNLOAD_FRIDA_FROM_ASSET_FAILED);
            handler.sendMessage(msg);
        }
    }

    private void getFridaFromSdcard() {
        if (fridaAgent != null) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*");
            startActivityForResult(intent, READ_REQUEST_CODE);
        } else {
            Message msg = handler.obtainMessage(Msg.DOWNLOAD_FRIDA_FROM_SDCARD_FAILED);
            handler.sendMessage(msg);
        }
    }

    private void installFrida(File cacheFile) {
        boolean isSuccess = false;
        if (fridaAgent != null) {
            isSuccess = fridaAgent.installFrida(cacheFile);
        }
        if (isSuccess) {
            checkAll();
        } else {
            makeMessageDialog(R.string.warnings, R.string.operation_failed_message).show();
        }
    }

    private void removeFrida() {
        boolean isSuccess = false;
        if (fridaAgent != null) {
            isSuccess = fridaAgent.removeFrida();
        }
        if (isSuccess) {
            checkAll();
        } else {
            makeMessageDialog(R.string.warnings, R.string.operation_failed_message).show();
        }
    }

    private void startFrida() {
        boolean isFinish = false;
        if (fridaAgent != null) {
            isFinish = fridaAgent.startFrida();
        }
        if (isFinish) {
            checkAll();
        } else {
            makeMessageDialog(R.string.warnings, R.string.operation_failed_message).show();
        }
    }

    private void stopFrida() {
        boolean isFinish = false;
        if (fridaAgent != null) {
            isFinish = fridaAgent.stopFrida();
        }
        if (isFinish) {
            checkAll();
        } else {
            makeMessageDialog(R.string.warnings, R.string.operation_failed_message).show();
        }
    }

    @Override
    public void setProgress(final int progressBarId, final double percentage) {
        runOnUiThread(() -> {
            ProgressBar bar = findViewById(progressBarId);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                bar.setProgress((int) (bar.getMax() * percentage), true);
            } else {
                bar.setProgress((int) (bar.getMax() * percentage));
            }
        });
    }

    private AlertDialog.Builder makeMessageDialog(@StringRes int title, @StringRes int message) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setNegativeButton(R.string.close, (dialog1, which) -> {});
        return dialog;
    }

    @Deprecated
    private AlertDialog.Builder makeMessageDialog(String title, String message) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setNegativeButton(R.string.close, (dialog1, which) -> {});
        return dialog;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == READ_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                try {
                    InputStream is = getContentResolver().openInputStream(data.getData());
                    File cacheFile = fridaAgent.extractLocalFrida(is, getCacheDir().getAbsolutePath());
                    if (fridaAgent.validFridaExecutable(cacheFile)) {
                        Message msg = handler.obtainMessage(Msg.DOWNLOAD_FRIDA_FROM_SDCARD_SUCCESS, cacheFile);
                        handler.sendMessage(msg);
                    } else {
                        Message msg = handler.obtainMessage(Msg.DOWNLOAD_FRIDA_FROM_SDCARD_FAILED, null);
                        handler.sendMessage(msg);
                    }
                } catch (IOException e) {
                    Message msg = handler.obtainMessage(Msg.DOWNLOAD_FRIDA_FROM_SDCARD_FAILED, e);
                    handler.sendMessage(msg);
                }
            } else {
                Message msg = handler.obtainMessage(Msg.DOWNLOAD_FRIDA_FROM_SDCARD_FAILED, null);
                handler.sendMessage(msg);
            }

        }
    }

    private boolean checkPermission(String permission) {
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
