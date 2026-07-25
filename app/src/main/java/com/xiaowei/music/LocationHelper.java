package com.xiaowei.music;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import java.security.cert.X509Certificate;

public class LocationHelper {

    private static final String TAG = "LocationHelper";

    public static final String PREF_LATITUDE = "saved_lat";
    public static final String PREF_LONGITUDE = "saved_lon";
    public static final String PREF_COUNTRY = "saved_country";
    public static final String PREF_PROVINCE = "saved_province";
    public static final String PREF_CITY = "saved_city";
    public static final String PREF_DISTRICT = "saved_district";
    public static final String PREF_TOWN = "saved_town";
    public static final String PREF_LOCATION_TIMESTAMP = "saved_location_ts";

    private static final long LOCATION_TIMEOUT_MS = 15_000L;

    private static final String REVERSE_GEOCODE_URL =
            "https://api.mirror-earth.com/nominatim/reverse";

    private static final ExecutorService IO = Executors.newFixedThreadPool(4);

    public static class LocationInfo {
        public double latitude;
        public double longitude;
        public String country = "中国";
        public String province = "北京";
        public String city = "北京";
        public String district = "";
        public String town = "";
        public long timestamp = 0L;

        public String displayLocationName() {
            if (!district.isEmpty()) return district;
            return city;
        }
    }

    public interface Callback {
        void onLocationSuccess(LocationInfo info);
        void onLocationFailed(String reason);
    }

    private LocationHelper() {}

    private static void trustAllHostsForOldAndroid(HttpURLConnection connection) {
        if (connection instanceof HttpsURLConnection) {
            try {
                TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            public X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[]{};
                            }
                            public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                            public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                        }
                };
                SSLContext sc = SSLContext.getInstance("TLS");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                ((HttpsURLConnection) connection).setSSLSocketFactory(sc.getSocketFactory());
                ((HttpsURLConnection) connection).setHostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "trustAllHostsForOldAndroid error", e);
            }
        }
    }

    public static boolean hasPermission(Context context) {
        int fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
        int coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION);
        return fine == PackageManager.PERMISSION_GRANTED
                || coarse == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isLocationEnabled(Context context) {
        try {
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (lm == null) return false;
            boolean gps = false, network = false;
            try { gps = lm.isProviderEnabled(LocationManager.GPS_PROVIDER); } catch (Exception ignored) {}
            try { network = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER); } catch (Exception ignored) {}
            return gps || network;
        } catch (Exception e) {
            Log.e(TAG, "isLocationEnabled error", e);
            return false;
        }
    }

    public static void saveToPrefs(SharedPreferences prefs, LocationInfo info) {
        if (prefs == null || info == null) return;
        SharedPreferences.Editor ed = prefs.edit();
        ed.putFloat(PREF_LATITUDE, (float) info.latitude);
        ed.putFloat(PREF_LONGITUDE, (float) info.longitude);
        ed.putString(PREF_COUNTRY, info.country);
        ed.putString(PREF_PROVINCE, info.province);
        ed.putString(PREF_CITY, info.city);
        ed.putString(PREF_DISTRICT, info.district);
        ed.putString(PREF_TOWN, info.town);
        ed.putLong(PREF_LOCATION_TIMESTAMP, info.timestamp);
        ed.apply();
    }

    public static LocationInfo loadFromPrefs(SharedPreferences prefs) {
        if (prefs == null) return null;
        if (!prefs.contains(PREF_LATITUDE) || !prefs.contains(PREF_LONGITUDE)) return null;
        LocationInfo info = new LocationInfo();
        info.latitude = prefs.getFloat(PREF_LATITUDE, 0f);
        info.longitude = prefs.getFloat(PREF_LONGITUDE, 0f);
        info.country = prefs.getString(PREF_COUNTRY, "中国");
        info.province = prefs.getString(PREF_PROVINCE, "北京");
        info.city = prefs.getString(PREF_CITY, "北京");
        info.district = prefs.getString(PREF_DISTRICT, "");
        info.town = prefs.getString(PREF_TOWN, "");
        info.timestamp = prefs.getLong(PREF_LOCATION_TIMESTAMP, 0L);
        return info;
    }

    public static void requestSingleLocation(Context context, Callback cb) {
        final Callback callback = cb != null ? cb : new Callback() {
            @Override public void onLocationSuccess(LocationInfo i) {}
            @Override public void onLocationFailed(String r) {}
        };
        Log.d(TAG, "[LOC] requestSingleLocation called");
        if (!hasPermission(context)) {
            Log.w(TAG, "[LOC] requestSingleLocation: no_permission");
            callback.onLocationFailed("no_permission");
            return;
        }
        if (!isLocationEnabled(context)) {
            Log.w(TAG, "[LOC] requestSingleLocation: location_disabled");
            callback.onLocationFailed("location_disabled");
            return;
        }
        Log.d(TAG, "[LOC] requestSingleLocation: submitting LocationTask");
        IO.execute(new LocationTask(context, callback));
    }

    private static class LocationTask implements Runnable {
        private final Context context;
        private final Callback callback;
        private final Handler main = new Handler(Looper.getMainLooper());
        private volatile boolean finished = false;

        LocationTask(Context context, Callback cb) {
            this.context = context.getApplicationContext();
            this.callback = cb;
        }

        @Override
        public void run() {
            try {
                Log.d(TAG, "[LOC] LocationTask.run start");
                final LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                if (lm == null) {
                    Log.w(TAG, "[LOC] LocationTask: no_service");
                    deliverFailed("no_service");
                    return;
                }
                boolean gpsEnabled = false, networkEnabled = false;
                try { gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER); } catch (Exception ignored) {}
                try { networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER); } catch (Exception ignored) {}
                Log.d(TAG, "[LOC] providers: gps=" + gpsEnabled + " network=" + networkEnabled);

                if (!gpsEnabled && !networkEnabled) {
                    Log.w(TAG, "[LOC] LocationTask: location_disabled");
                    deliverFailed("location_disabled");
                    return;
                }

                Location lastKnown = null;
                try {
                    if (gpsEnabled) {
                        lastKnown = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    }
                } catch (SecurityException ignored) {}
                if (lastKnown == null) {
                    try {
                        if (networkEnabled) {
                            lastKnown = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        }
                    } catch (SecurityException ignored) {}
                }
                Log.d(TAG, "[LOC] lastKnown=" + (lastKnown == null ? "null" : (lastKnown.getLatitude() + "," + lastKnown.getLongitude() + " age=" + (System.currentTimeMillis() - lastKnown.getTime()) + "ms")));
                if (lastKnown != null && System.currentTimeMillis() - lastKnown.getTime() < 5 * 60 * 1000L) {
                    Log.d(TAG, "[LOC] using recent lastKnown location");
                    deliverSuccess(lastKnown);
                    return;
                }

                final boolean finalGpsEnabled = gpsEnabled;
                final boolean finalNetworkEnabled = networkEnabled;

                final LocationListener listener = new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        if (finished) return;
                        finished = true;
                        try { lm.removeUpdates(this); } catch (Exception ignored) {}
                        deliverSuccess(location);
                    }
                    @Override public void onStatusChanged(String p, int s, Bundle extras) {}
                    @Override public void onProviderEnabled(String p) {}
                    @Override public void onProviderDisabled(String p) {}
                };

                try {
                    if (gpsEnabled) {
                        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, listener, Looper.getMainLooper());
                    }
                    if (networkEnabled) {
                        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, listener, Looper.getMainLooper());
                    }
                } catch (SecurityException se) {
                    deliverFailed("security_exception");
                    return;
                } catch (IllegalArgumentException iae) {
                    deliverFailed("invalid_provider");
                    return;
                }

                main.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (finished) return;
                        finished = true;
                        Log.w(TAG, "[LOC] LocationTask TIMEOUT after " + LOCATION_TIMEOUT_MS + "ms");
                        try { lm.removeUpdates(listener); } catch (Exception ignored) {}
                        Location lk = null;
                        try {
                            if (finalGpsEnabled) lk = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        } catch (Exception ignored) {}
                        if (lk == null) {
                            try {
                                if (finalNetworkEnabled) lk = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                            } catch (Exception ignored) {}
                        }
                        Log.d(TAG, "[LOC] timeout fallback lastKnown=" + (lk == null ? "null" : (lk.getLatitude() + "," + lk.getLongitude())));
                        if (lk != null) {
                            deliverSuccess(lk);
                        } else {
                            deliverFailed("timeout");
                        }
                    }
                }, LOCATION_TIMEOUT_MS);

            } catch (Exception e) {
                Log.e(TAG, "LocationTask error", e);
                deliverFailed("exception");
            }
        }

        private void deliverSuccess(final Location location) {
            double lat = location.getLatitude();
            double lon = location.getLongitude();
            if (Math.abs(lat) > 90.0 && Math.abs(lon) <= 90.0) {
                double tmp = lat;
                lat = lon;
                lon = tmp;
            }
            final LocationInfo info = new LocationInfo();
            info.latitude = lat;
            info.longitude = lon;
            info.timestamp = System.currentTimeMillis();
            Log.d(TAG, "[LOC] deliverSuccess lat=" + info.latitude + " lon=" + info.longitude + " ts=" + info.timestamp);
            main.post(new Runnable() {
                @Override
                public void run() {
                    callback.onLocationSuccess(info);
                }
            });
        }

        private void deliverFailed(final String reason) {
            Log.w(TAG, "[LOC] deliverFailed reason=" + reason);
            main.post(new Runnable() {
                @Override
                public void run() {
                    callback.onLocationFailed(reason);
                }
            });
        }
    }

    public static void reverseGeocode(final double lat, final double lon, final Callback cb) {
        final Callback callback = cb != null ? cb : new Callback() {
            @Override public void onLocationSuccess(LocationInfo i) {}
            @Override public void onLocationFailed(String r) {}
        };
        Log.d(TAG, "[GEO] reverseGeocode start lat=" + lat + " lon=" + lon);
        IO.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    java.util.concurrent.Callable<String> z14 = new CallableReverse(lat, lon, 14);
                    java.util.concurrent.Callable<String> z10 = new CallableReverse(lat, lon, 10);
                    java.util.concurrent.Future<String> f14 = IO.submit(z14);
                    java.util.concurrent.Future<String> f10 = IO.submit(z10);

                    String resp14 = null, resp10 = null;
                    try { resp14 = f14.get(15, TimeUnit.SECONDS); } catch (Exception e) { Log.w(TAG, "[GEO] z14 fetch exception: " + e.getMessage()); }
                    try { resp10 = f10.get(15, TimeUnit.SECONDS); } catch (Exception e) { Log.w(TAG, "[GEO] z10 fetch exception: " + e.getMessage()); }

                    Log.d(TAG, "[GEO] resp14=" + (resp14 == null ? "null" : resp14));
                    Log.d(TAG, "[GEO] resp10=" + (resp10 == null ? "null" : resp10));

                    if (resp14 == null && resp10 == null) {
                        Log.w(TAG, "[GEO] both responses null -> network_error");
                        deliverFailed("network_error");
                        return;
                    }

                    final LocationInfo info = new LocationInfo();
                    info.latitude = lat;
                    info.longitude = lon;
                    info.timestamp = System.currentTimeMillis();

                    AddressFields a14 = parseAddress(resp14);
                    AddressFields a10 = parseAddress(resp10);
                    Log.d(TAG, "[GEO] a14: country=" + a14.country + " state=" + a14.state + " city=" + a14.city + " county=" + a14.county + " city_district=" + a14.city_district + " town=" + a14.town + " village=" + a14.village + " hamlet=" + a14.hamlet + " suburb=" + a14.suburb + " quarter=" + a14.quarter);
                    Log.d(TAG, "[GEO] a10: country=" + a10.country + " state=" + a10.state + " city=" + a10.city + " county=" + a10.county + " city_district=" + a10.city_district + " town=" + a10.town + " village=" + a10.village + " hamlet=" + a10.hamlet + " suburb=" + a10.suburb + " quarter=" + a10.quarter);

                    String displayName14 = extractDisplayName(resp14);
                    String displayName10 = extractDisplayName(resp10);
                    String[] h14 = parseDisplayNameHierarchy(displayName14);
                    String[] h10 = parseDisplayNameHierarchy(displayName10);
                    String[] h = mergeHierarchies(h14, h10);
                    Log.d(TAG, "[GEO] displayName14=" + displayName14);
                    Log.d(TAG, "[GEO] displayName10=" + displayName10);
                    Log.d(TAG, "[GEO] h14=" + Arrays.toString(h14));
                    Log.d(TAG, "[GEO] h10=" + Arrays.toString(h10));
                    Log.d(TAG, "[GEO] merged h=" + Arrays.toString(h));

                    info.country = firstNonEmpty(a10.country, a14.country, "中国");

                    if (h.length == 0) {
                        Log.d(TAG, "[GEO] hierarchy empty, fallback to address fields");
                        info.province = firstNonEmpty(a10.state, a14.state, "");
                        info.city = firstNonEmpty(a14.city, "");
                        info.district = firstNonEmpty(a10.city, a10.county, a10.city_district, "");
                        info.town = "";
                    } else {
                        info.province = h[0];
                        if (isDirectCity(info.province)) {
                            info.city = info.province;
                            if (h.length >= 2) info.district = h[1];
                            for (int i = 2; i < h.length; i++) {
                                if (isTownLike(h[i])) { info.town = h[i]; break; }
                            }
                            if (info.town.isEmpty() && h.length >= 3) info.town = h[h.length - 1];
                        } else {
                            if (h.length >= 2) info.city = h[1];
                            else info.city = info.province;
                            if (h.length >= 3) {
                                if (isTownLike(h[2])) {
                                    info.town = h[2];
                                } else {
                                    info.district = h[2];
                                    for (int i = 3; i < h.length; i++) {
                                        if (isTownLike(h[i])) { info.town = h[i]; break; }
                                    }
                                    if (info.town.isEmpty() && h.length >= 4) info.town = h[h.length - 1];
                                }
                            }
                        }
                    }

                    if (info.province.isEmpty() && !info.city.isEmpty()) {
                        info.province = info.city;
                    }
                    if (info.city.isEmpty() && !info.province.isEmpty()) {
                        info.city = info.province;
                    }
                    if (info.province.isEmpty()) info.province = "北京";
                    if (info.city.isEmpty()) info.city = "北京";

                    Log.d(TAG, "[GEO] FINAL info: country=" + info.country + " province=" + info.province + " city=" + info.city + " district=" + info.district + " town=" + info.town + " display=" + info.displayLocationName());
                    deliverSuccess(info);
                } catch (Exception e) {
                    Log.e(TAG, "[GEO] reverseGeocode error", e);
                    deliverFailed("exception");
                }
            }

            private void deliverSuccess(final LocationInfo info) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onLocationSuccess(info);
                    }
                });
            }

            private void deliverFailed(final String reason) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onLocationFailed(reason);
                    }
                });
            }
        });
    }

    private static class CallableReverse implements java.util.concurrent.Callable<String> {
        final double lat, lon;
        final int zoom;
        CallableReverse(double lat, double lon, int zoom) {
            this.lat = lat; this.lon = lon; this.zoom = zoom;
        }
        @Override
        public String call() throws Exception {
            String qs = "?lat=" + lat + "&lon=" + lon
                    + "&format=jsonv2&addressdetails=1&accept-language=zh-CN&zoom=" + zoom;
            URL url = new URL(REVERSE_GEOCODE_URL + qs);
            Log.d(TAG, "[GEO] HTTP GET zoom=" + zoom + " url=" + url);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            try {
                trustAllHostsForOldAndroid(conn);
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(10_000);
                conn.setRequestProperty("User-Agent", "MusicClock/1.0 (android)");
                int code = conn.getResponseCode();
                Log.d(TAG, "[GEO] HTTP zoom=" + zoom + " responseCode=" + code);
                if (code != HttpURLConnection.HTTP_OK) {
                    Log.w(TAG, "[GEO] HTTP zoom=" + zoom + " not OK, code=" + code);
                    return null;
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                return sb.toString();
            } finally {
                conn.disconnect();
            }
        }
    }

    private static class AddressFields {
        String country = "";
        String state = "";
        String city = "";
        String county = "";
        String city_district = "";
        String town = "";
        String village = "";
        String hamlet = "";
        String suburb = "";
        String quarter = "";
    }

    private static AddressFields parseAddress(String json) {
        AddressFields f = new AddressFields();
        if (json == null || json.isEmpty()) return f;
        try {
            int addrIdx = json.indexOf("\"address\":");
            if (addrIdx < 0) return f;
            int braceStart = json.indexOf('{', addrIdx);
            if (braceStart < 0) return f;
            int depth = 0;
            int braceEnd = -1;
            for (int i = braceStart; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) { braceEnd = i; break; }
                }
            }
            if (braceEnd < 0) return f;
            String addrJson = json.substring(braceStart, braceEnd + 1);

            f.country = extractString(addrJson, "country");
            f.state = extractString(addrJson, "state");
            f.city = extractString(addrJson, "city");
            f.county = extractString(addrJson, "county");
            f.city_district = extractString(addrJson, "city_district");
            f.town = extractString(addrJson, "town");
            f.village = extractString(addrJson, "village");
            f.hamlet = extractString(addrJson, "hamlet");
            f.suburb = extractString(addrJson, "suburb");
            f.quarter = extractString(addrJson, "quarter");
        } catch (Exception e) {
            Log.e(TAG, "parseAddress error", e);
        }
        return f;
    }

    private static String extractString(String json, String key) {
        if (json == null || key == null) return "";
        String pat = "\"" + key + "\":";
        int idx = json.indexOf(pat);
        if (idx < 0) return "";
        int i = idx + pat.length();
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
        if (i >= json.length()) return "";
        char c = json.charAt(i);
        if (c == '"') {
            int end = i + 1;
            StringBuilder sb = new StringBuilder();
            while (end < json.length()) {
                char ch = json.charAt(end);
                if (ch == '\\' && end + 1 < json.length()) {
                    char next = json.charAt(end + 1);
                    switch (next) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'n': sb.append('\n'); break;
                        case 't': sb.append('\t'); break;
                        case 'r': sb.append('\r'); break;
                        default: sb.append(next); break;
                    }
                    end += 2;
                } else if (ch == '"') {
                    end++;
                    break;
                } else {
                    sb.append(ch);
                    end++;
                }
            }
            return sb.toString().trim();
        } else {
            return "";
        }
    }

    private static String firstNonEmpty(String... vals) {
        for (String v : vals) {
            if (v != null && !v.isEmpty()) return v;
        }
        return "";
    }

    private static String extractDisplayName(String json) {
        return extractString(json, "display_name");
    }

    private static String[] parseDisplayNameHierarchy(String displayName) {
        if (displayName == null || displayName.isEmpty()) return new String[0];
        String[] parts = displayName.split(",");
        List<String> reversed = new ArrayList<>();
        boolean foundCountry = false;
        for (int i = parts.length - 1; i >= 0; i--) {
            String s = parts[i].trim();
            if (s.isEmpty()) continue;
            if (!foundCountry) {
                if (s.equals("中国") || s.equals("China")) {
                    foundCountry = true;
                }
                continue;
            }
            if (s.matches("^\\d+$")) continue;
            reversed.add(s);
        }
        return reversed.toArray(new String[0]);
    }

    private static String[] mergeHierarchies(String[] h14, String[] h10) {
        if (h14 == null || h14.length == 0) return h10 == null ? new String[0] : h10;
        if (h10 == null || h10.length == 0) return h14;
        int lca = 0;
        while (lca < h14.length && lca < h10.length && h14[lca].equals(h10[lca])) {
            lca++;
        }
        List<String> result = new ArrayList<>();
        for (int i = 0; i < lca; i++) result.add(h14[i]);
        for (int i = lca; i < h10.length; i++) result.add(h10[i]);
        for (int i = lca; i < h14.length; i++) result.add(h14[i]);
        return result.toArray(new String[0]);
    }

    private static boolean isDirectCity(String name) {
        if (name == null) return false;
        return name.equals("北京") || name.equals("北京市")
                || name.equals("上海") || name.equals("上海市")
                || name.equals("天津") || name.equals("天津市")
                || name.equals("重庆") || name.equals("重庆市");
    }

    private static boolean isTownLike(String name) {
        if (name == null || name.isEmpty()) return false;
        return name.endsWith("街道") || name.endsWith("镇") || name.endsWith("乡");
    }
}