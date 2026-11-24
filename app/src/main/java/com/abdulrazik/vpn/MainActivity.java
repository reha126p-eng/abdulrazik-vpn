package com.abdulrazik.vpn;

import android.app.Activity;
import android.app.AlertDialog;
import android.net.VpnService;
import android.net.Uri;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.net.URLDecoder;
import java.util.ArrayList;

public class MainActivity extends Activity {

    private static final int VPN_REQUEST_CODE = 100;
    private TextView status;
    private TextView powerButton;
    private Spinner countries;
    private SharedPreferences prefs;
    private final ArrayList<String> serverUris = new ArrayList<>();
    private final ArrayList<String> serverNames = new ArrayList<>();

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

        prefs = getSharedPreferences("abdulrazik_servers", MODE_PRIVATE);
        loadServers();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(25), dp(20), dp(25), dp(20));
        root.setBackgroundColor(Color.rgb(15, 15, 15));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(root);

        TextView menu = text("☰", 35, Color.WHITE);
        menu.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        root.addView(menu, new LinearLayout.LayoutParams(-1, dp(55)));

        TextView title = text("ABDULRAZIK VPN", 28, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams =
            new LinearLayout.LayoutParams(-1, dp(100));
        titleParams.topMargin = dp(15);
        root.addView(title, titleParams);

        TextView traffic = text("↑  0 B                 ↓  0 B", 18, Color.WHITE);
        LinearLayout.LayoutParams trafficParams =
            new LinearLayout.LayoutParams(-1, dp(90));
        trafficParams.topMargin = dp(20);
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
        powerParams.topMargin = dp(15);
        powerParams.bottomMargin = dp(30);
        root.addView(powerButton, powerParams);

        TextView serverLabel = text("Select Server", 16, Color.WHITE);
        serverLabel.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        root.addView(serverLabel, new LinearLayout.LayoutParams(-1, dp(35)));

        countries = new Spinner(this);
        countries.setBackground(gradient(
            Color.rgb(255, 145, 0),
            Color.rgb(240, 25, 35),
            35
        ));
        root.addView(countries, new LinearLayout.LayoutParams(-1, dp(60)));
        refreshServerList();

        Button addServer = new Button(this);
        addServer.setText("+ Add VLESS Server");
        addServer.setTextColor(Color.WHITE);
        addServer.setBackground(gradient(
            Color.rgb(45, 45, 45),
            Color.rgb(75, 75, 75),
            25
        ));
        LinearLayout.LayoutParams addParams =
            new LinearLayout.LayoutParams(-1, dp(55));
        addParams.topMargin = dp(15);
        root.addView(addServer, addParams);

        status = text("Status: Disconnected", 16, Color.WHITE);
        LinearLayout.LayoutParams statusParams =
            new LinearLayout.LayoutParams(-1, dp(55));
        statusParams.topMargin = dp(20);
        root.addView(status, statusParams);

        TextView ip = text("IP: Unknown\nLocation: Unknown", 17, Color.WHITE);
        root.addView(ip, new LinearLayout.LayoutParams(-1, dp(75)));

        TextView footer = text(
            "ABDULRAZIK VPN  •  Secure Connection",
            12, Color.LTGRAY
        );
        root.addView(footer, new LinearLayout.LayoutParams(-1, dp(45)));

        addServer.setOnClickListener(v -> showAddServerDialog());

        powerButton.setOnClickListener(v -> requestVpnPermission());

        menu.setOnClickListener(v ->
            Toast.makeText(this, "ABDULRAZIK VPN", Toast.LENGTH_SHORT).show()
        );

        setContentView(scroll);
    }

    private void loadServers() {
        serverUris.clear();
        serverNames.clear();

        int count = prefs.getInt("count", 0);

        for (int i = 0; i < count; i++) {
            String uri = prefs.getString("server_" + i, null);
            if (uri != null && uri.startsWith("vless://")) {
                serverUris.add(uri);
                serverNames.add(getServerName(uri));
            }
        }
    }

    private String getServerName(String link) {
        try {
            Uri uri = Uri.parse(link);
            String name = uri.getFragment();

            if (name != null && !name.isEmpty()) {
                name = URLDecoder.decode(name, "UTF-8");
                return name;
            }

            return uri.getHost() == null ? "VLESS Server" : uri.getHost();
        } catch (Exception e) {
            return "VLESS Server";
        }
    }

    private void refreshServerList() {
        ArrayList<String> items = new ArrayList<>();

        if (serverNames.isEmpty()) {
            items.add("No servers added");
        } else {
            items.addAll(serverNames);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_item,
            items
        );
        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        );
        countries.setAdapter(adapter);

        int selected = prefs.getInt("selected", 0);
        if (selected >= 0 && selected < items.size()) {
            countries.setSelection(selected);
        }
    }

    private void showAddServerDialog() {
        EditText input = new EditText(this);
        input.setSingleLine(false);
        input.setMinLines(2);
        input.setTextColor(Color.BLACK);
        input.setHint("Paste your vless:// link here");
        input.setPadding(dp(12), dp(12), dp(12), dp(12));

        new AlertDialog.Builder(this)
            .setTitle("Add VLESS Server")
            .setMessage("Paste the complete VLESS link:")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", (dialog, which) -> {
                String link = input.getText().toString().trim();

                if (!link.startsWith("vless://")) {
                    Toast.makeText(this,
                        "Invalid VLESS link", Toast.LENGTH_LONG).show();
                    return;
                }

                Uri parsed = Uri.parse(link);
                if (parsed.getHost() == null || parsed.getUserInfo() == null) {
                    Toast.makeText(this,
                        "The VLESS link is incomplete", Toast.LENGTH_LONG).show();
                    return;
                }

                for (String saved : serverUris) {
                    if (saved.equals(link)) {
                        Toast.makeText(this,
                            "This server is already saved", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                serverUris.add(link);
                serverNames.add(getServerName(link));

                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("count", serverUris.size());

                for (int i = 0; i < serverUris.size(); i++) {
                    editor.putString("server_" + i, serverUris.get(i));
                }

                editor.putInt("selected", serverUris.size() - 1);
                editor.apply();

                refreshServerList();

                Toast.makeText(this,
                    "Server saved on this phone", Toast.LENGTH_LONG).show();
            })
            .show();
    }

    private void requestVpnPermission() {
        if (serverUris.isEmpty()) {
            status.setText("Please add a VLESS server first");
            Toast.makeText(this,
                "Add a server before connecting", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = VpnService.prepare(this);

        if (intent != null) {
            startActivityForResult(intent, VPN_REQUEST_CODE);
        } else {
            status.setText("Permission granted; Xray is not connected yet");
        }
    }

    @Override
    protected void onActivityResult(
        int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == VPN_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                status.setText("Permission granted; Xray is not connected yet");
            } else {
                status.setText("VPN permission denied");
            }
        }
    }
}
