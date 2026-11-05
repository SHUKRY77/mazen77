package com.abusultan.vip;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import android.util.Base64;
import org.json.JSONObject;

public class VlessParser {

    // فكّ ترميز URL بأمان (يرجّع النص كما هو لو فشل)
    private static String dec(String s) {
        if (s == null) return "";
        try { return URLDecoder.decode(s, "UTF-8"); } catch (Exception e) { return s; }
    }

    // محلّل يدوي للـ query يقبل أي قيمة بلا اعتماد على Uri.parse الهشّ
    private static Map<String, String> parseQuery(String link) {
        Map<String, String> q = new HashMap<>();
        int qIdx = link.indexOf('?');
        if (qIdx < 0) return q;
        int hash = link.indexOf('#', qIdx);
        String query = (hash >= 0) ? link.substring(qIdx + 1, hash) : link.substring(qIdx + 1);
        for (String pair : query.split("&")) {
            if (pair.isEmpty()) continue;
            int eq = pair.indexOf('=');
            if (eq < 0) { q.put(dec(pair).toLowerCase(), ""); }
            else { q.put(dec(pair.substring(0, eq)).toLowerCase(), dec(pair.substring(eq + 1))); }
        }
        return q;
    }

    private static String generateVmessConfig(String link) {
        try {
            String encoded = link.substring("vmess://".length()).trim();
            String json = new String(Base64.decode(encoded, Base64.DEFAULT), "UTF-8");
            JSONObject vmess = new JSONObject(json);
            String address = vmess.optString("add", "");
            String id = vmess.optString("id", "");
            if (address.isEmpty() || id.isEmpty()) return null;
            int port = vmess.optInt("port", 443);
            int alterId = vmess.optInt("aid", 0);
            String network = vmess.optString("net", "tcp").toLowerCase();
            String tls = vmess.optString("tls", "").toLowerCase();
            String host = vmess.optString("host", "");
            String path = vmess.optString("path", "/");
            if (path.isEmpty()) path = "/";
            String sni = vmess.optString("sni", host.isEmpty() ? address : host);

            StringBuilder c = new StringBuilder();
            c.append("{\"log\":{\"loglevel\":\"warning\"},");
            c.append("\"inbounds\":[{\"port\":10808,\"listen\":\"127.0.0.1\",\"protocol\":\"socks\",\");
            c.append("\"settings\":{\"auth\":\"noauth\",\"udp\":true},\"sniffing\":{\"enabled\":true,\"destOverride\":[\"http\",\"tls\"]}}],");
            c.append("\"outbounds\":[{\"protocol\":\"vmess\",\"settings\":{\"vnext\":[{\");
            c.append("\"address\":").append(JSONObject.quote(address)).append(",\"port\":").append(port).append(",");
            c.append("\"users\":[{\"id\":").append(JSONObject.quote(id)).append(",\"alterId\":").append(alterId).append(",\"security\":\"auto\"}]}]},");
            c.append("\"streamSettings\":{\"network\":").append(JSONObject.quote(network));
            if ("tls".equals(tls)) {
                c.append(",\"security\":\"tls\",\"tlsSettings\":{\"serverName\":").append(JSONObject.quote(sni)).append("}");
            } else {
                c.append(",\"security\":\"none\"");
            }
            if ("ws".equals(network)) {
                c.append(",\"wsSettings\":{\"path\":").append(JSONObject.quote(path));
                if (!host.isEmpty()) c.append(",\"headers\":{\"Host\":").append(JSONObject.quote(host)).append("}");
                c.append("}");
            } else if ("grpc".equals(network)) {
                c.append(",\"grpcSettings\":{\"serviceName\":").append(JSONObject.quote(path.startsWith("/") ? path.substring(1) : path)).append("}");
            }
            c.append("}}]}");
            return c.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public static String generateXrayConfig(String link) {
        try {
            if (link == null || link.isEmpty()) return null;

            // استخراج المخطط (vless / trojan / vmess)
            int schemeEnd = link.indexOf("://");
            if (schemeEnd < 0) return null;
            String scheme = link.substring(0, schemeEnd).toLowerCase();
            if ("vmess".equals(scheme)) return generateVmessConfig(link);
            String rest = link.substring(schemeEnd + 3);

            // ===== استخراج الـ UID/Password يدوياً: كل ما قبل أول @ =====
            // هذا يقبل أي قيمة مهما كانت رموزها (أرقام/أحرف/رموز/base64)
            int at = rest.indexOf('@');
            if (at < 0) return null;
            String passwordOrUuid = dec(rest.substring(0, at));
            String afterAt = rest.substring(at + 1);

            // ===== استخراج العنوان والمنفذ =====
            int hostEnd = afterAt.length();
            for (int i = 0; i < afterAt.length(); i++) {
                char c = afterAt.charAt(i);
                if (c == ':' || c == '/' || c == '?' || c == '#') { hostEnd = i; break; }
            }
            String address = afterAt.substring(0, hostEnd);

            int port = 443;
            if (hostEnd < afterAt.length() && afterAt.charAt(hostEnd) == ':') {
                int ps = hostEnd + 1;
                int pe = ps;
                while (pe < afterAt.length() && Character.isDigit(afterAt.charAt(pe))) pe++;
                if (pe > ps) { try { port = Integer.parseInt(afterAt.substring(ps, pe)); } catch (Exception e) {} }
            }
            if (address.isEmpty()) return null;

            // ===== معطيات الـ stream من الـ query =====
            Map<String, String> q = parseQuery(link);
            String security = q.containsKey("security") ? q.get("security") : "";
            String type = q.containsKey("type") ? q.get("type") : "";
            String host = q.containsKey("host") ? q.get("host") : "";
            String path = q.containsKey("path") ? q.get("path") : "";
            String sni = q.containsKey("sni") ? q.get("sni") : "";
            String alpn = q.containsKey("alpn") ? q.get("alpn") : "";
            String fp = q.containsKey("fp") ? q.get("fp") : "";

            if (security == null || security.isEmpty()) security = "none";
            if (type == null || type.isEmpty()) type = "tcp";
            if (host == null) host = "";
            if (path == null || path.isEmpty()) path = "/";
            // Some panels store WebSocket early-data as "path?ed=...".
            // Xray expects wsSettings.path without the query suffix.
            int pathQuery = path.indexOf('?');
            if (pathQuery >= 0) path = path.substring(0, pathQuery);
            if (path.isEmpty()) path = "/";
            if (sni == null || sni.isEmpty()) sni = address;

            StringBuilder config = new StringBuilder();
            config.append("{\n");
            config.append("  \"log\": {\"loglevel\": \"warning\"},\n");
            config.append("  \"inbounds\": [{\n");
            config.append("    \"port\": 10808,\n");
            config.append("    \"listen\": \"127.0.0.1\",\n");
            config.append("    \"protocol\": \"socks\",\n");
            config.append("    \"settings\": {\"auth\": \"noauth\", \"udp\": true},\n");
            config.append("    \"sniffing\": {\"enabled\": true, \"destOverride\": [\"http\", \"tls\"]}\n");
            config.append("  }],\n");
            config.append("  \"outbounds\": [{\n");

            if ("trojan".equals(scheme)) {
                config.append("    \"protocol\": \"trojan\",\n");
                config.append("    \"settings\": {\n");
                config.append("      \"servers\": [{\n");
                config.append("        \"address\": \"").append(address).append("\",\n");
                config.append("        \"port\": ").append(port).append(",\n");
                config.append("        \"password\": ").append(org.json.JSONObject.quote(passwordOrUuid)).append("\n");
                config.append("      }]\n");
                config.append("    },\n");
            } else {
                config.append("    \"protocol\": \"vless\",\n");
                config.append("    \"settings\": {\n");
                config.append("      \"vnext\": [{\n");
                config.append("        \"address\": \"").append(address).append("\",\n");
                config.append("        \"port\": ").append(port).append(",\n");
                config.append("        \"users\": [{\"id\": ").append(org.json.JSONObject.quote(passwordOrUuid)).append(", \"encryption\": \"none\"}]\n");
                config.append("      }]\n");
                config.append("    },\n");
            }

            config.append("    \"streamSettings\": {\n");
            config.append("      \"network\": \"").append(type).append("\",\n");
            config.append("      \"security\": \"").append(security).append("\"");

            if ("ws".equals(type)) {
                config.append(",\n      \"wsSettings\": {\"path\": ").append(org.json.JSONObject.quote(path))
                      .append(", \"headers\": {\"Host\": ").append(org.json.JSONObject.quote(host)).append("}}");
            }
            if ("tls".equals(security)) {
                config.append(",\n      \"tlsSettings\": {\"serverName\": ").append(org.json.JSONObject.quote(sni));
                if (!alpn.isEmpty()) {
                    config.append(", \"alpn\": [");
                    String[] alpns = alpn.split(",");
                    for (int i = 0; i < alpns.length; i++) {
                        if (i > 0) config.append(", ");
                        config.append(org.json.JSONObject.quote(alpns[i].trim()));
                    }
                    config.append("]");
                }
                if (!fp.isEmpty()) config.append(", \"fingerprint\": ").append(org.json.JSONObject.quote(fp));
                config.append("}");
            }

            config.append("\n    }\n");
            config.append("  }]\n");
            config.append("}");

            return config.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
