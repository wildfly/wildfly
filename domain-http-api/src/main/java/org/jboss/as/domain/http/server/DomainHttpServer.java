package org.jboss.as.domain.http.server;


import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executor;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * An embedded web server that provides a JSON over HTTP API to the
 * domain management model.
 *
 * @author Jason T. Greene
 */
public class DomainHttpServer implements HttpHandler {

    private static final String DOMAIN_API_CONTEXT = "/domain-api";
    private static final Logger log = Logger.getLogger("org.jboss.as.domain.http.api");


    /**
     * Represents all possible management operations that can be executed using HTTP GET
     */
    enum GetOperation {
        RESOURCE("read-resource"),
        ATTRIBUTE("read-attribute"),
        RESOURCE_DESCRIPTION("read-resource-description"),
        OPERATION_DESCRIPTION("read-operation-description"),
        OPERATION_NAMES("read-operation-names");

        private String realOperation;

        GetOperation(String realOperation) {
            this.realOperation = realOperation;
        }

        public String realOperation() {
            return realOperation;
        }
    }

    private HttpServer server;
    private ModelController modelController;

    DomainHttpServer(HttpServer server, ModelController modelController) {
        this.server = server;
        this.modelController = modelController;
    }

    @Override
    public void handle(HttpExchange http) throws IOException {
        URI request = http.getRequestURI();

        if (! http.getRequestMethod().equals("GET")) {
            http.sendResponseHeaders(405, -1);
            return;
        }

        ModelNode dmr = null;
        ModelNode response;
        int status = 200;

        Headers requestHeaders = http.getRequestHeaders();
        boolean encode = "application/dmr-encoded".equals(requestHeaders.getFirst("Accept")) ||
                         "application/dmr-encoded".equals(requestHeaders.getFirst("Content-Type"));

        try {
            dmr = convertToRequest(request);
            response = modelController.execute(dmr);
        } catch (OperationFailedException e) {
            response = e.getFailureDescription();
            status = 500;
        } catch (Throwable t) {
            log.error("Unexpected error executing model request", t);
            http.sendResponseHeaders(500, -1);
            return;
        }

        Headers responseHeaders = http.getResponseHeaders();
        responseHeaders.add("Content-Type", encode ? "application/dmr-encoded" : "application/json");
        responseHeaders.add("Access-Control-Allow-Origin", "*");
        http.sendResponseHeaders(status, 0);

        OutputStream out = http.getResponseBody();
        PrintStream print = new PrintStream(out);

        // Successful responses are wrapped in a result field to allow for an outcome,
        // which is already represented in the HTTP status, unwrap it.
        if (status == 200)
            response = response.get("result");

        try {
            if (encode) {
                response.writeBase64(out);
            } else {
                // TODO: Change DMR to stream JSON output
                print.println(response.toJSONString(false));
            }
        } finally {
            out.flush();
            print.flush();
            print.close();
        }
    }

    private ModelNode convertToRequest(URI request) {
        ArrayList<String> pathSegments = decodePath(request.getRawPath());
        Map<String, String> queryParameters = decodeQuery(request.getRawQuery());

        GetOperation operation = null;
        ModelNode dmr = new ModelNode();
        for (Entry<String, String> entry : queryParameters.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if ("operation".equals(key)) {
                try {
                    operation = GetOperation.valueOf(value.toUpperCase().replace('-', '_'));
                    value = operation.realOperation();
                } catch (Exception e) {
                    // Unknown
                    continue;
                }
            }

            dmr.get(entry.getKey()).set(value);
        }

        if (operation == null) {
            operation = GetOperation.RESOURCE;
            dmr.get("operation").set(operation.realOperation);
        }

        if (operation == GetOperation.RESOURCE && !dmr.has("recursive"))
            dmr.get("recursive").set(false);

        ModelNode list = dmr.get("address").setEmptyList();
        for (int i = 1; i < pathSegments.size() - 1; i+=2) {
            list.add(pathSegments.get(i), pathSegments.get(i + 1));
        }
        return dmr;
    }

    private ArrayList<String> decodePath(String path) {
        if (path == null)
            throw new IllegalArgumentException();

        int i = path.charAt(0) == '/' ? 1 : 0;

        ArrayList<String> segments = new ArrayList<String>();

        do {
            int j = path.indexOf('/', i);
            if (j == -1)
                j = path.length();

            segments.add(unescape(path.substring(i, j)));
            i = j + 1;
        } while (i < path.length());

        return segments;
    }

    private String unescape(String string) {
        try {
            // URLDecoder could be way more efficient, replace it one day
            return URLDecoder.decode(string, "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private Map<String, String> decodeQuery(String query) {
        if (query == null || query.isEmpty())
            return Collections.emptyMap();

        int i = 0;
        Map<String, String> parameters = new HashMap<String, String>();


        do {
            int j = query.indexOf('&', i);
            if (j == -1)
                j = query.length();

            String pair = query.substring(i, j);
            int k = pair.indexOf('=');

            String key;
            String value;
            if (k == -1) {
                key = unescape(pair);
                value = "true";
            } else {
                key = unescape(pair.substring(0, k));
                value = unescape(pair.substring(k + 1, pair.length()));
            }

            parameters.put(key, value);

            i = j + 1;
        } while (i < query.length());

        return parameters;
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
        modelController = null;
    }

    public static DomainHttpServer create(InetSocketAddress socket, int backlog, ModelController modelController, Executor executor) throws IOException {
        HttpServer server = HttpServer.create(socket, backlog);
        DomainHttpServer me = new DomainHttpServer(server, modelController);
        server.createContext(DOMAIN_API_CONTEXT, me);
        server.setExecutor(executor);

        return new DomainHttpServer(server, modelController);
    }

}
