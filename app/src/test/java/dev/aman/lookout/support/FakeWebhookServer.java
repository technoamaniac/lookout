package dev.aman.lookout.support;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A minimal loopback HTTP/1.1 server that records requests and answers with a fixed status.
 * The JDK's own HttpServer is not on the Android unit-test classpath.
 */
public final class FakeWebhookServer implements AutoCloseable {
    /** What the server saw, reduced to the parts the tests check. */
    public static final class Request {
        public final String method;
        public final String path;
        public final String contentType;
        public final String webhookId;
        public final String body;

        Request(String method, String path, String contentType, String webhookId, String body) {
            this.method = method;
            this.path = path;
            this.contentType = contentType;
            this.webhookId = webhookId;
            this.body = body;
        }
    }

    /** A canned answer for one path. */
    private static final class Route {
        final int status;
        final String location;
        final byte[] body;

        Route(int status, String location, byte[] body) {
            this.status = status;
            this.location = location;
            this.body = body;
        }
    }

    private final ServerSocket socket;
    private final Map<String, Route> routes = new ConcurrentHashMap<>();
    private final List<Request> requests = new CopyOnWriteArrayList<>();
    private volatile int status = 204;

    public FakeWebhookServer() throws IOException {
        socket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress());
        Thread acceptor = new Thread(this::acceptUntilClosed, "fake-webhook-server");
        acceptor.setDaemon(true);
        acceptor.start();
    }

    /** Status for every following response; a 3xx also carries a Location header. */
    public void respondWith(int status) {
        this.status = status;
    }

    /** Overrides the default response for one path; location may be null. */
    public void route(String path, int status, String location, byte[] body) {
        routes.put(path, new Route(status, location, body));
    }

    /** The server's own address, for building absolute redirect targets. */
    public String baseUrl() {
        return "http://127.0.0.1:" + socket.getLocalPort();
    }

    public URL url(String path) throws IOException {
        return new URL("http://127.0.0.1:" + socket.getLocalPort() + path);
    }

    public List<Request> requests() {
        return requests;
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }

    private void acceptUntilClosed() {
        while (!socket.isClosed()) {
            try (Socket connection = socket.accept()) {
                Request request = readRequest(connection);
                requests.add(request);
                writeResponse(connection, request.path);
            } catch (IOException e) {
                // The server was closed, or the client hung up mid-request
            }
        }
    }

    private static Request readRequest(Socket connection) throws IOException {
        BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
        String[] requestLine = in.readLine().split(" ");
        String contentType = null;
        String webhookId = null;
        int contentLength = 0;
        for (String line = in.readLine(); line != null && !line.isEmpty(); line = in.readLine()) {
            String[] header = line.split(":", 2);
            String name = header[0].trim();
            String value = header[1].trim();
            if (name.equalsIgnoreCase("Content-Type")) contentType = value;
            if (name.equalsIgnoreCase("webhook-id")) webhookId = value;
            if (name.equalsIgnoreCase("Content-Length")) contentLength = Integer.parseInt(value);
        }
        // The body is ASCII JSON in these tests, so its byte length equals its char length
        char[] body = new char[contentLength];
        for (int read = 0; read < contentLength; ) {
            int n = in.read(body, read, contentLength - read);
            if (n < 0) throw new IOException("Body ended early");
            read += n;
        }
        return new Request(requestLine[0], requestLine[1], contentType, webhookId, new String(body));
    }

    private void writeResponse(Socket connection, String path) throws IOException {
        Route route = routes.get(path);
        int code = route != null ? route.status : status;
        String location = route != null ? route.location : (code / 100 == 3 ? "/elsewhere" : null);
        byte[] body = route != null ? route.body : new byte[0];
        StringBuilder head = new StringBuilder("HTTP/1.1 ").append(code).append(" Status\r\n");
        if (location != null) head.append("Location: ").append(location).append("\r\n");
        head.append("Content-Length: ").append(body.length).append("\r\nConnection: close\r\n\r\n");
        OutputStream out = connection.getOutputStream();
        out.write(head.toString().getBytes(StandardCharsets.UTF_8));
        out.write(body);
        out.flush();
    }
}
