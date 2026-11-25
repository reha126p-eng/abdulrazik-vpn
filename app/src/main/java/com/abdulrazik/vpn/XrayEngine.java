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

        if (uuid == null || uuid.isEmpty() || host == null || port < 1) {
            throw new IllegalArgumentException("Invalid VLESS server address");
        }

        String security = uri.getQueryParameter("security");
        String network = uri.getQueryParameter("type");
        String serverName = uri.getQueryParameter("sni");
        String fingerprint = uri.getQueryParameter("fp");
        String flow = uri.getQueryParameter("flow");

        if (physicalInterface == null || physicalInterface.isEmpty()) {
            throw new IllegalArgumentException("Physical network interface not found");
        }

        if (fingerprint == null || fingerprint.isEmpty()) {
            fingerprint = "chrome";
        }

        boolean realityMode = "reality".equalsIgnoreCase(security);
        boolean tlsMode = "tls".equalsIgnoreCase(security);

        if (!realityMode && !tlsMode) {
            throw new IllegalArgumentException("Only REALITY and TLS security are supported");
        }

        if (realityMode && network != null && !network.equalsIgnoreCase("tcp")) {
            throw new IllegalArgumentException("REALITY currently requires TCP transport");
        }

        if (tlsMode && (network == null || !network.equalsIgnoreCase("ws"))) {
            throw new IllegalArgumentException("TLS currently requires WebSocket transport");
        }

        if (serverName == null || serverName.isEmpty()) {
            serverName = host;
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

        JSONObject stream = new JSONObject();

        if (realityMode) {
            String publicKey = uri.getQueryParameter("pbk");
            String shortId = uri.getQueryParameter("sid");
            String spiderX = uri.getQueryParameter("spx");

            if (publicKey == null || publicKey.isEmpty()) {
                throw new IllegalArgumentException("REALITY public key is missing");
            }

            stream.put("network", "tcp");
            stream.put("security", "reality");

            JSONObject reality = new JSONObject();
            reality.put("serverName", serverName);
            reality.put("fingerprint", fingerprint);
            reality.put("password", publicKey);
            reality.put("shortId", shortId == null ? "" : shortId);
            reality.put("spiderX", spiderX == null ? "/" : spiderX);

            stream.put("realitySettings", reality);
        } else {
            stream.put("network", "ws");
            stream.put("security", "tls");

            JSONObject tls = new JSONObject();
            tls.put("serverName", serverName);
            tls.put("fingerprint", fingerprint);

            String alpn = uri.getQueryParameter("alpn");
            if (alpn != null && !alpn.isEmpty()) {
                JSONArray protocols = new JSONArray();
                for (String item : alpn.split(",")) {
                    if (!item.trim().isEmpty()) {
                        protocols.put(item.trim());
                    }
                }
                if (protocols.length() > 0) {
                    tls.put("alpn", protocols);
                }
            }

            stream.put("tlsSettings", tls);

            JSONObject ws = new JSONObject();
            String path = uri.getQueryParameter("path");
            ws.put("path", path == null || path.isEmpty() ? "/" : path);

            JSONObject headers = new JSONObject();
            String wsHost = uri.getQueryParameter("host");
            headers.put("Host", wsHost == null || wsHost.isEmpty() ? host : wsHost);
            ws.put("headers", headers);

            stream.put("wsSettings", ws);
        }

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
