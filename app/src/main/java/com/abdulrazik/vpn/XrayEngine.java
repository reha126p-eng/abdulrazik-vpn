package com.abdulrazik.vpn;

import android.content.Context;
import android.provider.Settings;
import android.util.Base64;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import go.Seq;
import libv2ray.Libv2ray;

public final class XrayEngine {

    private static boolean initialized = false;

    private static void copyAsset(Context context, File directory, String name)
            throws Exception {
        File target = new File(directory, name);

        if (target.exists() && target.length() > 0) {
            return;
        }

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
                copyAsset(app, assetDir, "geoip-only-cn-private.dat");

                String androidId = Settings.Secure.getString(
                    app.getContentResolver(),
                    Settings.Secure.ANDROID_ID
                );

                if (androidId == null) androidId = "";

                byte[] keyBytes = Arrays.copyOf(
                    androidId.getBytes(StandardCharsets.UTF_8), 32
                );

                String deviceKey = Base64.encodeToString(
                    keyBytes, Base64.URL_SAFE | Base64.NO_PADDING
                );

                Libv2ray.initCoreEnv(
                    assetDir.getAbsolutePath(),
                    deviceKey
                );

                initialized = true;
            }

            return Libv2ray.checkVersionX();

        } catch (Throwable e) {
            initialized = false;
            throw new RuntimeException("Xray initialization failed", e);
        }
    }
}
