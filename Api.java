// Single-file API: Fibonacci (recursive) + multiply using addition-only.

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Api {

    // --- business logic ---
    public static long fib(int n) {
        if (n < 0) throw new IllegalArgumentException("n must be >= 0");
        if (n <= 1) return n;
        return fib(n - 1) + fib(n - 2);
    }

    public static long multiply(long a, long b) {
        boolean negative = (a < 0) ^ (b < 0);
        long x = Math.abs(a), y = Math.abs(b), res = 0;
        for (long i = 0; i < y; i++) res = res + x; // addition only
        return negative ? -res : res;
    }

    // --- tiny HTTP server ---
    public static void main(String[] args) throws Exception {
        int port = 8080; // change to 8081 if 8080 is busy
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/fib", ex -> {
            Map<String,String> q = qparams(ex.getRequestURI().getRawQuery());
            try {
                if (!q.containsKey("n")) { send(ex, 400, err("missing 'n'")); return; }
                int n = Integer.parseInt(q.get("n"));
                if (n < 0) { send(ex, 400, err("n must be >= 0")); return; }
                long v = fib(n);
                send(ex, 200, "{\"n\":"+n+",\"value\":"+v+"}");
            } catch (NumberFormatException e) { send(ex, 400, err("invalid integer 'n'")); }
        });

        server.createContext("/multiply", ex -> {
            Map<String,String> q = qparams(ex.getRequestURI().getRawQuery());
            try {
                if (!q.containsKey("a") || !q.containsKey("b")) {
                    send(ex, 400, err("missing 'a' and/or 'b'")); return;
                }
                long a = Long.parseLong(q.get("a"));
                long b = Long.parseLong(q.get("b"));
                long p = multiply(a, b);
                send(ex, 200, "{\"a\":"+a+",\"b\":"+b+",\"product\":"+p+"}");
            } catch (NumberFormatException e) { send(ex, 400, err("invalid long in 'a' or 'b'")); }
        });

        server.setExecutor(null);
        System.out.println("API ready at http://localhost:"+port+"  (endpoints: /fib, /multiply)");
        server.start();
    }

    // helpers
    private static Map<String,String> qparams(String raw) {
        Map<String,String> m = new HashMap<>();
        if (raw == null || raw.isEmpty()) return m;
        for (String p : raw.split("&")) {
            int i = p.indexOf('=');
            String k = i>=0 ? p.substring(0,i) : p;
            String v = i>=0 ? p.substring(i+1) : "";
            m.put(ud(k), ud(v));
        }
        return m;
    }
    private static String ud(String s){ return URLDecoder.decode(s, StandardCharsets.UTF_8); }
    private static String err(String msg){ return "{\"error\":\""+msg.replace("\"","\\\"")+"\"}"; }
    private static void send(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try(OutputStream os = ex.getResponseBody()){ os.write(bytes); }
    }
}
