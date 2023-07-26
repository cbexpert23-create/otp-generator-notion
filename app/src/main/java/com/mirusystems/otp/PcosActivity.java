package com.mirusystems.otp;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.mirusystems.otp.databinding.ActivityPcosBinding;

public class PcosActivity extends AppCompatActivity {
    private static final String TAG = "PcosActivity";
    private ActivityPcosBinding binding;
    private OneTimePassword oneTimePassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_pcos);

        binding.rootLayout.setOnTouchListener((v, event) -> {
            ActivityUtils.hideKeyboard(this);
            return false;
        });
        binding.pollingStationEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                Log.v(TAG, "afterTextChanged: s = [" + s + "]");
                onSeedTextChanged();
            }
        });
        binding.passwordEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                Log.v(TAG, "afterTextChanged: s = [" + s + "]");
                onSeedTextChanged();
            }
        });

        binding.confirmButton.setOnClickListener(v -> checkPassword());

        binding.resetButton.setOnClickListener(v -> resetPassword());

        Context context = getApplicationContext();
        oneTimePassword = new OneTimePassword(context);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        setTitle("PCOS SAMPLE " + ActivityUtils.getAppVersion(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        onSeedTextChanged();
        generateRandomNumber();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void onSeedTextChanged() {
        String pollingStationId = binding.pollingStationEdit.getText().toString();
        String password = binding.passwordEdit.getText().toString();
        if (pollingStationId.length() == 8 && password.length() == 10) {
            setButtonEnabled(true);
            return;
        }
        setButtonEnabled(false);
    }

    private void setButtonEnabled(boolean enabled) {
        runOnUiThread(() -> {
            binding.confirmButton.setEnabled(enabled);
        });
    }

    private void generateRandomNumber() {
        try {
            String random = oneTimePassword.generateRandomNumber();
            binding.randomNumberText.setText(random);
        } catch (OneTimePasswordException e) {
            e.printStackTrace();
            binding.randomNumberText.setText("");
            binding.resultText.setText(e.getMessage());
            binding.resultText.setTextColor(Color.RED);
        }
    }

    private void resetPassword() {
        oneTimePassword.resetPassword();
    }

    private void checkPassword() {
        ActivityUtils.hideKeyboard(this);
        String password = binding.passwordEdit.getText().toString();
        String seed = binding.pollingStationEdit.getText().toString();
        String salt = binding.randomNumberText.getText().toString();
        try {
            boolean success = oneTimePassword.checkPassword(password, seed, OneTimePassword.PCOS, salt);
            Log.v(TAG, "onClick: button_check_password: success = " + success);
            if (success) {
                binding.resultText.setText("SUCCESS");
                binding.resultText.setTextColor(Color.DKGRAY);
            } else {
                binding.resultText.setText("FAILURE");
                binding.resultText.setTextColor(Color.RED);
            }
        } catch (OneTimePasswordException e) {
            e.printStackTrace();
            binding.resultText.setText(e.getMessage());
            binding.resultText.setTextColor(Color.RED);
        }
    }

}