package com.mirusystems.otp;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.mirusystems.otp.databinding.ActivityPcosBinding;

public class PcosActivity extends AppCompatActivity {
    private static final String TAG = "SampleActivity";
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
        binding.permissionGroup.setOnCheckedChangeListener((group, checkedId) -> Log.v(TAG, "onCheckedChanged: group = [" + group + "], checkedId = [" + checkedId + "]"));

        binding.confirmButton.setOnClickListener(v -> checkPassword());
        onDeviceIdSelected(OneTimePassword.PCOS);

        binding.resetButton.setOnClickListener(v -> resetPassword());

        Context context = getApplicationContext();
        oneTimePassword = new OneTimePassword(context);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        setTitle("PCOS " + ActivityUtils.getAppVersion(this));
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
        String randomNumber = binding.randomNumberText.getText().toString();
        String password = binding.passwordEdit.getText().toString();
        if (pollingStationId != null && randomNumber != null && password != null) {
            int deviceId = getDeviceId();
            if (deviceId == OneTimePassword.RTS) {
                if (pollingStationId.length() == 6 && randomNumber.length() == 2 && password.length() == 10) {
                    setButtonEnabled(true);
                    return;
                }
            } else {
                if (pollingStationId.length() == 8 && randomNumber.length() == 2 && password.length() == 10) {
                    setButtonEnabled(true);
                    return;
                }
            }
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
        String seed = getSeed();
        int deviceId = getDeviceId();
        int permission = getPermission();
        String randomNumber = binding.randomNumberText.getText().toString();
        try {
            boolean success = oneTimePassword.checkPassword(password, seed, deviceId, permission, randomNumber);
            Log.v(TAG, "onClick: button_check_password: success = " + success);
            if (success) {
                binding.resultText.setText("success");
                binding.resultText.setTextColor(Color.DKGRAY);
            } else {
                binding.resultText.setText("fail");
                binding.resultText.setTextColor(Color.RED);
            }
        } catch (OneTimePasswordException e) {
            e.printStackTrace();
            binding.resultText.setText(e.getMessage());
            binding.resultText.setTextColor(Color.RED);
        }
    }

    private int getDeviceId() {
        return OneTimePassword.PCOS;
    }

    private int getPermission() {
        if (binding.permissionGroup.getCheckedRadioButtonId() == R.id.superAdminButton) {
            return OneTimePassword.SUPER_ADMIN;
        }
        return OneTimePassword.ADMIN;
    }

    private String getSeed() {
        String seed;
        int deviceId = getDeviceId();
        if (deviceId == OneTimePassword.RTS) {
            seed = binding.pollingStationEdit.getText().toString() + "80";
        } else {
            seed = binding.pollingStationEdit.getText().toString();
        }
        return seed;
    }

    private void onDeviceIdSelected(int deviceId) {
        switch (deviceId) {
            case OneTimePassword.VVD:
            case OneTimePassword.PCOS: {
                binding.pollingStationEdit.setText("");
                binding.pollingStationEdit.setHint("polling station id (8 digits)");
                binding.pollingStationEdit.requestFocus();
                binding.pollingStationEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(8)});
                binding.randomNumberText.setText("");
                break;
            }
            case OneTimePassword.RTS: {
                binding.pollingStationEdit.setText("");
                binding.pollingStationEdit.setHint("SAT IMEI (6 digits)");
                binding.pollingStationEdit.requestFocus();
                binding.pollingStationEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
                binding.randomNumberText.setText("");
                break;
            }
        }
    }
}