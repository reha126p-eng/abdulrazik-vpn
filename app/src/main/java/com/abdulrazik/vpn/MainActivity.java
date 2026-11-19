package com.abdulrazik.vpn;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
import android.graphics.Typeface;

public class MainActivity extends Activity {

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
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        countries.setAdapter(adapter);
        layout.addView(countries);

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

        connect.setOnClickListener(v ->
            status.setText("Status: VPN engine not installed yet")
        );

        setContentView(layout);
    }
}
