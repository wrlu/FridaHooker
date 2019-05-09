package com.wrlus.seciot;

import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.wrlus.seciot.hook.FridaServerAgent;
import com.wrlus.seciot.model.FridaVersionResponse;
import com.wrlus.seciot.msg.Msg;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements Handler.Callback {
    private Handler handler = new Handler(this);
    private Switch switchStatus, switchServerConfig;
    private ImageView imageStatus;
    private TextView textViewFridaVersion;
    private Button btnFridaManage;
    private CheckBox checkBoxAPICheck, checkBoxConnection, checkBoxDataTransfer, checkBoxFileIO, checkBoxDB;
    private String fridaVersion = "未知";

    static {
        System.loadLibrary("seciot_agent");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.bindWidget();
        this.bindWidgetEvent();
        try {
            boolean rootStatus = FridaServerAgent.requestRootPermission("whoami");
            Toast.makeText(this, "ROOT状态："+rootStatus, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "ROOT状态：false", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.getFridaVersionOnServer();
    }

    public void bindWidget() {
        switchStatus = findViewById(R.id.switchStatus);
        switchServerConfig = findViewById(R.id.switchServerConfig);
        imageStatus = findViewById(R.id.imageStatus);
        textViewFridaVersion = findViewById(R.id.textViewFridaVersion);
        btnFridaManage = findViewById(R.id.btnFridaManage);
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
                String[] manageFridaAction = { "安装"+textViewFridaVersion.getText(), "卸载"+textViewFridaVersion.getText() };
                AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                dialog.setTitle("管理 Frida 模块");
                dialog.setItems(manageFridaAction, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                installFrida(fridaVersion);
                                break;
                            case 1:
                                uninstallFrida(fridaVersion);
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
                textViewFridaVersion.setText("Frida 版本 "+fridaVersion);
                break;
            case Msg.GET_FRIDA_VERSION_FAILED:
                textViewFridaVersion.setText("Frida 版本 "+fridaVersion);
                Toast.makeText(MainActivity.this, "无法连接到服务器，请检查网络设置。", Toast.LENGTH_SHORT).show();
                break;
            default:
                Log.e("MessafeHandler", "Unknown message what = "+msg.what);
        }
        return true;
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
                    FridaVersionResponse fridaVersionResponse = gson.fromJson(res, FridaVersionResponse.class);
                    if (fridaVersionResponse.getStatus() == 0) {
                        Message msg = handler.obtainMessage(Msg.GET_FRIDA_VERSION_SUCCESS, fridaVersionResponse.getVersion());
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

    public void installFrida(String version) {

    }

    public void uninstallFrida(String version) {

    }
}
