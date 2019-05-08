package com.wrlus.seciot;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.wrlus.seciot.hook.FridaServerAgent;

public class MainActivity extends AppCompatActivity {
    private Switch switchStatus, switchServerConfig;
    private ImageView imageStatus;
    private TextView textViewFridaVersion;
    private Button btnFridaInstall, btnFridaUninstall;
    private CheckBox checkBoxAPICheck, checkBoxConnection, checkBoxDataTransfer, checkBoxFileIO, checkBoxDB;

    static {
        System.loadLibrary("seciot_agent");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.bindWidget();
//        boolean rootStatus = FridaServerAgent.requestRootPermission();
        try {
            boolean rootStatus = FridaServerAgent.requestRootPermissionJava();
            Toast.makeText(this, "ROOT状态："+rootStatus, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "ROOT状态：false", Toast.LENGTH_SHORT).show();
        }
    }

    public void bindWidget() {
        switchStatus = findViewById(R.id.switchStatus);
        switchServerConfig = findViewById(R.id.switchServerConfig);
        imageStatus = findViewById(R.id.imageStatus);
        textViewFridaVersion = findViewById(R.id.textViewFridaVersion);
        btnFridaInstall = findViewById(R.id.btnFridaInstall);
        btnFridaUninstall = findViewById(R.id.btnFridaUninstall);
        checkBoxAPICheck = findViewById(R.id.checkBoxAPICheck);
        checkBoxConnection = findViewById(R.id.checkBoxConnection);
        checkBoxDataTransfer = findViewById(R.id.checkBoxDataTransfer);
        checkBoxFileIO = findViewById(R.id.checkBoxFileIO);
        checkBoxDB = findViewById(R.id.checkBoxDB);
    }

}
