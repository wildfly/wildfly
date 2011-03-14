package org.jboss.as.domain.http.server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

/**
 * @author Heiko Braun
 * @date 3/14/11
 */
public class ConsoleHandler implements HttpHandler {

    public static final String CONTEXT = "/console";

    private ClassLoader loader = null;

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

        if(resource.equals("")) respond404(http);

        // load resource
        InputStream inputStream = getLoader().getResourceAsStream(resource);
        if(inputStream!=null)
        {
            Headers responseHeaders = http.getResponseHeaders();
            responseHeaders.set("Content-Type", "text/plain");// TODO
            http.sendResponseHeaders(200, 0);

            OutputStream outputStream = http.getResponseBody();

            int nextChar;
            while ( ( nextChar = inputStream.read() ) != -1  )
            {
                outputStream.write(nextChar);
            }
            outputStream.close();
        }
        else
        {
            respond404(http);
        }

    }

    private void respond404(HttpExchange http) throws IOException {
        http.sendResponseHeaders(404, 0);
    }

    private ClassLoader getLoader()
    {
        if(loader!=null)
            return loader;
        else
            return Thread.currentThread().getContextClassLoader();
    }
}
