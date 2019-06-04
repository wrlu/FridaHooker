package com.wrlus.seciot;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.UUID;

public class SettingsActivity extends AppCompatActivity {
    private TextView textViewServerURL, textViewFrpsIP;
    private Button btnManageServerURL, btnManageFrpsIP, btnResetClientId;
    private String[] serverUrls = {
            "http://140.143.53.29:8080/SecIoT",
            "http://172.16.0.1:8080/SecIoT",
            "http://192.168.43.7:8080/SecIoT"
    };
    private String[] frpsIps = {
            "140.143.53.29",
            "172.16.0.1",
            "192.168.43.7"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        bindWidget();
        bindWidgetEvent();
        getServer();
        getFrpsIp();
    }

    public void bindWidget() {
        textViewServerURL = findViewById(R.id.textViewServerURL);
        textViewFrpsIP = findViewById(R.id.textViewFrpsIP);
        btnManageServerURL = findViewById(R.id.btnManageServerURL);
        btnManageFrpsIP = findViewById(R.id.btnManageFrpsIP);
        btnResetClientId = findViewById(R.id.btnResetClientId);
    }

    public void getServer() {
        SharedPreferences sharedPref = getSharedPreferences("com.wrlus.seciot", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        String serverUrl = sharedPref.getString("server_url", "Undefined");
        if (serverUrl == null || serverUrl.equals("Undefined")) {
            serverUrl = "http://140.143.53.29:8080/SecIoT";
            editor.putString("server_url", serverUrl);
            editor.apply();
        }
        String serverURLString = getString(R.string.server_host);
        serverURLString = String.format(serverURLString, serverUrl);
        textViewServerURL.setText(serverURLString);
    }

    public void getFrpsIp() {
        SharedPreferences sharedPref = getSharedPreferences("com.wrlus.seciot", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        String frpsIp = sharedPref.getString("frps_ip", "Undefined");
        if (frpsIp == null || frpsIp.equals("Undefined")) {
            frpsIp = "140.143.53.29";
            editor.putString("frps_ip", frpsIp);
            editor.apply();
        }
        String frpsIpString = getString(R.string.frps_ip);
        frpsIpString = String.format(frpsIpString, frpsIp);
        textViewFrpsIP.setText(frpsIpString);
    }

    public void bindWidgetEvent() {
        btnManageServerURL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                manageServerURL();
            }
        });
        btnManageFrpsIP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                manageFrpsIp();
            }
        });
        btnResetClientId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetClientId();
            }
        });
    }

    public void manageServerURL() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("更改 SecIoT 服务地址");
        dialog.setItems(serverUrls, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String serverUrl = serverUrls[which];
                SharedPreferences sharedPref = getSharedPreferences("com.wrlus.seciot", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("server_url", serverUrl);
                editor.apply();
                getServer();
            }
        });
        dialog.show();
    }

    public void manageFrpsIp() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("更改 Frps 代理地址");
        dialog.setItems(frpsIps, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String frpsIp = frpsIps[which];
                SharedPreferences sharedPref = getSharedPreferences("com.wrlus.seciot", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("frps_ip", frpsIp);
                editor.apply();
                getFrpsIp();
            }
        });
        dialog.show();
    }

    public void resetClientId() {
        SharedPreferences sharedPref = getSharedPreferences("com.wrlus.seciot", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        String clientId = UUID.randomUUID().toString();
        editor.putString("client_id", clientId);
        editor.apply();
        Toast.makeText(this, "重置客户端标识符成功", Toast.LENGTH_SHORT).show();
    }

}
