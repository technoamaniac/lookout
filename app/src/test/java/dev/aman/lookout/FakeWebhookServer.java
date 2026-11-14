package dev.aman.lookout;

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
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A minimal loopback HTTP/1.1 server that records requests and answers with a fixed status.
 * The JDK's own HttpServer is not on the Android unit-test classpath.
 */
final class FakeWebhookServer implements AutoCloseable {
    /** What the server saw, reduced to the parts the tests check. */
    static final class Request {
        final String method;
        final String path;
        final String contentType;
        final String body;

        Request(String method, String path, String contentType, String body) {
            this.method = method;
            this.path = path;
            this.contentType = contentType;
            this.body = body;
        }
    }

    private final ServerSocket socket;
    private final List<Request> requests = new CopyOnWriteArrayList<>();
    private volatile int status = 204;

    FakeWebhookServer() throws IOException {
        socket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress());
        Thread acceptor = new Thread(this::acceptUntilClosed, "fake-webhook-server");
        acceptor.setDaemon(true);
        acceptor.start();
    }

    /** Status for every following response; a 3xx also carries a Location header. */
    void respondWith(int status) {
        this.status = status;
    }

    URL url(String path) throws IOException {
        return new URL("http://127.0.0.1:" + socket.getLocalPort() + path);
    }

    List<Request> requests() {
        return requests;
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }

    private void acceptUntilClosed() {
        while (!socket.isClosed()) {
            try (Socket connection = socket.accept()) {
                requests.add(readRequest(connection));
                writeResponse(connection);
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
        int contentLength = 0;
        for (String line = in.readLine(); line != null && !line.isEmpty(); line = in.readLine()) {
            String[] header = line.split(":", 2);
            String name = header[0].trim();
            String value = header[1].trim();
            if (name.equalsIgnoreCase("Content-Type")) contentType = value;
            if (name.equalsIgnoreCase("Content-Length")) contentLength = Integer.parseInt(value);
        }
        // The body is ASCII JSON in these tests, so its byte length equals its char length
        char[] body = new char[contentLength];
        for (int read = 0; read < contentLength; ) {
            int n = in.read(body, read, contentLength - read);
            if (n < 0) throw new IOException("Body ended early");
            read += n;
        }
        return new Request(requestLine[0], requestLine[1], contentType, new String(body));
    }

    private void writeResponse(Socket connection) throws IOException {
        int code = status;
        StringBuilder response = new StringBuilder("HTTP/1.1 ").append(code).append(" Status\r\n");
        if (code / 100 == 3) response.append("Location: /elsewhere\r\n");
        response.append("Content-Length: 0\r\nConnection: close\r\n\r\n");
        OutputStream out = connection.getOutputStream();
        out.write(response.toString().getBytes(StandardCharsets.UTF_8));
        out.flush();
    }
}
