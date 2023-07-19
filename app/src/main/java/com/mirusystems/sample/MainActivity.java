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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.mirusystems.otp.OneTimePassword;
import com.mirusystems.otp.OneTimePasswordException;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private LinearLayout pollingCenterLayout;
    private LinearLayout imeiLayout;
    private EditText pollingCenterEdit;
    private EditText pollingStationEdit;
    private EditText imeiEdit1;
    private EditText imeiEdit2;
    private EditText imeiEdit3;
    private TextView passwordEdit;
    private TextView resultText;
    private Spinner devicesSpinner;
    private Spinner permissionsSpinner;

    private AdapterView.OnItemSelectedListener itemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
            Log.v(TAG, "onItemSelected: position = " + position + ", id = " + id);
            onDeviceIdSelected(position + 1);
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    };

    private OneTimePassword oneTimePassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pollingCenterLayout = findViewById(R.id.layout_polling_center);
        imeiLayout = findViewById(R.id.layout_imei);

        pollingCenterEdit = findViewById(R.id.edit_polling_center);
        pollingStationEdit = findViewById(R.id.edit_polling_station);
        imeiEdit1 = findViewById(R.id.edit_imei1);
        imeiEdit2 = findViewById(R.id.edit_imei2);
        imeiEdit3 = findViewById(R.id.edit_imei3);

        passwordEdit = findViewById(R.id.edit_password);
        resultText = findViewById(R.id.text_result);
        devicesSpinner = findViewById(R.id.spinner_devices);
        permissionsSpinner = findViewById(R.id.spinner_permissions);

        passwordEdit.setFilters(new InputFilter[]{new InputFilter.AllCaps()});
        devicesSpinner.setAdapter(ArrayAdapter.createFromResource(this, R.array.devices, R.layout.spinner_item));
        permissionsSpinner.setAdapter(ArrayAdapter.createFromResource(this, R.array.permissions, R.layout.spinner_item));

        devicesSpinner.setOnItemSelectedListener(itemSelectedListener);
        int position = devicesSpinner.getSelectedItemPosition();
        onDeviceIdSelected(position + 1);

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
                String seed = getSeed();
                int deviceId = getDeviceId();
                int permission = getPermission();
                try {
                    String password = oneTimePassword.generatePassword(seed, deviceId, permission);
                    passwordEdit.setText(password);
                } catch (OneTimePasswordException e) {
                    e.printStackTrace();
                    resultText.setText(e.getMessage());
                    resultText.setTextColor(Color.RED);
                }
                break;
            }
            case R.id.button_check_password: {
                String password = passwordEdit.getText().toString();
                String seed = getSeed();
                int deviceId = getDeviceId();
                int permission = getPermission();
                try {
                    boolean success = oneTimePassword.checkPassword(password, seed, deviceId, permission);
                    Log.v(TAG, "onClick: button_check_password: success = " + success);
                    if (success) {
                        resultText.setText("success");
                        resultText.setTextColor(Color.DKGRAY);
                    } else {
                        resultText.setText("fail");
                        resultText.setTextColor(Color.RED);
                    }
                } catch (OneTimePasswordException e) {
                    e.printStackTrace();
                    resultText.setText(e.getMessage());
                    resultText.setTextColor(Color.RED);
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

    private String getSeed() {
        String seed;
        int deviceId = getDeviceId();
        if (deviceId == OneTimePassword.RTS) {
            seed = imeiEdit2.getText().toString() + "80";
        } else {
            seed = pollingCenterEdit.getText().toString() + pollingStationEdit.getText().toString();
        }
        return seed;
    }

    private void onDeviceIdSelected(int deviceId) {
        switch (deviceId) {
            case OneTimePassword.VVD:
            case OneTimePassword.PCOS: {
                pollingCenterLayout.setVisibility(View.VISIBLE);
                imeiLayout.setVisibility(View.INVISIBLE);

                pollingCenterEdit.setText("");
                pollingStationEdit.setText("");
                pollingCenterEdit.requestFocus();
                break;
            }
            case OneTimePassword.RTS: {
                pollingCenterLayout.setVisibility(View.INVISIBLE);
                imeiLayout.setVisibility(View.VISIBLE);

                imeiEdit1.setText("");
                imeiEdit2.setText("");
                imeiEdit3.setText("");
                imeiEdit1.requestFocus();
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
