package org.jboss.as.domain.http.server;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
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

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.domain.http.server.multipart.BoundaryDelimitedInputStream;
import org.jboss.as.domain.http.server.multipart.MultipartHeader;
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

    private static final int HTTP_INTERNAL_SERVER_ERROR_STATUS = 500;
    private static final String DOMAIN_API_CONTEXT = "/domain-api";
    private static final String UPLOAD_REQUEST = DOMAIN_API_CONTEXT + "/add-content";
    private static final String POST_REQUEST_METHOD = "POST";
    private static final String GET_REQUEST_METHOD = "GET";
    private static final String MULTIPART_FORM_DATA_CONTENT_TYPE = "application/octet-stream";
    private static final String UPLOAD_TEMP_DIRECTORY = "uploads";
    private static final int UPLOAD_BUFFER_SIZE = 1024;
    private final byte[] POST_BOUNDARY = new byte[] { 0xd, 0xa, 0x2d, 0x2d };
    private final byte[] POST_HEADER_BOUNDARY = new byte[] { 0xd, 0xa, 0xd, 0xa };
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
    private final File serverTempDir;

    DomainHttpServer(HttpServer server, ModelController modelController, File serverTempDir) {
        this.server = server;
        this.modelController = modelController;
        this.serverTempDir = new File(serverTempDir, UPLOAD_TEMP_DIRECTORY);

        // Create the upload sub-directory under the server temporary directory.
        if (!this.serverTempDir.exists()) {
            this.serverTempDir.mkdir();
        }
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
        int status = 200;

        try {
             tempUploadFile = extractPostContent(http);

             /*
             * TODO Change to use upload-deployment-stream operation. This would involve wrapping the input stream containing
             * the request body in a multipart decoder stream that would read only the deployment contained in the
             * multipart/form data.
             */
             final ModelNode dmr = new ModelNode();
             dmr.get("operation").set("upload-deployment-url");
             dmr.get("address").setEmptyList();
             dmr.get("url").set(tempUploadFile.toURI().toURL().toString());

             response = modelController.execute(OperationBuilder.Factory.create(dmr).build());
        } catch (Throwable t) {
            log.error("Unexpected error executing deployment upload request", t);
            http.sendResponseHeaders(HTTP_INTERNAL_SERVER_ERROR_STATUS, -1);
            return;
        } finally {
            if (tempUploadFile != null && tempUploadFile.exists()) {
                tempUploadFile.delete();
            }
        }

        // TODO Determine what format the response should be in for a deployment upload request.
        writeResponse(http, false, false, response, status, false);
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
            http.sendResponseHeaders(HTTP_INTERNAL_SERVER_ERROR_STATUS, -1);
            return;
        }

        if (response.hasDefined(OUTCOME) && FAILED.equals(response.get(OUTCOME).asString())) {
            status = 500;
        }

        boolean pretty = dmr.hasDefined("json.pretty") && dmr.get("json.pretty").asBoolean();
        writeResponse(http, isGet, pretty, response, status, encode);
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
            boolean encode) throws IOException {
        final Headers responseHeaders = http.getResponseHeaders();
        responseHeaders.add("Content-Type", encode ? "application/dmr-encoded" : "application/json");
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

    /**
     * Extracts the body content contained in a POST request.
     *
     * @param http The <code>HttpExchange</code> object containing POST request data.
     * @return The temporary file containing the extracted POST data.
     * @throws IOException if an error occurs while attempting to extract the POST request data.
     */
    private File extractPostContent(final HttpExchange http) throws IOException {
        final BoundaryDelimitedInputStream iStream = new BoundaryDelimitedInputStream(http.getRequestBody(), POST_BOUNDARY);
        final File tempUploadFile = File.createTempFile("upload", ".tmp", serverTempDir);
        final BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(tempUploadFile));

        final byte[] buffer = new byte[UPLOAD_BUFFER_SIZE];
        boolean isDeploymentPart = false;

        try {
            // Read from the stream until the deployment is found in the POST data.
            while (!isDeploymentPart && !iStream.isOuterStreamClosed()) {
                int numRead = 0;

                // Read the POST section header from the inner stream.
                final MultipartHeader header = readHeader(iStream);

                if(header != null) {
                    // Determine if the current section is a deployment file upload.
                    isDeploymentPart = MULTIPART_FORM_DATA_CONTENT_TYPE.equals(header.getContentType());

                    /*
                     *  Read the body following the header.  If it is the deployment,
                     *  write it to the temporary file.  Otherwise, discard the data.
                     */
                    while (numRead != -1) {
                        numRead = iStream.read(buffer);
                        if (numRead > 0 && isDeploymentPart) {
                            bos.write(buffer, 0, numRead);
                        }
                    }
                }
            }
        } finally {
            /*
             * Files produced by this method that are left hanging around by an exception will be cleaned up when the HTTP
             * server stops. Therefore, we don't need any special handling here to clean-up due to an exception. Simply close
             * the stream like normal.
             */
            bos.flush();
            safeClose(bos);
        }

        return tempUploadFile;
    }

    /**
     * Reads and returns the multipart headers from the source stream.
     *
     * @param boundaryStream A <code>BoundaryDelimitedInputStream</code> that wraps a POST request.
     * @return A <code>MultipartHeader</code> object wrapping the extracted header or <code>null</code> if the stream is closed.
     * @throws IOException if an error occurs while attempting to read the headers from the current inside stream.
     */
    private MultipartHeader readHeader(final BoundaryDelimitedInputStream boundaryStream) throws IOException {
        MultipartHeader currentHeader = null;
        if (!boundaryStream.isOuterStreamClosed()) {
            int separatorCounter = 0;
            byte[] separatorBuffer = new byte[POST_HEADER_BOUNDARY.length];
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();

            try {
                // Read the header until the post header separator sequence is found.
                while (separatorCounter < POST_HEADER_BOUNDARY.length) {
                    int current = boundaryStream.read();
                    if (current == POST_HEADER_BOUNDARY[separatorCounter]) {
                        separatorBuffer[separatorCounter] = (byte) current;
                        separatorCounter++;
                    } else {
                        if (separatorCounter > 0) {
                            bos.write(separatorBuffer, 0, separatorCounter);
                        }
                        bos.write(current);
                        separatorCounter = 0;
                    }
                }
            } finally {
                bos.flush();
                bos.close();
            }

            // Create the header from the read data
            currentHeader = new MultipartHeader(bos.toByteArray());
        }

        return currentHeader;
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
        if (serverTempDir.exists()) {
            // Clean up any files that accidentally got left behind
            for (File file : serverTempDir.listFiles()) {
                file.delete();
            }
            serverTempDir.delete();
        }
    }

    public static DomainHttpServer create(InetSocketAddress socket, int backlog, ModelController modelController,
            Executor executor, File serverTempDir) throws IOException {
        HttpServer server = HttpServer.create(socket, backlog);
        DomainHttpServer me = new DomainHttpServer(server, modelController, serverTempDir);
        server.createContext(DOMAIN_API_CONTEXT, me);
        server.setExecutor(executor);

        return new DomainHttpServer(server, modelController, serverTempDir);
    }
}