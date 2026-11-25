package com.abdulrazik.vpn;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.system.OsConstants;
import android.util.Log;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import libv2ray.CoreCallbackHandler;
import libv2ray.CoreController;
import libv2ray.Libv2ray;

public class VpnService extends android.net.VpnService {

    public static final String EXTRA_SERVER_URI = "server_uri";
    public static final String ACTION_STOP = "abdulrazik.vpn.STOP";
    public static final String ACTION_STATUS = "abdulrazik.vpn.STATUS";
    public static final String EXTRA_CONNECTED = "connected";
    public static final String EXTRA_MESSAGE = "message";

    private static final String TAG = "ABDULRAZIK";
    private static final String CHANNEL_ID = "abdulrazik_vpn";

    private ParcelFileDescriptor vpnInterface;
    private CoreController coreController;
    private volatile boolean stopRequested = false;
    private synchronized void diagnostic(String message) {
        try {
            File dir = getExternalFilesDir(null);
            if (dir == null) return;
            File file = new File(dir, "vpn-diagnostic.txt");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                String time = new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss", Locale.US
                ).format(new Date());
                writer.write(time + " | " + message);
                writer.newLine();
            }
        } catch (Exception e) {
            Log.e(TAG, "Diagnostic write failed", e);
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopRequested = true;
            stopVpn();
            sendStatus(false, "وضعیت: قطع است");
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        stopRequested = false;
        createNotification();
        startForeground(1, buildNotification());
        sendStatus(false, "وضعیت: در حال اتصال...");

        String serverUri = intent == null
                ? null
                : intent.getStringExtra(EXTRA_SERVER_URI);

        if (serverUri == null || serverUri.isEmpty()) {
            Log.e(TAG, "Server URI is missing");
            stopSelf();
            return START_NOT_STICKY;
        }

        new Thread(() -> startVpn(serverUri), "ABDULRAZIK-Xray").start();

        return START_NOT_STICKY;
    }

    private void startVpn(String serverUri) {
        try {
            if (stopRequested) return;
            diagnostic("VPN startup requested");
            String physicalInterface =
                    NetworkHelper.getPhysicalInterface(this);

            if (stopRequested) return;

            if (physicalInterface == null) {
                throw new Exception("Physical network not found");
            }

            diagnostic("Physical interface: " + physicalInterface);
            String version = XrayEngine.initialize(this);
            diagnostic("Xray initialized: " + version);
            Log.i(TAG, "Xray: " + version);

            String config =
                    XrayEngine.buildConfig(serverUri, physicalInterface);

            if (stopRequested) return;

            Builder builder = new Builder();
            builder.setSession("ABDULRAZIK VPN");
            builder.addAddress("10.0.0.2", 30);
            builder.addRoute("0.0.0.0", 0);
            builder.addDnsServer("1.1.1.1");
            builder.allowFamily(OsConstants.AF_INET);
            builder.setMtu(1500);

            synchronized (this) {
                if (stopRequested) return;

                vpnInterface = builder.establish();

                if (vpnInterface == null) {
                    throw new Exception("Android VPN interface failed");
                }

                CoreCallbackHandler callback = new CoreCallbackHandler() {
                    @Override
                    public long startup() {
                        Log.i(TAG, "Xray started");
                        return 0;
                    }

                    @Override
                    public long shutdown() {
                        Log.i(TAG, "Xray stopped");
                        return 0;
                    }

                    @Override
                    public long onEmitStatus(long level, String message) {
                        Log.i(TAG, message);
                        return 0;
                    }
                };

                coreController = Libv2ray.newCoreController(callback);
                diagnostic("Starting Xray core");
                coreController.startLoop(config, vpnInterface.getFd());
                diagnostic("Xray core returned from startLoop");

                if (!coreController.getIsRunning()) {
                    throw new Exception("Xray did not start");
                }
            }

            if (stopRequested) {
                stopVpn();
                return;
            }

            diagnostic("VPN service reports connected");
            Log.i(TAG, "ABDULRAZIK VPN connected");
            sendStatus(true, "وضعیت: متصل است");

        } catch (Exception e) {
            Log.e(TAG, "VPN startup failed", e);
            diagnostic("VPN startup failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            stopVpn();

            if (stopRequested) return;

            sendStatus(false, "اتصال ناموفق بود");
            stopForeground(true);
            stopSelf();
        }
    }

    private void sendStatus(boolean connected, String message) {
        Intent statusIntent = new Intent(ACTION_STATUS);
        statusIntent.setPackage(getPackageName());
        statusIntent.putExtra(EXTRA_CONNECTED, connected);
        statusIntent.putExtra(EXTRA_MESSAGE, message);
        sendBroadcast(statusIntent);
    }

    private void createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "ABDULRAZIK VPN",
                    NotificationManager.IMPORTANCE_LOW
            );

            NotificationManager manager =
                    getSystemService(NotificationManager.class);

            manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("ABDULRAZIK VPN")
                    .setContentText("VPN service is running")
                    .setSmallIcon(android.R.drawable.stat_sys_warning)
                    .build();
        }

        return new Notification.Builder(this)
                .setContentTitle("ABDULRAZIK VPN")
                .setContentText("VPN service is running")
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .build();
    }

    private synchronized void stopVpn() {
        try {
            if (coreController != null && coreController.getIsRunning()) {
                coreController.stopLoop();
            }
        } catch (Exception e) {
            Log.e(TAG, "Xray stop failed", e);
        }

        coreController = null;

        try {
            if (vpnInterface != null) {
                vpnInterface.close();
                vpnInterface = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "VPN interface close failed", e);
        }
    }

    @Override
    public void onRevoke() {
        stopVpn();
        stopSelf();
        super.onRevoke();
    }

    @Override
    public void onDestroy() {
        stopVpn();
        super.onDestroy();
    }
}
