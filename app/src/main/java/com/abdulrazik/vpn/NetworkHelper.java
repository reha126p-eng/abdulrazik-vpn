package com.abdulrazik.vpn;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;

public final class NetworkHelper {

    public static String getPhysicalInterface(Context context) {
        ConnectivityManager cm =
            (ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE
            );

        if (cm == null) return null;

        Network network = cm.getActiveNetwork();
        if (network == null) return null;

        NetworkCapabilities caps =
            cm.getNetworkCapabilities(network);

        if (caps == null ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
            return null;
        }

        LinkProperties properties = cm.getLinkProperties(network);
        if (properties == null) return null;

        String name = properties.getInterfaceName();

        return (name == null || name.isEmpty()) ? null : name;
    }

    private NetworkHelper() {}
}
