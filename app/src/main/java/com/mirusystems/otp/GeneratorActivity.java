package com.mirusystems.otp;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.mirusystems.otp.databinding.ActivityGeneratorBinding;

public class GeneratorActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private ActivityGeneratorBinding binding;
    private OneTimePassword oneTimePassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_generator);
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
        binding.randomNumberEdit.addTextChangedListener(new TextWatcher() {
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
        binding.deviceGroup.setOnCheckedChangeListener((group, checkedId) -> {
            Log.v(TAG, "onCheckedChanged: group = [" + group + "], checkedId = [" + checkedId + "]");
            int deviceId = OneTimePassword.VVD;
            if (checkedId == R.id.pcosButton) {
                deviceId = OneTimePassword.PCOS;
            } else if (checkedId == R.id.rtsButton) {
                deviceId = OneTimePassword.RTS;
            }
            onDeviceIdSelected(deviceId);
        });
        binding.permissionGroup.setOnCheckedChangeListener((group, checkedId) -> Log.v(TAG, "onCheckedChanged: group = [" + group + "], checkedId = [" + checkedId + "]"));
        binding.generateButton.setOnClickListener(v -> generatePassword());

//        binding.passwordEdit.setFilters(new InputFilter[]{new InputFilter.AllCaps()});
        onDeviceIdSelected(OneTimePassword.VVD);

        Context context = getApplicationContext();
        oneTimePassword = new OneTimePassword(context);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        setTitle("OTP GENERATOR " + ActivityUtils.getAppVersion(this));
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
        String randomNumber = binding.randomNumberEdit.getText().toString();
        if (pollingStationId != null && randomNumber != null) {
            int deviceId = getDeviceId();
            if (deviceId == OneTimePassword.RTS) {
                if (pollingStationId.length() == 6 && randomNumber.length() == 2) {
                    setButtonEnabled(true);
                    return;
                }
            } else {
                if (pollingStationId.length() == 8 && randomNumber.length() == 2) {
                    setButtonEnabled(true);
                    return;
                }
            }
        }
        setButtonEnabled(false);
    }

    private void setButtonEnabled(boolean enabled) {
        runOnUiThread(() -> {
            binding.generateButton.setEnabled(enabled);
        });
    }

    private void generatePassword() {
        String seed = getSeed();
        int deviceId = getDeviceId();
        int permission = getPermission();
        String randomNumber = binding.randomNumberEdit.getText().toString();
        try {
            String password = oneTimePassword.generatePassword(seed, deviceId, permission, randomNumber);
            password = String.format("%s-%s-%s", password.substring(0, 3), password.substring(3, 6), password.substring(6)); // 관리자가 읽기 편하도록 3-3-4로 표시
            binding.passwordText.setText(password);
        } catch (OneTimePasswordException e) {
            e.printStackTrace();
        }
    }

    private int getDeviceId() {
        switch (binding.deviceGroup.getCheckedRadioButtonId()) {
            case R.id.pcosButton:
                return OneTimePassword.PCOS;
            case R.id.rtsButton:
                return OneTimePassword.RTS;
        }
        return OneTimePassword.VVD;
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
                binding.pollingStationEdit.setHint("Polling station ID (8 digits)");
                binding.pollingStationEdit.requestFocus();
                binding.pollingStationEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(8)});
                binding.randomNumberEdit.setText("");
                binding.passwordText.setText("");
                break;
            }
            case OneTimePassword.RTS: {
                binding.pollingStationEdit.setText("");
                binding.pollingStationEdit.setHint("The middle 6 digits of \"SAT IMEI\"");
                binding.pollingStationEdit.requestFocus();
                binding.pollingStationEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
                binding.randomNumberEdit.setText("");
                binding.passwordText.setText("");
                break;
            }
        }
    }

}
