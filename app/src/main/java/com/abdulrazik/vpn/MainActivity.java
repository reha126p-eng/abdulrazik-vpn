package com.abdulrazik.vpn;

import android.app.Activity;
import android.net.VpnService;
import android.content.Intent;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
import android.graphics.Typeface;

public class MainActivity extends Activity {

    private static final int VPN_REQUEST_CODE = 100;
    TextView status;
    Spinner countries;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(30, 30, 30, 30);
        layout.setBackgroundColor(Color.rgb(12, 20, 38));

        TextView title = new TextView(this);
        title.setText("ABDULRAZIK VPN");
        title.setTextSize(28);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Secure Connection");
        subtitle.setTextColor(Color.LTGRAY);
        subtitle.setTextSize(15);
        layout.addView(subtitle);

        TextView countryLabel = new TextView(this);
        countryLabel.setText("Select Country");
        countryLabel.setTextColor(Color.WHITE);
        countryLabel.setTextSize(18);
        countryLabel.setPadding(0, 40, 0, 10);
        layout.addView(countryLabel);

        String[] items = {"Germany", "Turkey", "USA", "United Kingdom"};
        countries = new Spinner(this);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, items);

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item);

        countries.setAdapter(adapter);
        layout.addView(countries);

        Button test = new Button(this);
        test.setText("TEST XRAY ENGINE");
        layout.addView(test);

        Button connect = new Button(this);
        connect.setText("CONNECT");
        connect.setTextSize(18);
        layout.addView(connect);

        status = new TextView(this);
        status.setText("Status: Disconnected");
        status.setTextColor(Color.WHITE);
        status.setTextSize(16);
        status.setPadding(0, 25, 0, 0);
        layout.addView(status);

        test.setOnClickListener(v -> testXray());

        connect.setOnClickListener(v -> requestVpnPermission());

        setContentView(layout);
    }

    private void testXray() {
        status.setText("Status: Testing Xray...");

        new Thread(() -> {
            try {
                String version = XrayEngine.initialize(
                    getApplicationContext()
                );

                runOnUiThread(() ->
                    status.setText("Xray Ready: " + version)
                );

            } catch (Throwable e) {
                runOnUiThread(() ->
                    status.setText("Xray Error: " + e.getClass().getSimpleName())
                );
            }
        }).start();
    }

    private void requestVpnPermission() {
        Intent intent = VpnService.prepare(this);

        if (intent != null) {
            startActivityForResult(intent, VPN_REQUEST_CODE);
        } else {
            status.setText("Permission granted; server not connected yet");
        }
    }

    @Override
    protected void onActivityResult(
        int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == VPN_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                status.setText(
                    "VPN permission granted; engine not connected yet"
                );
            } else {
                status.setText("VPN permission denied");
            }
        }
    }
}
