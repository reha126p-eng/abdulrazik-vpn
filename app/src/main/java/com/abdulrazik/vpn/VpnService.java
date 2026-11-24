package com.abdulrazik.vpn;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.system.OsConstants;
import android.util.Log;

import libv2ray.CoreCallbackHandler;
import libv2ray.CoreController;
import libv2ray.Libv2ray;

public class VpnService extends android.net.VpnService {

    public static final String EXTRA_SERVER_URI = "server_uri";
    public static final String ACTION_STOP = "abdulrazik.vpn.STOP";

    private static final String TAG = "ABDULRAZIK";
    private static final String CHANNEL_ID = "abdulrazik_vpn";

    private ParcelFileDescriptor vpnInterface;
    private CoreController coreController;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopVpn();
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        createNotification();
        startForeground(1, buildNotification());

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
            String physicalInterface =
                    NetworkHelper.getPhysicalInterface(this);

            if (physicalInterface == null) {
                throw new Exception("Physical network not found");
            }

            String version = XrayEngine.initialize(this);
            Log.i(TAG, "Xray: " + version);

            String config =
                    XrayEngine.buildConfig(serverUri, physicalInterface);

            Builder builder = new Builder();
            builder.setSession("ABDULRAZIK VPN");
            builder.addAddress("10.0.0.2", 30);
            builder.addRoute("0.0.0.0", 0);
            builder.addDnsServer("1.1.1.1");
            builder.allowFamily(OsConstants.AF_INET);
            builder.setMtu(1500);

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

            coreController.startLoop(config, vpnInterface.getFd());

            if (!coreController.getIsRunning()) {
                throw new Exception("Xray did not start");
            }

            Log.i(TAG, "ABDULRAZIK VPN connected");

        } catch (Exception e) {
            Log.e(TAG, "VPN startup failed", e);
            stopVpn();
            stopForeground(true);
            stopSelf();
        }
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
