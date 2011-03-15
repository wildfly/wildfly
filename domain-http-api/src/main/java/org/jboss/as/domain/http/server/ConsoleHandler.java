package org.jboss.as.domain.http.server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Heiko Braun
 * @date 3/14/11
 */
public class ConsoleHandler implements HttpHandler {

    public static final String CONTEXT = "/console";
    private static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

    private ClassLoader loader = null;

    private static Map<String, String> contentTypeMapping = new ConcurrentHashMap<String, String>();

    static {
        contentTypeMapping.put(".js",   "application/javascript");
        contentTypeMapping.put(".html", "text/html");
        contentTypeMapping.put(".htm",  "text/html");
        contentTypeMapping.put(".css",  "text/css");
        contentTypeMapping.put(".gif",  "image/gif");
        contentTypeMapping.put(".png",  "image/png");
        contentTypeMapping.put(".jpeg", "image/jpeg");
    }

    public ConsoleHandler() {
    }

    public ConsoleHandler(ClassLoader loader) {
        this.loader = loader;
    }

    @Override
    public void handle(HttpExchange http) throws IOException {
        final URI uri = http.getRequestURI();
        final String requestMethod = http.getRequestMethod();

        // only GET supported
        if (!"GET".equals(requestMethod)) {
            http.sendResponseHeaders(405, -1);
            return;
        }

        // normalize to request resource
        String path = uri.getPath();
        String resource = path.substring(CONTEXT.length(), path.length());
        if(resource.startsWith("/")) resource = resource.substring(1);

        // respond 404 directory request
        if(resource.equals("") || resource.indexOf(".")==-1) respond404(http);

        // load resource
        InputStream inputStream = getLoader().getResourceAsStream(resource);
        if(inputStream!=null) {

            final Headers responseHeaders = http.getResponseHeaders();
            responseHeaders.add("Content-Type", resolveContentType(path));
            responseHeaders.add("Access-Control-Allow-Origin", "*");
            http.sendResponseHeaders(200, 0);

            OutputStream outputStream = http.getResponseBody();

            int nextChar;
            while ( ( nextChar = inputStream.read() ) != -1  ) {
                outputStream.write(nextChar);
            }

            outputStream.flush();
            safeClose(outputStream);
            safeClose(inputStream);

        } else {
            respond404(http);
        }

    }

    private void safeClose(Closeable close) {
        try {
            close.close();
        } catch (Throwable eat) {
        }
    }

    private String resolveContentType(String resource) {
        assert resource.indexOf(".")!=-1 : "Invalid resource";

        String contentType = null;
        for(String suffix : contentTypeMapping.keySet()) {
            if(resource.endsWith(suffix)) {
                contentType = contentTypeMapping.get(suffix);
                break;
            }
        }

        if(null==contentType) contentType = APPLICATION_OCTET_STREAM;

        return contentType;
    }

    private void respond404(HttpExchange http) throws IOException {

        final Headers responseHeaders = http.getResponseHeaders();
        responseHeaders.add("Content-Type", "text/html");
        responseHeaders.add("Access-Control-Allow-Origin", "*");
        http.sendResponseHeaders(404, 0);
        OutputStream out = http.getResponseBody();
        out.flush();
        safeClose(out);
    }

    private ClassLoader getLoader() {
        if(loader!=null)
            return loader;
        else
            return Thread.currentThread().getContextClassLoader();
    }
}
