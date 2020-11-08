package com.IoT.personalroomcare;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.core.content.res.ResourcesCompat;

import android.Manifest;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.ficat.easyble.BleDevice;
import com.ficat.easyble.scan.BleScanCallback;
import com.ficat.easypermissions.EasyPermissions;
import com.ficat.easypermissions.RequestExecutor;
import com.ficat.easypermissions.bean.Permission;
import com.ficat.easyble.gatt.bean.ServiceInfo;
import com.ficat.easyble.gatt.callback.BleConnectCallback;
import com.ficat.easyble.gatt.callback.BleNotifyCallback;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.ficat.easyble.BleManager;

import static java.lang.Math.max;
import static java.lang.Math.min;


enum Status {
    GOOD,
    MODERATE,
    UNHEALTHY_LITE,
    UNHEALTHY,
    VERY_UNHEALTHY,
    HAZARDOUS,
    NONE,
}

enum ConnectionStatus {
    OFF,
    CONNECTING,
    ON
}

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    // Hardcoded BLE connection values
    private final static String TAG = "PRC";
    private final static String esp32Name = "UART Service";
    private final static String serviceUUID = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    private final static String characteristicUUID = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";

    private List<Integer> arrayAQI = new ArrayList<>();
    private List<Date> arrayTime = new ArrayList<>();

    private Status s;

    public BleManager manager;
    public BleDevice esp32 = null;
    public ConnectionStatus bleConnected = ConnectionStatus.OFF;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);


        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_dashboard, R.id.navigation_info
        ).build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupWithNavController(navView, navController);
        this.s = Status.NONE;
        initBleManager();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        BleManager.getInstance().destroy();
    }

    private void initBleManager() {
        if(!BleManager.supportBle(this)) {
            return;
        }

        // Open Bluetooth without a request dialog
        BleManager.toggleBluetooth(true);

        BleManager.ScanOptions scanOptions = BleManager.ScanOptions
                .newInstance()
                .scanPeriod(2000)
                .scanDeviceName(null);

        BleManager.ConnectOptions connectOptions = BleManager.ConnectOptions
                .newInstance()
                .connectTimeout(12000);

        manager = BleManager
                .getInstance()
                .setScanOptions(scanOptions)
                .setConnectionOptions(connectOptions)
                .setLog(true, "PersonalRoomCare")
                .init(this.getApplication());
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.connection_card:
                // Toggle BLE
                if(bleConnected == ConnectionStatus.OFF) {
                    bleConnected = ConnectionStatus.CONNECTING;
                    updateConnection("...");
                    initBleScan();
                } else if(bleConnected == ConnectionStatus.ON) {
                    turnOffBle();
                    updateConnectionText(false);
                }

                break;
            default:
                break;
        }

    }

    public void initBleScan() {
        // Init BLE
        if(!BleManager.isBluetoothOn()) {
            BleManager.toggleBluetooth(true);
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !isGpsOn()) {
            Toast.makeText(this, getResources().getString(R.string.tips_turn_on_gps), Toast.LENGTH_LONG).show();
            return;
        }
        EasyPermissions
                .with(this)
                .request(Manifest.permission.ACCESS_FINE_LOCATION)
                .autoRetryWhenUserRefuse(true, null)
                .result(new RequestExecutor.ResultReceiver() {
                    @Override
                    public void onPermissionsRequestResult(boolean grantAll, List<Permission> results) {
                        if (grantAll) {
                            if (!manager.isScanning()) {
                                starScan();
                            }
                        } else {
                            Toast.makeText(MainActivity.this,
                                    getResources().getString(R.string.tips_go_setting_to_grant_location),
                                    Toast.LENGTH_LONG).show();
                            EasyPermissions.goToSettingsActivity(MainActivity.this);
                        }
                    }
                });
    }

    private void turnOffBle() {
        manager.disconnectAll();;
    }

    private boolean isGpsOn() {
        // Check if GPS is on
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public void starScan() {
        // Start BLE scanning
        manager.startScan(new BleScanCallback() {
            @Override
            public void onLeScan(BleDevice device, int rssi, byte[] scanRecord) {
                Log.e(TAG, "Found " + device.name);
                if(device.name.equals(esp32Name) && esp32 == null) {
                    Toast.makeText(MainActivity.this, "Found ESP32", Toast.LENGTH_SHORT).show();
                    esp32 = device;
                }
            }

            @Override
            public void onStart(boolean startScanSuccess, String info) {
                Log.e(TAG, "start scan = " + startScanSuccess + "    info " + info);
                Toast.makeText(MainActivity.this, "Start scanning", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFinish() {
                Log.e(TAG, "scan finish");

                if(esp32 != null) {
                    Log.e(TAG, "Start connection");
                    updateConnectionText(true);
                    manager.connect(esp32, connectCallback);
                } else {
                    Toast.makeText(MainActivity.this, "ESP32 not found", Toast.LENGTH_SHORT).show();
                    updateConnectionText(false);
                    turnOffBle();
                }
            }
        });
    }

    private BleConnectCallback connectCallback = new BleConnectCallback() {
        @Override
        public void onStart(boolean startConnectSuccess, String info, BleDevice device) {
            Log.e(TAG, "start connecting:" + startConnectSuccess + "    info=" + info);
            if (!startConnectSuccess) {
                Toast.makeText(MainActivity.this, "start connecting fail:" + info, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onConnected(BleDevice device) {
            Log.e(TAG, "Connected");
            Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();
            BleManager.getInstance().notify(esp32, serviceUUID, characteristicUUID,  notifyCallback);

        }

        @Override
        public void onDisconnected(String info, int status, BleDevice device) {
            Log.e(TAG, "Disconnected");
            Toast.makeText(MainActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
            esp32 = null;
            updateConnectionText(false);
        }

        @Override
        public void onFailure(int failCode, String info, BleDevice device) {
            Log.e(TAG, "Connection Failed");
            Toast.makeText(MainActivity.this, "Connection Failed", Toast.LENGTH_SHORT).show();
            esp32 = null;
            updateConnectionText(false);
        }
    };

    private BleNotifyCallback notifyCallback = new BleNotifyCallback() {
        @Override
        public void onCharacteristicChanged(byte[] data, BleDevice device) {
             // Convert bytes to integer
            int value = 0;
            for (int i = data.length-1; i >= 0; i--) {
                value += (value << 8) + (data[i] & 0xFF);
            }

            updateAQI(value);

            Log.e(TAG, "onCharacteristicChanged:" + value);
        }

        @Override
        public void onNotifySuccess(String notifySuccessUuid, BleDevice device) {
            Log.e(TAG, "notify success uuid:" + notifySuccessUuid);
        }

        @Override
        public void onFailure(int failCode, String info, BleDevice device) {
            Log.e(TAG, "notify fail:" + info);
            Toast.makeText(MainActivity.this, "notify fail:" + info, Toast.LENGTH_LONG).show();
        }
    };

    public void updateConnectionText(boolean connection) {
        if(connection) {
            this.bleConnected = ConnectionStatus.ON;
        } else {

            this.bleConnected = ConnectionStatus.OFF;
            this.s = Status.NONE;
            updateRecommendation();
        }
        this.updateDeviceUI();
    }

    public void updateDeviceUI() {
        if(this.bleConnected == ConnectionStatus.ON) {
            this.updateConnection("On");
            this.updateDevice("ESP32");
        } else if(this.bleConnected == ConnectionStatus.OFF) {
            this.updateConnection("Off");
            updateDevice("NA");
            this.updateLabelAQI("NA");
            this.updateValueAQI(0);
            this.updateColors();
        }
    }



    private void updateRecommendation() {
        TextView recommendationText = findViewById(R.id.recommendation_text);
        if(recommendationText == null) {
            return;
        }

        switch (this.s) {
            case NONE:
                recommendationText.setText(getString(R.string.status_text));
                break;
            case GOOD:
                recommendationText.setText(getString(R.string.status_text_good));
                break;
            case MODERATE:
                recommendationText.setText(getString(R.string.status_text_moderate));
                break;
            case UNHEALTHY_LITE:
                recommendationText.setText(getString(R.string.status_text_unhealthy_lite));
                break;
            case UNHEALTHY:
                recommendationText.setText(getString(R.string.status_text_unhealthy));
                break;
            case VERY_UNHEALTHY:
                recommendationText.setText(getString(R.string.status_text_very_healthy));
                break;
            case HAZARDOUS:
                recommendationText.setText(getString(R.string.status_text_hazardous));
                break;
        }
    }


    private void updateConnection(String connection) {
        TextView valueConnection = findViewById(R.id.connection_value);
        if(valueConnection != null) {
            valueConnection.setText(connection);
        }
    }

    private void updateDevice(String device) {
        TextView valueDevice = findViewById(R.id.device_value);
        if(valueDevice != null) {
            valueDevice.setText(device);
        }
    }

    private void updateAQI(int value) {
        this.updateDeviceUI(); // Ugly fix to UI when switching fragments
        Date currentTime = Calendar.getInstance().getTime();
        int AQI = max(min(value, 500), 0);

        // Save the value to an array
        this.arrayAQI.add(AQI);
        this.arrayTime.add(currentTime);

        // Remove values when array is too large
        if(this.arrayAQI.size() > 10000) {
            this.arrayAQI.remove(0);
        }

        if(this.arrayTime.size() > 10000) {
            this.arrayTime.remove(0);
        }

        // Update the UI values
        this.updateValueAQI(AQI);
        if(AQI <= 50) {
            this.s = Status.GOOD;
            updateLabelAQI("Good");
        } else if(AQI <= 100) {
            this.s = Status.MODERATE;
            updateLabelAQI("Moderate");
        } else if(AQI <= 150) {
            this.s = Status.UNHEALTHY_LITE;
            updateLabelAQI("Unhealthy for some");
        } else if(AQI <= 200) {
            this.s = Status.UNHEALTHY;
            updateLabelAQI("Unhealthy");
        } else if(AQI <= 300) {
            this.s = Status.VERY_UNHEALTHY;
            updateLabelAQI("Very unhealthy");
        } else {
            this.s = Status.HAZARDOUS;
            updateLabelAQI("Hazardous");
        }

        this.updateColors();
        this.updateRecommendation();

    }

    private void updateValueAQI(int AQI) {
        TextView valueAQI = findViewById(R.id.aqi_status_value);
        if(valueAQI != null) {
            valueAQI.setText(String.format(Locale.getDefault(),"%d", AQI));
        } else {
            Log.e(TAG, "Couldn't update the AQI value");
        }
    }

    private void updateLabelAQI(String status) {
        TextView statusTextAQI = findViewById(R.id.aqi_status_text);
        if(statusTextAQI != null) {
            statusTextAQI.setText(status);
        } else {
            Log.e(TAG, "Couldn't update the AQI label");
        }
    }

    private void updateColors() {
        int color;
        int colorLight;
        int colorDark;
        Drawable gradient;
        switch (this.s) {
            case MODERATE:
                color = ResourcesCompat.getColor(getResources(), R.color.AQI_moderate, null);
                colorLight = ResourcesCompat.getColor(getResources(), R.color.AQI_moderate_light, null);
                colorDark = ResourcesCompat.getColor(getResources(), R.color.AQI_moderate_dark, null);
                gradient = ResourcesCompat.getDrawable(getResources(), R.drawable.gradient_moderate, null);
                break;

            case UNHEALTHY_LITE:
                color = ResourcesCompat.getColor(getResources(), R.color.AQI_unhealthy_lite, null);
                colorLight = ResourcesCompat.getColor(getResources(), R.color.AQI_unhealthy_lite_light, null);
                colorDark = ResourcesCompat.getColor(getResources(), R.color.AQI_unhealthy_lite_dark, null);
                gradient = ResourcesCompat.getDrawable(getResources(), R.drawable.gradient_unhealthy_lite, null);
                break;

            case UNHEALTHY:
                color = ResourcesCompat.getColor(getResources(), R.color.AQI_unhealthy, null);
                colorLight = ResourcesCompat.getColor(getResources(), R.color.AQI_unhealthy_light, null);
                colorDark = ResourcesCompat.getColor(getResources(), R.color.AQI_unhealthy_dark, null);
                gradient = ResourcesCompat.getDrawable(getResources(), R.drawable.gradient_unhealthy, null);
                break;

            case VERY_UNHEALTHY:
                color = ResourcesCompat.getColor(getResources(), R.color.AQI_very_unhealthy, null);
                colorLight = ResourcesCompat.getColor(getResources(), R.color.AQI_very_unhealthy_light, null);
                colorDark = ResourcesCompat.getColor(getResources(), R.color.AQI_very_unhealthy_dark, null);
                gradient = ResourcesCompat.getDrawable(getResources(), R.drawable.gradient_very_unhealthy, null);
                break;

            case HAZARDOUS:
                color = ResourcesCompat.getColor(getResources(), R.color.AQI_hazardous, null);
                colorLight = ResourcesCompat.getColor(getResources(), R.color.AQI_hazardous_light, null);
                colorDark = ResourcesCompat.getColor(getResources(), R.color.AQI_hazardous_dark, null);
                gradient = ResourcesCompat.getDrawable(getResources(), R.drawable.gradient_hazardous, null);
                break;

            default:
                color = ResourcesCompat.getColor(getResources(), R.color.AQI_good, null);
                colorLight = ResourcesCompat.getColor(getResources(), R.color.AQI_good_light, null);
                colorDark = ResourcesCompat.getColor(getResources(), R.color.AQI_good_dark, null);
                gradient = ResourcesCompat.getDrawable(getResources(), R.drawable.gradient_good, null);
        }

        getWindow().setStatusBarColor(colorDark);

        CardView connectionCard = findViewById(R.id.connection_card);
        if(connectionCard != null) connectionCard.setCardBackgroundColor(color);

        CardView recommendationCard = findViewById(R.id.recommendation_card);
        if(recommendationCard != null) recommendationCard.setCardBackgroundColor(color);

        CardView deviceCard = findViewById(R.id.device_card);
        if(deviceCard != null) deviceCard.setCardBackgroundColor(color);

        ConstraintLayout aqiLayout = findViewById(R.id.air_quality_index_layout);
        if(aqiLayout != null) aqiLayout.setBackground(gradient);

    }


}