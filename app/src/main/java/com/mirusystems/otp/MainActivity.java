package com.mirusystems.otp;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.mirusystems.otp.OneTimePassword;
import com.mirusystems.otp.OneTimePasswordException;
import com.mirusystems.otp.R;
import com.mirusystems.otp.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private ActivityMainBinding binding;
    private OneTimePassword oneTimePassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.rootLayout.setOnTouchListener((v, event) -> {
            hideKeyboard(this);
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
        binding.checkButton.setOnClickListener(v -> checkPassword());

        binding.passwordEdit.setFilters(new InputFilter[]{new InputFilter.AllCaps()});
        onDeviceIdSelected(OneTimePassword.VVD);

        Context context = getApplicationContext();
        oneTimePassword = new OneTimePassword(context);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
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
            binding.checkButton.setEnabled(enabled);
        });
    }

    private void generatePassword() {
        String seed = getSeed();
        int deviceId = getDeviceId();
        int permission = getPermission();
        try {
            String password = oneTimePassword.generatePassword(seed, deviceId, permission);
            binding.passwordEdit.setText(password);
        } catch (OneTimePasswordException e) {
            e.printStackTrace();
            binding.resultText.setText(e.getMessage());
            binding.resultText.setTextColor(Color.RED);
        }
    }

    private void checkPassword() {
        String password = binding.passwordEdit.getText().toString();
        String seed = getSeed();
        int deviceId = getDeviceId();
        int permission = getPermission();
        try {
            boolean success = oneTimePassword.checkPassword(password, seed, deviceId, permission);
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
            seed = binding.pollingStationEdit.getText().toString() + "80" + binding.randomNumberEdit.getText().toString();
        } else {
            seed = binding.pollingStationEdit.getText().toString() + binding.randomNumberEdit.getText().toString();
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
                binding.randomNumberEdit.setText("");
                break;
            }
            case OneTimePassword.RTS: {
                binding.pollingStationEdit.setText("");
                binding.pollingStationEdit.setHint("SAT IMEI (6 digits)");
                binding.pollingStationEdit.requestFocus();
                binding.randomNumberEdit.setText("");
                break;
            }
        }
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = activity.getCurrentFocus();
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
