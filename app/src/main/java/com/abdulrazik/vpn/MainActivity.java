package com.abdulrazik.vpn;

import android.app.Activity;
import android.net.VpnService;
import android.content.Intent;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

public class MainActivity extends Activity {

    private static final int VPN_REQUEST_CODE = 100;
    private TextView status;
    private TextView powerButton;
    private Spinner countries;

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable gradient(int color1, int color2, int radius) {
        GradientDrawable shape = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{color1, color2}
        );
        shape.setCornerRadius(dp(radius));
        return shape;
    }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(25), dp(20), dp(25), dp(20));
        root.setBackgroundColor(Color.rgb(15, 15, 15));

        TextView menu = text("☰", 35, Color.WHITE);
        menu.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        root.addView(menu, new LinearLayout.LayoutParams(
            -1, dp(55)
        ));

        TextView title = text("ABDULRAZIK VPN", 28, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams =
            new LinearLayout.LayoutParams(-1, dp(100));
        titleParams.topMargin = dp(15);
        root.addView(title, titleParams);

        TextView traffic = text("↑  0 B                 ↓  0 B", 18, Color.WHITE);
        LinearLayout.LayoutParams trafficParams =
            new LinearLayout.LayoutParams(-1, dp(90));
        trafficParams.topMargin = dp(35);
        root.addView(traffic, trafficParams);

        powerButton = text("⏻\nSTART", 27, Color.WHITE);
        powerButton.setTypeface(null, Typeface.BOLD);
        powerButton.setBackground(gradient(
            Color.rgb(255, 145, 0),
            Color.rgb(240, 25, 35),
            200
        ));
        LinearLayout.LayoutParams powerParams =
            new LinearLayout.LayoutParams(dp(190), dp(190));
        powerParams.gravity = Gravity.CENTER;
        powerParams.topMargin = dp(20);
        powerParams.bottomMargin = dp(65);
        root.addView(powerButton, powerParams);

        TextView serverLabel = text(
            "🇩🇪   Select Server", 16, Color.WHITE
        );
        serverLabel.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        root.addView(serverLabel, new LinearLayout.LayoutParams(
            -1, dp(35)
        ));

        String[] items = {
            "Automatic",
            "Germany",
            "Turkey",
            "USA",
            "United Kingdom"
        };

        countries = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_item,
            items
        );
        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        );
        countries.setAdapter(adapter);
        countries.setBackground(gradient(
            Color.rgb(255, 145, 0),
            Color.rgb(240, 25, 35),
            35
        ));

        root.addView(countries, new LinearLayout.LayoutParams(
            -1, dp(65)
        ));

        status = text("Status: Disconnected", 16, Color.WHITE);
        LinearLayout.LayoutParams statusParams =
            new LinearLayout.LayoutParams(-1, dp(55));
        statusParams.topMargin = dp(55);
        root.addView(status, statusParams);

        TextView ip = text("IP: Unknown\nLocation: Unknown", 17, Color.WHITE);
        root.addView(ip, new LinearLayout.LayoutParams(
            -1, dp(75)
        ));

        TextView footer = text(
            "ABDULRAZIK VPN  •  Secure Connection",
            12, Color.LTGRAY
        );
        root.addView(footer, new LinearLayout.LayoutParams(
            -1, dp(45)
        ));

        powerButton.setOnClickListener(v -> requestVpnPermission());

        menu.setOnClickListener(v ->
            Toast.makeText(this, "ABDULRAZIK VPN", Toast.LENGTH_SHORT).show()
        );

        setContentView(root);
    }

    private void requestVpnPermission() {
        Intent intent = VpnService.prepare(this);

        if (intent != null) {
            startActivityForResult(intent, VPN_REQUEST_CODE);
        } else {
            status.setText(
                "Permission granted; Xray connection is not configured"
            );
        }
    }

    @Override
    protected void onActivityResult(
        int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == VPN_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                status.setText(
                    "Permission granted; Xray connection is not configured"
                );
            } else {
                status.setText("VPN permission denied");
            }
        }
    }
}
