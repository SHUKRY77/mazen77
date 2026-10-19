package com.maxtunnel.vip;

import android.net.VpnService;
import android.util.Log;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RouteHelper {

    private static final int MAX_EXCLUDED = 8;

    public static boolean isIpv4Literal(String s) {
        if (s == null) return false;
        return s.matches("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");
    }

    /** يحلّ الدومين إلى عناوين IPv4 (يجب استدعاؤه قبل establish وخارج الـ Main Thread) */
    public static List<String> resolveIpv4(String host) {
        List<String> result = new ArrayList<>();
        if (host == null || host.isEmpty()) return result;
        if (isIpv4Literal(host)) {
            result.add(host);
            return result;
        }
        try {
            InetAddress[] all = InetAddress.getAllByName(host);
            for (InetAddress a : all) {
                if (a instanceof Inet4Address) {
                    String ip = a.getHostAddress();
                    if (!result.contains(ip)) result.add(ip);
                }
            }
        } catch (Exception e) {
            Log.e("RouteHelper", "resolve failed: " + e.getMessage());
        }
        return result;
    }

    /** يضيف كل مسارات 0.0.0.0/0 ما عدا العناوين المستثناة */
    public static void addRoutesExcluding(VpnService.Builder builder, Collection<String> excludedIps) {
        List<Long> ex = new ArrayList<>();
        if (excludedIps != null) {
            for (String ip : excludedIps) {
                if (ex.size() >= MAX_EXCLUDED) break;
                if (isIpv4Literal(ip)) ex.add(toLong(ip));
            }
        }
        if (ex.isEmpty()) {
            builder.addRoute("0.0.0.0", 0);
            return;
        }
        split(builder, 0L, 0, ex);
    }

    private static void split(VpnService.Builder b, long net, int prefix, List<Long> ex) {
        long size = 1L << (32 - prefix);
        long end = net + size - 1;
        boolean has = false;
        for (long e : ex) {
            if (e >= net && e <= end) { has = true; break; }
        }
        if (!has) {
            b.addRoute(toIp(net), prefix);
            return;
        }
        if (prefix == 32) return; // هذا هو IP المستثنى نفسه
        split(b, net, prefix + 1, ex);
        split(b, net + (size / 2), prefix + 1, ex);
    }

    private static long toLong(String ip) {
        String[] p = ip.split("\\.");
        return (Long.parseLong(p[0]) << 24) | (Long.parseLong(p[1]) << 16)
                | (Long.parseLong(p[2]) << 8) | Long.parseLong(p[3]);
    }

    private static String toIp(long n) {
        return ((n >> 24) & 255) + "." + ((n >> 16) & 255) + "." + ((n >> 8) & 255) + "." + (n & 255);
    }
}