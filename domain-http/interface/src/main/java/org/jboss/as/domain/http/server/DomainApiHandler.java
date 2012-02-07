/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.domain.http.server;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.domain.http.server.Constants.ACCEPT;
import static org.jboss.as.domain.http.server.Constants.APPLICATION_DMR_ENCODED;
import static org.jboss.as.domain.http.server.Constants.APPLICATION_JSON;
import static org.jboss.as.domain.http.server.Constants.CONTENT_DISPOSITION;
import static org.jboss.as.domain.http.server.Constants.CONTENT_TYPE;
import static org.jboss.as.domain.http.server.Constants.FORBIDDEN;
import static org.jboss.as.domain.http.server.Constants.GET;
import static org.jboss.as.domain.http.server.Constants.HOST;
import static org.jboss.as.domain.http.server.Constants.HTTP;
import static org.jboss.as.domain.http.server.Constants.HTTPS;
import static org.jboss.as.domain.http.server.Constants.INTERNAL_SERVER_ERROR;
import static org.jboss.as.domain.http.server.Constants.METHOD_NOT_ALLOWED;
import static org.jboss.as.domain.http.server.Constants.OK;
import static org.jboss.as.domain.http.server.Constants.OPTIONS;
import static org.jboss.as.domain.http.server.Constants.ORIGIN;
import static org.jboss.as.domain.http.server.Constants.POST;
import static org.jboss.as.domain.http.server.Constants.TEXT_HTML;
import static org.jboss.as.domain.http.server.Constants.UNSUPPORTED_MEDIA_TYPE;
import static org.jboss.as.domain.http.server.Constants.US_ASCII;
import static org.jboss.as.domain.http.server.Constants.UTF_8;
import static org.jboss.as.domain.http.server.HttpServerLogger.ROOT_LOGGER;
import static org.jboss.as.domain.http.server.HttpServerMessages.MESSAGES;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.domain.http.server.multipart.BoundaryDelimitedInputStream;
import org.jboss.as.domain.http.server.multipart.MimeHeaderParser;
import org.jboss.as.domain.http.server.security.SubjectAssociationHandler;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.as.domain.management.security.DomainCallbackHandler;
import org.jboss.as.network.NetworkUtils;
import org.jboss.com.sun.net.httpserver.Authenticator;
import org.jboss.com.sun.net.httpserver.Filter;
import org.jboss.com.sun.net.httpserver.Headers;
import org.jboss.com.sun.net.httpserver.HttpContext;
import org.jboss.com.sun.net.httpserver.HttpExchange;
import org.jboss.com.sun.net.httpserver.HttpServer;
import org.jboss.dmr.ModelNode;

/**
 * An embedded web server that provides a JSON over HTTP API to the domain management model.
 *
 * @author Jason T. Greene
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class DomainApiHandler implements ManagementHttpHandler {

    private static final String DOMAIN_API_CONTEXT = "/management";
    private static final String UPLOAD_REQUEST = DOMAIN_API_CONTEXT + "/add-content";

    private static Pattern MULTIPART_FD_BOUNDARY =  Pattern.compile("^multipart/form-data.*;\\s*boundary=(.*)$");
    private static Pattern DISPOSITION_FILE =  Pattern.compile("^form-data.*filename=\"?([^\"]*)?\"?.*$");

    /**
     * Represents all possible management operations that can be executed using HTTP GET
     */
    enum GetOperation {
        /*
         *  It is essential that the GET requests exposed over the HTTP interface are for read only
         *  operations that do not modify the domain model or update anything server side.
         */
        RESOURCE("read-resource"),
        ATTRIBUTE("read-attribute"),
        RESOURCE_DESCRIPTION("read-resource-description"),
        SNAPSHOTS("list-snapshots"),
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

    private final Authenticator authenticator;
    private ModelControllerClient modelController;


    DomainApiHandler(ModelControllerClient modelController, Authenticator authenticator) {
        this.modelController = modelController;
        this.authenticator = authenticator;
    }

    private void doHandle(HttpExchange http) throws IOException {
        /**
         *  Request Verification - before the request is handled a set of checks are performed for
         *  CSRF and XSS
         */

        /*
         * Completely disallow OPTIONS - if the browser suspects this is a cross site request just reject it.
         */
        final String requestMethod = http.getRequestMethod();
        if (OPTIONS.equals(requestMethod)) {
            drain(http);
            http.sendResponseHeaders(METHOD_NOT_ALLOWED, -1);

            return;
        }

        /*
         *  Origin check, if it is set the Origin header should match the Host otherwise reject the request.
         *
         *  This check is for cross site scripted GET and POST requests.
         */
        final Headers headers = http.getRequestHeaders();
        final URI request = http.getRequestURI();
        if (headers.containsKey(ORIGIN)) {
            String origin = headers.getFirst(ORIGIN);
            String host = headers.getFirst(HOST);
            String protocol = http.getHttpContext().getServer() instanceof HttpServer ? HTTP : HTTPS;
            String allowedOrigin = protocol + "://" + NetworkUtils.formatPossibleIpv6Address(host);

            // This will reject multi-origin Origin headers due to the exact match.
            if (origin.equals(allowedOrigin) == false) {
                drain(http);
                http.sendResponseHeaders(FORBIDDEN, -1);

                return;
            }
        }

        /*
         *  Cross Site Request Forgery makes use of a specially constructed form to pass in what appears to be
         *  a valid operation request - except for upload requests any inbound requests where the Content-Type
         *  is not application/json or application/dmr-encoded will be rejected.
         */

        final boolean uploadRequest = UPLOAD_REQUEST.equals(request.getPath());
        if (POST.equals(requestMethod)) {
            if (uploadRequest) {
                // This type of request doesn't need the content type check.
                processUploadRequest(http);

                return;
            }

            String contentType = extractContentType(headers.getFirst(CONTENT_TYPE));
            if (!(APPLICATION_JSON.equals(contentType) || APPLICATION_DMR_ENCODED.equals(contentType))) {
                drain(http);
                // RFC 2616: 14.11 Content-Encoding
                // If the content-coding of an entity in a request message is not
                // acceptable to the origin server, the server SHOULD respond with a
                // status code of 415 (Unsupported Media Type).
                sendResponse(http, UNSUPPORTED_MEDIA_TYPE, contentType + "\n");

                return;
            }


        }

        processRequest(http);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // make sure we send something back
        try {
            doHandle(exchange);
        } catch (Exception e) {
            sendResponse(exchange, INTERNAL_SERVER_ERROR, e.getMessage() + "\n");
        }
    }

    private void drain(HttpExchange exchange) throws IOException {
        try {
            exchange.getRequestBody().close();
        } catch (IOException e) {
            // ignore
        }
    }

    private String extractContentType(final String fullContentType) {
        int pos = fullContentType.indexOf(';');

        return pos < 0 ? fullContentType : fullContentType.substring(0, pos).trim();
    }

    /**
     * Handle a form POST deployment upload request.
     *
     * @param http The HttpExchange object that allows access to the request and response.
     * @throws IOException if an error occurs while attempting to extract the deployment from the multipart/form data.
     */
    private void processUploadRequest(final HttpExchange http) throws IOException {
        ModelNode response = null;

        try {
            SeekResult result = seekToDeployment(http);

            final ModelNode dmr = new ModelNode();
            dmr.get("operation").set("upload-deployment-stream");
            dmr.get("address").setEmptyList();
            dmr.get("input-stream-index").set(0);

            OperationBuilder operation = new OperationBuilder(dmr);
            operation.addInputStream(result.stream);
            response = modelController.execute(operation.build());
            drain(http.getRequestBody());
        } catch (Throwable t) {
            // TODO Consider draining input stream
            ROOT_LOGGER.uploadError(t);
            sendError(http,false,t);
            return;
        }

        // TODO Determine what format the response should be in for a deployment upload request.
        writeResponse(http, false, false, response, OK, false, TEXT_HTML);
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

        boolean isGet = GET.equals(requestMethod);
        if (!isGet && !POST.equals(requestMethod)) {
            http.sendResponseHeaders(METHOD_NOT_ALLOWED, -1);

            return;
        }

        ModelNode dmr;
        ModelNode response;
        int status = OK;

        Headers requestHeaders = http.getRequestHeaders();
        boolean encode = APPLICATION_DMR_ENCODED.equals(requestHeaders.getFirst(ACCEPT))
                || APPLICATION_DMR_ENCODED.equals(requestHeaders.getFirst(CONTENT_TYPE));

        try {
            dmr = isGet ? convertGetRequest(request) : convertPostRequest(http.getRequestBody(), encode);
        } catch (Exception iae) {
            ROOT_LOGGER.debugf("Unable to construct ModelNode '%s'", iae.getMessage());
            sendError(http,isGet,iae);
            return;
        }

        try {
            response = modelController.execute(new OperationBuilder(dmr).build());
        } catch (Throwable t) {
            ROOT_LOGGER.modelRequestError(t);
            sendError(http,isGet,t);
            return;
        }

        if (response.hasDefined(OUTCOME) && FAILED.equals(response.get(OUTCOME).asString())) {
            status = INTERNAL_SERVER_ERROR;
        }

        boolean pretty = dmr.hasDefined("json.pretty") && dmr.get("json.pretty").asBoolean();
        writeResponse(http, isGet, pretty, response, status, encode);
    }

    private void sendError(final HttpExchange http, boolean isGet, Throwable t) throws IOException {
        ModelNode response = new ModelNode();
        response.set(t.getMessage());
        writeResponse(http, isGet, true, response, INTERNAL_SERVER_ERROR, false);
    }

    private void sendResponse(final HttpExchange exchange, final int responseCode, final String body) throws IOException {
        exchange.sendResponseHeaders(responseCode, 0);
        final PrintWriter out = new PrintWriter(exchange.getResponseBody());
        try {
            out.print(body);
            out.flush();
        } finally {
            safeClose(out);
        }
    }

     private void writeResponse(final HttpExchange http, boolean isGet, boolean pretty, ModelNode response, int status,
            boolean encode) throws IOException {
         String contentType = encode ? APPLICATION_DMR_ENCODED : APPLICATION_JSON;
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
        responseHeaders.add(CONTENT_TYPE, contentType);
        http.sendResponseHeaders(status, 0);

        final OutputStream out = http.getResponseBody();
        final PrintWriter print = new PrintWriter(out);

        // GET (read) operations will never have a compensating update, and the status is already
        // available via the http response status code, so unwrap them.
        if (isGet && status == OK)
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
        final String type = http.getRequestHeaders().getFirst(CONTENT_TYPE);
        if (type == null)
            throw MESSAGES.invalidContentType();

        Matcher matcher = MULTIPART_FD_BOUNDARY.matcher(type);
        if (!matcher.matches())
            throw MESSAGES.invalidContentType(type);

        final String boundary = "--" + matcher.group(1);

        final BoundaryDelimitedInputStream stream = new BoundaryDelimitedInputStream(http.getRequestBody(), boundary.getBytes("US-ASCII"));

        // Eat preamble
        byte[] ignore = new byte[1024];
        while (stream.read(ignore) != -1) {}

        // From here on out a boundary is prefixed with a CRLF that should be skipped
        stream.setBoundary(("\r\n" + boundary).getBytes(US_ASCII));

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

        throw MESSAGES.invalidDeployment();
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
                    throw MESSAGES.invalidOperation(e, value);
                }
            }

            dmr.get(entry.getKey()).set(value);
        }

        // This will now only occur if no operation at all was specified on the incoming request.
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
            return URLDecoder.decode(string, UTF_8);
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

    public void start(HttpServer httpServer, SecurityRealm securityRealm) {
        // The SubjectAssociationHandler wraps all calls to this HttpHandler to ensure the Subject has been associated
        // with the security context.
        HttpContext context = httpServer.createContext(DOMAIN_API_CONTEXT, new SubjectAssociationHandler(this));
        // Once there is a trust store we can no longer rely on users being defined so skip
        // any redirects.
        if (authenticator != null) {
            context.setAuthenticator(authenticator);
            List<Filter> filters = context.getFilters();
            if (securityRealm.hasTrustStore() == false) {
                DomainCallbackHandler callbackHandler = securityRealm.getCallbackHandler();
                filters.add(new RealmReadinessFilter(callbackHandler, ErrorHandler.getRealmRedirect()));
            }
        }
    }

    public void stop(HttpServer httpServer) {
        httpServer.removeContext(DOMAIN_API_CONTEXT);
        modelController = null;
    }

}


