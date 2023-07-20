package com.mirusystems.otp;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.mirusystems.otp.databinding.ActivityStartBinding;

public class StartActivity extends AppCompatActivity {
    private static final String TAG = "StartActivity";
    private ActivityStartBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_start);

        binding.generateActivityButton.setOnClickListener(v -> nextActivity(MainActivity.class));
        binding.checkActivityButton.setOnClickListener(v -> nextActivity(SampleActivity.class));

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        setTitle(R.string.app_name);
    }

    private void nextActivity(Class<?> cls) {
        startActivity(new Intent(this, cls));
    }
}