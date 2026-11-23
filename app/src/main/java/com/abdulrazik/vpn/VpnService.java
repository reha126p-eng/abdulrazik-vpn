package com.abdulrazik.vpn;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

public class VpnService extends android.net.VpnService {

    private ParcelFileDescriptor vpnInterface;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            Builder builder = new Builder();
            builder.setSession("ABDULRAZIK VPN");
            builder.addAddress("10.0.0.2", 30);
            builder.addRoute("0.0.0.0", 0);
            builder.addDnsServer("1.1.1.1");
            builder.setMtu(1500);

            vpnInterface = builder.establish();

            if (vpnInterface == null) {
                Log.e("ABDULRAZIK", "VPN interface could not be established");
                stopSelf();
                return START_NOT_STICKY;
            }

            Log.i("ABDULRAZIK", "VPN interface established");

        } catch (Exception e) {
            Log.e("ABDULRAZIK", "VPN startup failed", e);
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        try {
            if (vpnInterface != null) {
                vpnInterface.close();
                vpnInterface = null;
            }
        } catch (Exception e) {
            Log.e("ABDULRAZIK", "VPN close failed", e);
        }

        super.onDestroy();
    }
}
