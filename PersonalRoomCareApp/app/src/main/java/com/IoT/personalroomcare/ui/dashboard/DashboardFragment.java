package com.IoT.personalroomcare.ui.dashboard;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.IoT.personalroomcare.MainActivity;
import com.IoT.personalroomcare.R;
import com.ficat.easyble.BleManager;
//import com.IoT.personalroomcare.Status;

import java.util.Locale;

enum Status {
    GOOD,
    MODERATE,
    UNHEALTHY_LITE,
    UNHEALTHY,
    VERY_UNHEALTHY,
    HAZARDOUS
}

public class DashboardFragment extends Fragment {

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
    private BleManager manager;
    private MainActivity mainAct;


//    private DashboardViewModel dashboardViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
//        dashboardViewModel =
//                ViewModelProviders.of(this).get(DashboardViewModel.class);
        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);
//        final TextView textView = root.findViewById(R.id.text_dashboard);
//        dashboardViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
//            @Override
//            public void onChanged(@Nullable String s) {
//                textView.setText(s);
//            }
//        });

        cardAQI = root.findViewById(R.id.air_quality_index_card);
        cardHumidity = root.findViewById(R.id.humidity_card);
        cardTemperature = root.findViewById(R.id.temperature_card);
        cardStatus = root.findViewById(R.id.recommendation_card);
        layoutAQI = root.findViewById(R.id.air_quality_index_layout);
//        window = root.getWindow();

        valueAQI = root.findViewById(R.id.aqi_status_value);
        textAQI = root.findViewById(R.id.aqi_status_text);
        valueTemperature = root.findViewById(R.id.temperature_value);
        valueHumidity = root.findViewById(R.id.humidity_value);

        s = Status.GOOD;
        updateColors(s);

        updateTemperature(25);
        updateHumidity(80);
        updateAQI(40);

        cardStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Temporary way to change the color of the views
                s = Status.values()[(s.ordinal()+1) % (Status.values().length)];
                updateColors(s);
            }
        });

        mainAct = (MainActivity)getActivity();
        manager = mainAct.manager;

        bleButton = root.findViewById(R.id.ble_btn);
        bleButton.setOnClickListener(mainAct);


        return root;
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
//        this.window.setStatusBarColor(colorDark);
    }

    public void updateTemperature(int temperature) {
        this.valueTemperature.setText(String.format(Locale.getDefault(),"%d°C", temperature));
    }

    public void updateHumidity(int humidity) {
        this.valueHumidity.setText(String.format(Locale.getDefault(),"%d%%", humidity));
    }

    public void updateAQI(int AQI) {
        this.valueAQI.setText(String.format(Locale.getDefault(),"%d", AQI));
    }

}