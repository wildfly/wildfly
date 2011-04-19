package org.jboss.as.domain.http.server;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.domain.http.server.multipart.BoundaryDelimitedInputStream;
import org.jboss.as.domain.http.server.multipart.MimeHeaderParser;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * An embedded web server that provides a JSON over HTTP API to the domain management model.
 *
 * @author Jason T. Greene
 */
public class DomainHttpServer implements HttpHandler {

    private static final int INTERNAL_ERROR = 500;
    private static final String DOMAIN_API_CONTEXT = "/domain-api";
    private static final String UPLOAD_REQUEST = DOMAIN_API_CONTEXT + "/add-content";
    private static final String POST_REQUEST_METHOD = "POST";
    private static final String GET_REQUEST_METHOD = "GET";
    private static final String CONTENT_DISPOSITION = "Content-Disposition";
    private static final String UPLOAD_TEMP_DIRECTORY = "uploads";

    private static Pattern MULTIPART_FD_BOUNDARY =  Pattern.compile("^multipart/form-data.*;\\s*boundary=(.*)$");
    private static Pattern DISPOSITION_FILE =  Pattern.compile("^form-data.*filename=\"?([^\"]*)?\"?.*$");

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

    DomainHttpServer(HttpServer server, ModelController modelController, File serverTempDir) {
        this.server = server;
        this.modelController = modelController;
    }

    @Override
    public void handle(HttpExchange http) throws IOException {
        final URI request = http.getRequestURI();
        final String requestMethod = http.getRequestMethod();

        /*
         * Detect the file upload request. If it is not present, submit the incoming request to the normal handler.
         */
        if (POST_REQUEST_METHOD.equals(requestMethod) && UPLOAD_REQUEST.equals(request.getPath())) {
            processUploadRequest(http);
        } else {
            processRequest(http);
        }
    }

    /**
     * Handle a form POST deployment upload request.
     *
     * @param http The HttpExchange object that allows access to the request and response.
     * @throws IOException if an error occurs while attempting to extract the deployment from the multipart/form data.
     */
    private void processUploadRequest(final HttpExchange http) throws IOException {
        File tempUploadFile = null;
        ModelNode response = null;

        try {
            SeekResult result = seekToDeployment(http);

            final ModelNode dmr = new ModelNode();
            dmr.get("operation").set("upload-deployment-stream");
            dmr.get("address").setEmptyList();
            dmr.get("input-stream-index").set(0);

            OperationBuilder operation = OperationBuilder.Factory.create(dmr);
            operation.addInputStream(result.stream);
            response = modelController.execute(operation.build());
            drain(http.getRequestBody());
        } catch (Throwable t) {
            // TODO Consider draining input stream
            log.error("Unexpected error executing deployment upload request", t);
            http.sendResponseHeaders(INTERNAL_ERROR, -1);
            return;
        }

        // TODO Determine what format the response should be in for a deployment upload request.
        writeResponse(http, false, false, response, 200, false);
    }

    /**
     * Handles a operation request via HTTP.
     *
     * @param http The HttpExchange object that allows access to the request and response.
     * @throws IOException if an error occurs while attempting to process the request.
     */
    private void processRequest(final HttpExchange http) throws IOException {
        final URI request = http.getRequestURI();
        final String requestMethod = http.getRequestMethod();

        boolean isGet = GET_REQUEST_METHOD.equals(requestMethod);
        if (!isGet && !POST_REQUEST_METHOD.equals(requestMethod)) {
            http.sendResponseHeaders(405, -1);
            return;
        }

        ModelNode dmr = null;
        ModelNode response;
        int status = 200;

        Headers requestHeaders = http.getRequestHeaders();
        boolean encode = "application/dmr-encoded".equals(requestHeaders.getFirst("Accept"))
                || "application/dmr-encoded".equals(requestHeaders.getFirst("Content-Type"));

        try {
            dmr = isGet ? convertGetRequest(request) : convertPostRequest(http.getRequestBody(), encode);
            response = modelController.execute(OperationBuilder.Factory.create(dmr).build());
        } catch (Throwable t) {
            log.error("Unexpected error executing model request", t);
            http.sendResponseHeaders(INTERNAL_ERROR, -1);
            return;
        }

        if (response.hasDefined(OUTCOME) && FAILED.equals(response.get(OUTCOME).asString())) {
            status = 500;
        }

        boolean pretty = dmr.hasDefined("json.pretty") && dmr.get("json.pretty").asBoolean();
        writeResponse(http, isGet, pretty, response, status, encode, "text/html");
    }

     private void writeResponse(final HttpExchange http, boolean isGet, boolean pretty, ModelNode response, int status,
            boolean encode) throws IOException {
         String contentType = encode ? "application/dmr-encoded" : "application/json";
         writeResponse(http, isGet, pretty, response, status, encode, contentType);
     }
    /**
     * Writes the HTTP response to the output stream.
     *
     * @param http The HttpExchange object that allows access to the request and response.
     * @param isGet Flag indicating whether or not the request was a GET request or POST request.
     * @param pretty Flag indicating whether or not the output, if JSON, should be pretty printed or not.
     * @param response The DMR response from the operation.
     * @param status The HTTP status code to be included in the response.
     * @param encode Flag indicating whether or not to Base64 encode the response payload.
     * @throws IOException if an error occurs while attempting to generate the HTTP response.
     */
    private void writeResponse(final HttpExchange http, boolean isGet, boolean pretty, ModelNode response, int status,
            boolean encode, String contentType) throws IOException {
        final Headers responseHeaders = http.getResponseHeaders();
        responseHeaders.add("Content-Type", contentType);
        responseHeaders.add("Access-Control-Allow-Origin", "*");
        http.sendResponseHeaders(status, 0);

        final OutputStream out = http.getResponseBody();
        final PrintWriter print = new PrintWriter(out);

        // GET (read) operations will never have a compensating update, and the status is already
        // available via the http response status code, so unwrap them.
        if (isGet && status == 200)
            response = response.get("result");

        try {
            if (encode) {
                response.writeBase64(out);
            } else {
                response.writeJSONString(print, !pretty);
            }
        } finally {
            print.flush();
            out.flush();
            safeClose(print);
            safeClose(out);
        }
    }

    private static final class SeekResult {
        BoundaryDelimitedInputStream stream;
        String fileName;
    }

    /**
     * Extracts the body content contained in a POST request.
     *
     * @param http The <code>HttpExchange</code> object containing POST request data.
     * @return a result containing the stream and the file name reported by the client
     * @throws IOException if an error occurs while attempting to extract the POST request data.
     */
    private SeekResult seekToDeployment(final HttpExchange http) throws IOException {
        final String type = http.getRequestHeaders().getFirst("Content-Type");
        if (type == null)
            throw new IllegalArgumentException("No content type provided");

        Matcher matcher = MULTIPART_FD_BOUNDARY.matcher(type);
        if (!matcher.matches())
            throw new IllegalArgumentException("Invalid content type provided: " + type);

        final String boundary = "--" + matcher.group(1);

        final BoundaryDelimitedInputStream stream = new BoundaryDelimitedInputStream(http.getRequestBody(), boundary.getBytes("US-ASCII"));

        // Eat preamble
        byte[] ignore = new byte[1024];
        while (stream.read(ignore) != -1) {}

        // From here on out a boundary is prefixed with a CRLF that should be skipped
        stream.setBoundary(("\r\n" + boundary).getBytes("US-ASCII"));

        while (!stream.isOuterStreamClosed()) {
            // purposefully send the trailing CRLF to headers so that a headerless body can be detected
            MimeHeaderParser.ParseResult result = MimeHeaderParser.parseHeaders(stream);
            if (result.eof()) continue; // Skip content-less part

            Headers partHeaders = result.headers();
            String disposition = partHeaders.getFirst(CONTENT_DISPOSITION);
            if (disposition != null) {
                matcher = DISPOSITION_FILE.matcher(disposition);
                if (matcher.matches()) {
                    SeekResult seek = new SeekResult();
                    seek.fileName = matcher.group(1);
                    seek.stream = stream;

                    return seek;
                }
            }

            while (stream.read(ignore) != -1) {}
        }

        throw new IllegalArgumentException("Request did not contain a deployment");
    }

    private void drain(InputStream stream) {
        try {
            byte[] ignore = new byte[1024];
            while (stream.read(ignore) != -1) {}
        } catch (Throwable eat) {
        }
    }

    private void safeClose(Closeable close) {
        try {
            close.close();
        } catch (Throwable eat) {
        }
    }

    private ModelNode convertPostRequest(InputStream stream, boolean encode) throws IOException {
        return encode ? ModelNode.fromBase64(stream) : ModelNode.fromJSONStream(stream);
    }

    private ModelNode convertGetRequest(URI request) {
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
        for (int i = 1; i < pathSegments.size() - 1; i += 2) {
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

    public static DomainHttpServer create(InetSocketAddress socket, int backlog, ModelController modelController,
            Executor executor, File serverTempDir) throws IOException {
        HttpServer server = HttpServer.create(socket, backlog);
        DomainHttpServer me = new DomainHttpServer(server, modelController, serverTempDir);
        server.createContext(DOMAIN_API_CONTEXT, me);
        server.createContext(ConsoleHandler.CONTEXT, new ConsoleHandler());
        server.setExecutor(executor);

        return new DomainHttpServer(server, modelController, serverTempDir);
    }
}