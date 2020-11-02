package com.IoT.personalroomcare;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import android.Manifest;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.IoT.personalroomcare.ui.dashboard.DashboardFragment;
import com.ficat.easyble.BleDevice;
import com.ficat.easyble.scan.BleScanCallback;
import com.ficat.easypermissions.EasyPermissions;
import com.ficat.easypermissions.RequestExecutor;
import com.ficat.easypermissions.bean.Permission;
import com.ficat.easyble.gatt.bean.CharacteristicInfo;
import com.ficat.easyble.gatt.bean.ServiceInfo;
import com.ficat.easyble.gatt.callback.BleConnectCallback;
import com.ficat.easyble.gatt.callback.BleNotifyCallback;
import com.ficat.easyble.gatt.callback.BleReadCallback;
import com.ficat.easyble.gatt.callback.BleRssiCallback;
import com.ficat.easyble.gatt.callback.BleWriteCallback;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.ficat.easyble.BleManager;


enum Status {
    GOOD,
    MODERATE,
    UNHEALTHY_LITE,
    UNHEALTHY,
    VERY_UNHEALTHY,
    HAZARDOUS
}

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private final static String TAG = "PRC";
    private final static String esp32Name = "ESP32 UART Test";
    private final static String serviceUUID = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    private final static String characteristicUUID = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";

    private CardView cardAQI;
    private CardView cardHumidity;
    private CardView cardTemperature;
    private CardView cardStatus;
    private Status s;
    private ConstraintLayout layoutAQI;
    private Window window;
    private TextView valueAQI;
    private TextView textAQI;
    private TextView valueTemperature;
    private TextView valueHumidity;
    private Button bleButton;

    public BleManager manager;
    public BleDevice esp32 = null;
    public boolean bleConnected = false;
    public ServiceInfo curService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);


        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_dashboard, R.id.navigation_debug
        ).build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
//        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        initBleManager();
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
            case R.id.ble_btn:
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

                break;
            default:
                break;
        }



    }

    private boolean isGpsOn() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public void starScan() {
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
                Toast.makeText(MainActivity.this, "Finished scanning", Toast.LENGTH_SHORT).show();
                if(esp32 != null) {
                    Log.e(TAG, "Start connection");
                    manager.connect(esp32, connectCallback);
                }
            }
        });
    }

    private BleConnectCallback connectCallback = new BleConnectCallback() {
        @Override
        public void onStart(boolean startConnectSuccess, String info, BleDevice device) {
            Log.e(TAG, "start connecting:" + startConnectSuccess + "    info=" + info);
            if (!startConnectSuccess) {
                Toast.makeText(MainActivity.this, "start connecting fail:" + info, Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onConnected(BleDevice device) {
            Log.e(TAG, "Connected");
            Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_LONG).show();
            BleManager.getInstance().notify(esp32, serviceUUID, characteristicUUID,  notifyCallback);

        }

        @Override
        public void onDisconnected(String info, int status, BleDevice device) {
            Log.e(TAG, "Disconnected");
            Toast.makeText(MainActivity.this, "Disconnected", Toast.LENGTH_LONG).show();
            esp32 = null;
        }

        @Override
        public void onFailure(int failCode, String info, BleDevice device) {
            Log.e(TAG, "Connection Failed");
            Toast.makeText(MainActivity.this, "Connection Failed", Toast.LENGTH_LONG).show();
        }
    };

    private BleNotifyCallback notifyCallback = new BleNotifyCallback() {
        @Override
        public void onCharacteristicChanged(byte[] data, BleDevice device) {
//            String s = bytes2HexStr(data);
//            ByteBuffer wrapped = ByteBuffer.wrap(data);
//            short s = wrapped.getShort();
            int value = 0;
            for (byte datum : data) {
                value += (value << 8) + (datum & 0xFF);
            }

            TextView valueAQI = findViewById(R.id.aqi_status_value);
            if(valueAQI != null) {
                valueAQI.setText(String.format(Locale.getDefault(),"%d", value));
            }

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




    public static String bytes2HexStr(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        StringBuilder b = new StringBuilder();
        Log.e(TAG, "String length " + bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            b.append(String.format("%02x ", bytes[i]));
        }
        return b.toString();
    }



        private void updateColors(Status s) {
        int color;
        int colorLight;
        int colorDark;
        Drawable gradient;
        switch (s) {
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
        this.cardTemperature.setCardBackgroundColor(colorLight);
        this.cardHumidity.setCardBackgroundColor(colorLight);
        this.cardStatus.setCardBackgroundColor(colorLight);
        this.layoutAQI.setBackground(gradient);
        this.window.setStatusBarColor(colorDark);
    }

    private void updateTemperature(int temperature) {
        this.valueTemperature.setText(String.format(Locale.getDefault(),"%d°C", temperature));
    }

    private void updateHumidity(int humidity) {
        this.valueHumidity.setText(String.format(Locale.getDefault(),"%d%%", humidity));
    }

    private void updateAQI(int AQI) {
        this.valueAQI.setText(String.format(Locale.getDefault(),"%d", AQI));
    }
}