package com.IoT.personalroomcare.ui.dashboard;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.IoT.personalroomcare.MainActivity;
import com.IoT.personalroomcare.R;
import com.ficat.easyble.BleManager;

//import com.IoT.personalroomcare.Status;



public class DashboardFragment extends Fragment {

    public TextView valueAQI;
    public TextView statusTextAQI;
    public TextView valueConnection;
    public TextView valueDevice;
    public TextView recommendationText;
    private MainActivity mainAct;

    private CardView cardConnection;



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


        this.cardConnection = root.findViewById(R.id.connection_card);




//        cardStatus.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                // Temporary way to change the color of the views
//                s = Status.values()[(s.ordinal()+1) % (Status.values().length)];
//                updateColors(s);
//            }
//        });


        mainAct = (MainActivity)getActivity();
        mainAct.updateDeviceUI();


        cardConnection.setOnClickListener(mainAct);

//        bleButton = root.findViewById(R.id.ble_btn);
//        bleButton.setOnClickListener(mainAct);


        return root;
    }



}