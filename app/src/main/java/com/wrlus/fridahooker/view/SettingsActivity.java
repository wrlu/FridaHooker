package com.wrlus.fridahooker.view;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.wrlus.fridahooker.R;

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

}
