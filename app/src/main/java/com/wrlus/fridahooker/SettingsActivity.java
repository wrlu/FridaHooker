package com.wrlus.fridahooker;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import java.util.UUID;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        bindWidget();
        bindWidgetEvent();
    }

    public void bindWidget() {

    }

    public void bindWidgetEvent() {

    }

    public void resetClientId() {
        SharedPreferences sharedPref = getSharedPreferences("com.wrlus.seciot", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        String clientId = UUID.randomUUID().toString();
        editor.putString("client_id", clientId);
        editor.apply();
        Toast.makeText(this, "重置客户端ID成功", Toast.LENGTH_SHORT).show();
    }

}
