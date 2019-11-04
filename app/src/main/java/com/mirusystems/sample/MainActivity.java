package com.mirusystems.sample;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.mirusystems.otp.OneTimePassword;
import com.mirusystems.otp.OneTimePasswordException;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private EditText seedEdit;
    private TextView passwordEdit;
    private TextView passwordCheckResultText;
    private Spinner devicesSpinner;
    private Spinner permissionsSpinner;

    private OneTimePassword oneTimePassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        seedEdit = findViewById(R.id.edit_seed);
        passwordEdit = findViewById(R.id.edit_password);
        passwordCheckResultText = findViewById(R.id.text_password_check_result);
        devicesSpinner = findViewById(R.id.spinner_devices);
        permissionsSpinner = findViewById(R.id.spinner_permissions);

//        passwordEdit.setFilters(new InputFilter[]{new InputFilter.AllCaps()});
        devicesSpinner.setAdapter(ArrayAdapter.createFromResource(this, R.array.devices, R.layout.spinner_item));
        permissionsSpinner.setAdapter(ArrayAdapter.createFromResource(this, R.array.permissions, R.layout.spinner_item));

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

    public void onClick(View view) {
        int id = view.getId();
        Log.v(TAG, "onClick: id = " + id);
        switch (id) {
            case R.id.button_generate_password: {
                String seed = seedEdit.getText().toString();
                int deviceId = getDeviceId();
                int permission = getPermission();
                try {
                    String password = oneTimePassword.generatePassword(seed, deviceId, permission);
                    passwordEdit.setText(password);
                } catch (OneTimePasswordException e) {
                    e.printStackTrace();
                    passwordEdit.setText(e.getMessage());
                }
                break;
            }
            case R.id.button_check_password: {
                String password = passwordEdit.getText().toString();
                String seed = seedEdit.getText().toString();
                int deviceId = getDeviceId();
                int permission = getPermission();
                try {
                    boolean success = oneTimePassword.checkPassword(password, seed, deviceId, permission);
                    Log.v(TAG, "onClick: button_check_password: success = " + success);
                    if (success) {
                        passwordCheckResultText.setText("success");
                        passwordCheckResultText.setTextColor(Color.DKGRAY);
                    } else {
                        passwordCheckResultText.setText("fail");
                        passwordCheckResultText.setTextColor(Color.RED);
                    }
                } catch (OneTimePasswordException e) {
                    e.printStackTrace();
                    passwordCheckResultText.setText(e.getMessage());
                    passwordCheckResultText.setTextColor(Color.RED);
                }
                break;
            }
            case R.id.layout_root: {
                hideKeyboard(this);
                break;
            }
        }
    }

    private int getDeviceId() {
        int deviceId = devicesSpinner.getSelectedItemPosition() + 1;
        return deviceId;
    }

    private int getPermission() {
        int permission = permissionsSpinner.getSelectedItemPosition();
        if (permission != OneTimePassword.ADMIN) {
            permission = OneTimePassword.SUPER_ADMIN;
        }
        return permission;
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
