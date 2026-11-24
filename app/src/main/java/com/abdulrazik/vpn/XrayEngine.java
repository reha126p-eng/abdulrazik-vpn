package com.abdulrazik.vpn;

import android.content.Context;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import go.Seq;
import libv2ray.Libv2ray;

public final class XrayEngine {

    private static boolean initialized = false;

    private static void copyAsset(Context context, File directory, String name)
            throws Exception {
        File target = new File(directory, name);

        if (target.exists() && target.length() > 0) return;

        try (InputStream input = context.getAssets().open(name);
             FileOutputStream output = new FileOutputStream(target)) {

            byte[] buffer = new byte[8192];
            int count;

            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
        }
    }

    public static synchronized String initialize(Context context) {
        try {
            Context app = context.getApplicationContext();

            if (!initialized) {
                Seq.setContext(app);

                File assetDir = app.getDir("assets", Context.MODE_PRIVATE);

                copyAsset(app, assetDir, "geoip.dat");
                copyAsset(app, assetDir, "geosite.dat");

                Libv2ray.initCoreEnv(
                    assetDir.getAbsolutePath(),
                    ""
                );

                initialized = true;
            }

            return Libv2ray.checkVersionX();

        } catch (Throwable e) {
            initialized = false;
            throw new RuntimeException("Xray initialization failed", e);
        }
    }

    public static String buildConfig(
            String vlessLink, String physicalInterface) throws Exception {

        Uri uri = Uri.parse(vlessLink);

        if (!"vless".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Only VLESS links are supported");
        }

        String uuid = uri.getUserInfo();
        String host = uri.getHost();
        int port = uri.getPort();

        if (uuid == null || uuid.isEmpty() ||
            host == null || port < 1) {
            throw new IllegalArgumentException("Invalid VLESS server address");
        }

        String security = uri.getQueryParameter("security");
        String network = uri.getQueryParameter("type");

        if (security == null || !security.equalsIgnoreCase("reality")) {
            throw new IllegalArgumentException("This version requires REALITY");
        }

        if (network != null && !network.equalsIgnoreCase("tcp")) {
            throw new IllegalArgumentException("Only TCP transport is supported");
        }

        String serverName = uri.getQueryParameter("sni");
        String publicKey = uri.getQueryParameter("pbk");
        String shortId = uri.getQueryParameter("sid");
        String fingerprint = uri.getQueryParameter("fp");
        String flow = uri.getQueryParameter("flow");
        String spiderX = uri.getQueryParameter("spx");

        if (serverName == null || serverName.isEmpty() ||
            publicKey == null || publicKey.isEmpty()) {
            throw new IllegalArgumentException("REALITY settings are incomplete");
        }

        if (physicalInterface == null || physicalInterface.isEmpty()) {
            throw new IllegalArgumentException("Physical network interface not found");
        }

        if (fingerprint == null || fingerprint.isEmpty()) {
            fingerprint = "chrome";
        }

        JSONObject root = new JSONObject();

        JSONObject log = new JSONObject();
        log.put("loglevel", "warning");
        root.put("log", log);

        JSONArray inbounds = new JSONArray();
        JSONObject tun = new JSONObject();
        tun.put("tag", "tun-in");
        tun.put("port", 0);
        tun.put("protocol", "tun");

        JSONObject tunSettings = new JSONObject();
        tunSettings.put("name", "xray0");
        tunSettings.put("mtu", 1500);
        tunSettings.put("autoOutboundsInterface", physicalInterface);
        tun.put("settings", tunSettings);

        inbounds.put(tun);
        root.put("inbounds", inbounds);

        JSONObject user = new JSONObject();
        user.put("id", uuid);
        user.put("encryption", "none");

        if (flow != null && !flow.isEmpty()) {
            user.put("flow", flow);
        }

        JSONArray users = new JSONArray();
        users.put(user);

        JSONObject server = new JSONObject();
        server.put("address", host);
        server.put("port", port);
        server.put("users", users);

        JSONArray servers = new JSONArray();
        servers.put(server);

        JSONObject vlessSettings = new JSONObject();
        vlessSettings.put("vnext", servers);

        JSONObject reality = new JSONObject();
        reality.put("serverName", serverName);
        reality.put("fingerprint", fingerprint);
        reality.put("password", publicKey);
        reality.put("shortId", shortId == null ? "" : shortId);
        reality.put("spiderX", spiderX == null ? "/" : spiderX);

        JSONObject stream = new JSONObject();
        stream.put("network", "tcp");
        stream.put("security", "reality");
        stream.put("realitySettings", reality);

        JSONObject proxy = new JSONObject();
        proxy.put("tag", "proxy");
        proxy.put("protocol", "vless");
        proxy.put("settings", vlessSettings);
        proxy.put("streamSettings", stream);

        JSONObject direct = new JSONObject();
        direct.put("tag", "direct");
        direct.put("protocol", "freedom");
        direct.put("settings", new JSONObject());

        JSONObject block = new JSONObject();
        block.put("tag", "block");
        block.put("protocol", "blackhole");
        block.put("settings", new JSONObject());

        JSONArray outbounds = new JSONArray();
        outbounds.put(proxy);
        outbounds.put(direct);
        outbounds.put(block);

        root.put("outbounds", outbounds);

        return root.toString();
    }
}
