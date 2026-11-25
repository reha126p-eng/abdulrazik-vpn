package com.abdulrazik.vpn;

import android.app.Activity;
import android.app.AlertDialog;
import android.net.VpnService;
import android.net.Uri;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Build;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URLDecoder;
import java.util.ArrayList;

public class MainActivity extends Activity {

    private static final int VPN_REQUEST_CODE = 100;
    private TextView status;
    private TextView powerButton;
    private boolean vpnConnected = false;
    private boolean vpnConnecting = false;

    private final BroadcastReceiver vpnStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!com.abdulrazik.vpn.VpnService.ACTION_STATUS.equals(intent.getAction())) return;

            vpnConnected = intent.getBooleanExtra(
                com.abdulrazik.vpn.VpnService.EXTRA_CONNECTED, false
            );

            String message = intent.getStringExtra(
                com.abdulrazik.vpn.VpnService.EXTRA_MESSAGE
            );

            vpnConnecting = message != null && message.contains("در حال اتصال");
            status.setText(message == null ? "وضعیت نامشخص" : message);
            updatePowerButton();
        }
    };

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
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(25), dp(20), dp(25), dp(20));
        root.setBackgroundColor(Color.rgb(242, 247, 255));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(root);

        TextView menu = text("☰", 30, Color.rgb(35, 75, 130));
        menu.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        root.addView(menu, new LinearLayout.LayoutParams(-1, dp(55)));

        TextView title = text("ABDULRAZIK VPN", 28, Color.rgb(20, 55, 105));
        title.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams =
            new LinearLayout.LayoutParams(-1, dp(100));
        titleParams.topMargin = dp(15);
        root.addView(title, titleParams);

        TextView traffic = text("اتصال سریع و امن", 18, Color.rgb(90, 115, 150));
        LinearLayout.LayoutParams trafficParams =
            new LinearLayout.LayoutParams(-1, dp(90));
        trafficParams.topMargin = dp(20);
        root.addView(traffic, trafficParams);

        powerButton = text("⏻\nاتصال", 27, Color.WHITE);
        powerButton.setTypeface(null, Typeface.BOLD);
        powerButton.setBackground(gradient(
            Color.rgb(30, 125, 245),
            Color.rgb(20, 75, 190),
            200
        ));
        LinearLayout.LayoutParams powerParams =
            new LinearLayout.LayoutParams(dp(190), dp(190));
        powerParams.gravity = Gravity.CENTER;
        powerParams.topMargin = dp(15);
        powerParams.bottomMargin = dp(30);
        root.addView(powerButton, powerParams);

        TextView serverLabel = text("انتخاب موقعیت سرور", 16, Color.rgb(35, 65, 105));
        serverLabel.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        root.addView(serverLabel, new LinearLayout.LayoutParams(-1, dp(35)));

        countries = new Spinner(this);
        countries.setBackground(gradient(
            Color.rgb(30, 125, 245),
            Color.rgb(20, 75, 190),
            35
        ));
        root.addView(countries, new LinearLayout.LayoutParams(-1, dp(60)));
        refreshServerList();

        Button addServer = new Button(this);
        addServer.setText("+ افزودن سرور VLESS");
        addServer.setTextColor(Color.rgb(35, 75, 130));
        addServer.setBackground(gradient(
            Color.rgb(220, 235, 255),
            Color.rgb(195, 220, 250),
            25
        ));
        LinearLayout.LayoutParams addParams =
            new LinearLayout.LayoutParams(-1, dp(55));
        addParams.topMargin = dp(15);
        root.addView(addServer, addParams);

        status = text("وضعیت: قطع است", 16, Color.rgb(35, 75, 130));
        LinearLayout.LayoutParams statusParams =
            new LinearLayout.LayoutParams(-1, dp(55));
        statusParams.topMargin = dp(20);
        root.addView(status, statusParams);

        TextView ip = text("آی‌پی: نامشخص\nموقعیت: نامشخص", 17, Color.rgb(65, 90, 125));
        root.addView(ip, new LinearLayout.LayoutParams(-1, dp(75)));

        Button diagnosticButton = new Button(this);
        diagnosticButton.setText("گزارش عیب‌یابی");
        root.addView(diagnosticButton, new LinearLayout.LayoutParams(-1, dp(50)));
        diagnosticButton.setOnClickListener(v -> showDiagnostics());

        TextView footer = text(
            "ABDULRAZIK VPN  •  اتصال امن",
            12, Color.rgb(110, 130, 160)
        );
        root.addView(footer, new LinearLayout.LayoutParams(-1, dp(45)));

        addServer.setOnClickListener(v -> showAddServerDialog());

        powerButton.setOnClickListener(v -> toggleVpn());

        menu.setOnClickListener(v ->
            Toast.makeText(this, "ABDULRAZIK VPN", Toast.LENGTH_SHORT).show()
        );

        setContentView(scroll);
    }


    private void showDiagnostics() {
        File file = new File(getExternalFilesDir(null), "vpn-diagnostic.txt");
        StringBuilder report = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                report.append(line).append("\n");
            }
        } catch (Exception e) {
            report.append("گزارش هنوز ساخته نشده یا قابل خواندن نیست.\n");
            report.append(e.getMessage());
        }

        new AlertDialog.Builder(this)
            .setTitle("گزارش عیب‌یابی VPN")
            .setMessage(report.toString())
            .setPositiveButton("بستن", null)
            .setNeutralButton("کپی گزارش", (dialog, which) -> {
                android.content.ClipboardManager clipboard =
                    (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(
                    android.content.ClipData.newPlainText("VPN diagnostics", report.toString())
                );
                Toast.makeText(this, "گزارش کپی شد", Toast.LENGTH_SHORT).show();
            })
            .show();
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

            return uri.getHost() == null ? "سرور VLESS" : uri.getHost();
        } catch (Exception e) {
            return "سرور VLESS";
        }
    }

    private void refreshServerList() {
        ArrayList<String> items = new ArrayList<>();

        if (serverNames.isEmpty()) {
            items.add("هنوز سروری اضافه نشده");
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

        countries.setOnItemSelectedListener(
            new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(
                        AdapterView<?> parent, View view,
                        int position, long id) {
                    if (position < serverUris.size()) {
                        prefs.edit().putInt("selected", position).apply();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            }
        );

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
        input.setHint("لینک VLESS را اینجا وارد کنید");
        input.setPadding(dp(12), dp(12), dp(12), dp(12));

        new AlertDialog.Builder(this)
            .setTitle("افزودن سرور VLESS")
            .setMessage("لینک کامل سرور VLESS را وارد کنید:")
            .setView(input)
            .setNegativeButton("لغو", null)
            .setPositiveButton("ذخیره", (dialog, which) -> {
                String link = input.getText().toString().trim();

                if (!link.startsWith("vless://")) {
                    Toast.makeText(this,
                        "لینک VLESS نامعتبر است", Toast.LENGTH_LONG).show();
                    return;
                }

                Uri parsed = Uri.parse(link);
                if (parsed.getHost() == null || parsed.getUserInfo() == null) {
                    Toast.makeText(this,
                        "لینک سرور ناقص است", Toast.LENGTH_LONG).show();
                    return;
                }

                for (String saved : serverUris) {
                    if (saved.equals(link)) {
                        Toast.makeText(this,
                            "این سرور قبلاً ذخیره شده است", Toast.LENGTH_SHORT).show();
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
                    "سرور در گوشی ذخیره شد", Toast.LENGTH_LONG).show();
            })
            .show();
    }


    private void updatePowerButton() {
        if (vpnConnected) {
            powerButton.setText("⏻\\nقطع اتصال");
            powerButton.setBackground(gradient(
                Color.rgb(220, 65, 75),
                Color.rgb(165, 35, 55),
                200
            ));
        } else if (vpnConnecting) {
            powerButton.setText("⏳\\nدر حال اتصال");
        } else {
            powerButton.setText("⏻\\nاتصال");
            powerButton.setBackground(gradient(
                Color.rgb(30, 125, 245),
                Color.rgb(20, 75, 190),
                200
            ));
        }
    }

    private void toggleVpn() {
        if (vpnConnected || vpnConnecting) {
            Intent stopIntent = new Intent(this, com.abdulrazik.vpn.VpnService.class);
            stopIntent.setAction(com.abdulrazik.vpn.VpnService.ACTION_STOP);
            startService(stopIntent);
            vpnConnected = false;
            vpnConnecting = false;
            status.setText("وضعیت: در حال قطع اتصال...");
            updatePowerButton();
        } else {
            requestVpnPermission();
        }
    }

    private void requestVpnPermission() {
        if (serverUris.isEmpty()) {
            status.setText("ابتدا یک سرور VLESS اضافه کنید");
            Toast.makeText(this, "قبل از اتصال، یک سرور اضافه کنید", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent permissionIntent = android.net.VpnService.prepare(this);

        if (permissionIntent != null) {
            startActivityForResult(permissionIntent, VPN_REQUEST_CODE);
        } else {
            startVpnService();
        }
    }

    private void startVpnService() {
        int selected = countries.getSelectedItemPosition();

        if (selected < 0 || selected >= serverUris.size()) {
            status.setText("لطفاً یک سرور معتبر انتخاب کنید");
            return;
        }

        Intent serviceIntent = new Intent(this, com.abdulrazik.vpn.VpnService.class);
        serviceIntent.putExtra(
            com.abdulrazik.vpn.VpnService.EXTRA_SERVER_URI,
            serverUris.get(selected)
        );

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }

            status.setText("وضعیت: در حال اتصال...");
            powerButton.setText("⏻\\nدر حال اتصال");
        } catch (Exception e) {
            status.setText("اجرای سرویس VPN ممکن نشد");
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter(
            com.abdulrazik.vpn.VpnService.ACTION_STATUS
        );

        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(vpnStatusReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(vpnStatusReceiver, filter);
        }
    }

    @Override
    protected void onPause() {
        try {
            unregisterReceiver(vpnStatusReceiver);
        } catch (Exception ignored) {
        }
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == VPN_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                startVpnService();
            } else {
                status.setText("اجازه اتصال VPN داده نشد");
            }
        }
    }
}
