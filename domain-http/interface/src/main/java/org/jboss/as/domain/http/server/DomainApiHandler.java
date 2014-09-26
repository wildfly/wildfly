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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS_MECHANISM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.domain.http.server.Constants.ACCEPT;
import static org.jboss.as.domain.http.server.Constants.APPLICATION_DMR_ENCODED;
import static org.jboss.as.domain.http.server.Constants.APPLICATION_JSON;
import static org.jboss.as.domain.http.server.Constants.BAD_REQUEST;
import static org.jboss.as.domain.http.server.Constants.CONTENT_DISPOSITION;
import static org.jboss.as.domain.http.server.Constants.CONTENT_TYPE;
import static org.jboss.as.domain.http.server.Constants.FORBIDDEN;
import static org.jboss.as.domain.http.server.Constants.GET;
import static org.jboss.as.domain.http.server.Constants.INTERNAL_SERVER_ERROR;
import static org.jboss.as.domain.http.server.Constants.METHOD_NOT_ALLOWED;
import static org.jboss.as.domain.http.server.Constants.OK;
import static org.jboss.as.domain.http.server.Constants.POST;
import static org.jboss.as.domain.http.server.Constants.TEXT_HTML;
import static org.jboss.as.domain.http.server.Constants.UNSUPPORTED_MEDIA_TYPE;
import static org.jboss.as.domain.http.server.Constants.US_ASCII;
import static org.jboss.as.domain.http.server.Constants.UTF_8;
import static org.jboss.as.domain.http.server.DomainUtil.safeClose;
import static org.jboss.as.domain.http.server.DomainUtil.writeResponse;
import static org.jboss.as.domain.http.server.HttpServerLogger.ROOT_LOGGER;
import static org.jboss.as.domain.http.server.HttpServerMessages.MESSAGES;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.as.controller.ControlledProcessStateService;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.client.OperationResponse;
import org.jboss.as.core.security.AccessMechanism;
import org.jboss.as.domain.http.server.multipart.BoundaryDelimitedInputStream;
import org.jboss.as.domain.http.server.multipart.MimeHeaderParser;
import org.jboss.as.domain.management.AuthenticationMechanism;
import org.jboss.as.domain.management.SecurityRealm;
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
    private static final String ADD_CONTENT_REQUEST = DOMAIN_API_CONTEXT + "/add-content";

    private static Pattern MULTIPART_FD_BOUNDARY =  Pattern.compile("^multipart/form-data.*;\\s*boundary=(.*)$");
    private static Pattern DISPOSITION_FILE =  Pattern.compile("^form-data.*filename=\"?([^\"]*)?\"?.*$");
    private static final String JSON_PRETTY = "json.pretty";
    private static final String USE_STREAM_AS_RESPONSE = "useStreamAsResponse";
    private static final String USE_STREAM_AS_RESPONSE_HEADER = "org.wildfly.useStreamAsResponse";

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
    private final ControlledProcessStateService controlledProcessStateService;
    private ModelController modelController;


    DomainApiHandler(final ModelController modelController, final Authenticator authenticator,
                     final ControlledProcessStateService controlledProcessStateService) {
        this.modelController = modelController;
        this.authenticator = authenticator;
        this.controlledProcessStateService = controlledProcessStateService;
    }

    private void doHandle(HttpExchange http) throws IOException {

        /*
         *  Cross Site Request Forgery makes use of a specially constructed form to pass in what appears to be
         *  a valid operation request - except for upload requests any inbound requests where the Content-Type
         *  is not application/json or application/dmr-encoded will be rejected.
         */

        final String requestMethod = http.getRequestMethod();
        final URI request = http.getRequestURI();
        final boolean uploadRequest = ADD_CONTENT_REQUEST.equals(request.getPath());
        if (POST.equals(requestMethod)) {
            if (uploadRequest) {
                // This type of request doesn't need the content type check.
                processUploadRequest(http);
                return;
            }
            final Headers headers = http.getRequestHeaders();
            final String contentType = extractContentType(headers.getFirst(CONTENT_TYPE));
            if (!(APPLICATION_JSON.equals(contentType) || APPLICATION_DMR_ENCODED.equals(contentType))) {
                drain(http);
                // RFC 2616: 14.11 Content-Encoding
                // If the content-coding of an entity in a request message is not
                // acceptable to the origin server, the server SHOULD respond with a
                // status code of 415 (Unsupported Media Type).
                ROOT_LOGGER.debug("Request rejected due to unsupported media type - should be one of (application/json,application/dmr-encoded).");
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
            dmr.get(OPERATION_HEADERS, ACCESS_MECHANISM).set(AccessMechanism.HTTP.toString());
            dmr.get("operation").set("upload-deployment-stream");
            dmr.get("address").setEmptyList();
            dmr.get("input-stream-index").set(0);

            OperationBuilder operation = new OperationBuilder(dmr);
            operation.addInputStream(result.stream);
            response = modelController.execute(dmr, OperationMessageHandler.DISCARD, ModelController.OperationTransactionControl.COMMIT, operation.build());
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

        final boolean isGet = GET.equals(requestMethod);
        if (!isGet && !POST.equals(requestMethod)) {
            ROOT_LOGGER.debug("Request rejected as method not one of (GET,POST).");
            http.sendResponseHeaders(METHOD_NOT_ALLOWED, -1);
            return;
        }

        final ModelNode dmr;
        final Map<String, String> queryParameters;

        Headers requestHeaders = http.getRequestHeaders();
        final boolean encode = APPLICATION_DMR_ENCODED.equals(requestHeaders.getFirst(ACCEPT))
                || APPLICATION_DMR_ENCODED.equals(requestHeaders.getFirst(CONTENT_TYPE));
        try {
            queryParameters = decodeQuery(request.getRawQuery());
            dmr = isGet ? convertGetRequest(request, queryParameters) : convertPostRequest(http.getRequestBody(), encode);
        } catch (Exception iae) {
            ROOT_LOGGER.debugf("Unable to construct ModelNode '%s'", iae.getMessage());
            sendError(http,isGet,iae);
            return;
        }

        final int streamIndex = getStreamIndex(requestHeaders, queryParameters);
        final boolean pretty = dmr.hasDefined(JSON_PRETTY) && dmr.get(JSON_PRETTY).asBoolean();
        final ResponseCallback callback = new ResponseCallback() {
            @Override
            void doSendResponse(final OperationResponse response) {
                DomainApiHandler.this.sendResponse(http, response, isGet, pretty, encode, streamIndex);
            }
        };

        final boolean sendPreparedResponse = sendPreparedResponse(dmr);
        final ModelController.OperationTransactionControl control = sendPreparedResponse ? new ModelController.OperationTransactionControl() {
                @Override
                public void operationPrepared(final ModelController.OperationTransaction transaction, final ModelNode result) {
                        transaction.commit();
                        // Fix prepared result
                        result.get(OUTCOME).set(SUCCESS);
                        result.get(RESULT);
                        callback.sendResponse(OperationResponse.Factory.createSimple(result));
                    }
            } : ModelController.OperationTransactionControl.COMMIT;

        OperationResponse response;
        try {
            dmr.get(OPERATION_HEADERS, ACCESS_MECHANISM).set(AccessMechanism.HTTP.toString());
            response = modelController.execute(Operation.Factory.create(dmr), OperationMessageHandler.DISCARD, control);
        } catch (Throwable t) {
            ROOT_LOGGER.modelRequestError(t);
            sendError(http,isGet,t);
            return;
        }

        if (!sendPreparedResponse) {
            sendResponse(http, response, isGet, pretty, encode, streamIndex);
        }
    }

    private void sendResponse(HttpExchange http, OperationResponse response, boolean isGet, boolean pretty, boolean encode, int streamIndex) {
        try {
            ModelNode responseNode = response.getResponseNode();
            if (responseNode.hasDefined(OUTCOME) && FAILED.equals(responseNode.get(OUTCOME).asString())) {
                int status = getErrorResponseCode(responseNode.asString());
                writeResponse(http, isGet, pretty, responseNode, status, encode);
                return;
            }

            if (streamIndex < 0) {
                writeResponse(http, isGet, pretty, responseNode, OK, encode);
            } else {
                List<OperationResponse.StreamEntry> streamEntries = response.getInputStreams();
                if (streamIndex >= streamEntries.size()) {
                    // invalid index
                    ModelNode error = new ModelNode(HttpServerMessages.MESSAGES.invalidUseStreamAsResponseIndex(streamIndex, streamEntries.size()));
                    writeResponse(http, isGet, true, error, BAD_REQUEST, false);
                } else {
                    writeResponse(http, response, streamIndex);
                }
            }
        } catch (IOException e) {
            ROOT_LOGGER.responseFailed(e);
        } finally {
            safeClose(response);
        }

    }

    private void sendError(final HttpExchange http, boolean isGet, Throwable t) throws IOException {
        sendError(http, isGet, t.getMessage());
    }

    private void sendError(final HttpExchange http, boolean isGet, String message) throws IOException {
        ModelNode response = new ModelNode(message);
        writeResponse(http, isGet, true, response, INTERNAL_SERVER_ERROR, false);
    }

    private static int getStreamIndex(final Headers requestHeaders, final Map<String, String> queryParams) {
        // First check for an HTTP header
        int result = getStreamIndex(requestHeaders.get(USE_STREAM_AS_RESPONSE_HEADER));
        if (result == -1) {
            // Nope. Now check for a URL query parameter
            String value = queryParams.get(USE_STREAM_AS_RESPONSE);
            result = getStreamIndex(value == null ? null : Collections.singletonList(value));
        }
        return result;
    }

    private static int getStreamIndex(List<String> holder) {
        int result;
        if (holder != null) {
            String val;
            if (holder.size() > 0 && (val = holder.get(0)).length() > 0) {
                if ("true".equalsIgnoreCase(val))  {
                    // 'true' means param with no value
                    result = 0;
                } else {
                    result = Integer.parseInt(val);
                }
            } else {
                result = 0;
            }
        } else {
            result = -1;
        }
        return result;
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

    private static final class SeekResult {
        BoundaryDelimitedInputStream stream;
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

    private ModelNode convertPostRequest(InputStream stream, boolean encode) throws IOException {
        return encode ? ModelNode.fromBase64(stream) : ModelNode.fromJSONStream(stream);
    }

    private ModelNode convertGetRequest(URI request, Map<String, String> queryParameters) {
        ArrayList<String> pathSegments = decodePath(request.getRawPath());

        GetOperation operation = null;
        ModelNode dmr = new ModelNode();
        for (Entry<String, String> entry : queryParameters.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            ModelNode valueNode;
            if ("operation".equals(key)) {
                try {
                    operation = GetOperation.valueOf(value.toUpperCase(Locale.ENGLISH).replace('-', '_'));
                    value = operation.realOperation();
                    valueNode = dmr.get(key);
                } catch (Exception e) {
                    throw MESSAGES.invalidOperation(e, value);
                }
            } else if (key.startsWith("operation-header-")) {
                String header = key.substring("operation-header-".length());
                valueNode = dmr.get(OPERATION_HEADERS, header);
            } else {
                valueNode = dmr.get(key);
            }

            valueNode.set(value);
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

    private static String unescape(String string) {
        try {
            // URLDecoder could be way more efficient, replace it one day
            return URLDecoder.decode(string, UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    static Map<String, String> decodeQuery(String query) {
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
        // The DomainApiCheckHandler wraps all calls to the http handlers in order to perform the necessary common operations
        // Normal /management context
        HttpContext context = httpServer.createContext(DOMAIN_API_CONTEXT, new DomainApiCheckHandler(this, controlledProcessStateService));
        addAuthenticator(authenticator, context, securityRealm);
        // /management-upload context
        HttpContext upload = httpServer.createContext("/management-upload", new DomainApiCheckHandler(new DomainApiUploadHandler(modelController), controlledProcessStateService));
        addAuthenticator(authenticator, upload, securityRealm);
    }

    protected void addAuthenticator(final Authenticator authenticator, final HttpContext context, final SecurityRealm securityRealm) {
        // Once there is a trust store we can no longer rely on users being defined so skip
        // any redirects.
        if (authenticator != null) {
            context.setAuthenticator(authenticator);
            List<Filter> filters = context.getFilters();
            if (securityRealm != null &&  securityRealm.getSupportedAuthenticationMechanisms().contains(AuthenticationMechanism.CLIENT_CERT) == false) {
                filters.add(new DmrFailureReadinessFilter(securityRealm, ErrorHandler.getRealmRedirect()));
            }
        }
    }

    public void stop(HttpServer httpServer) {
        httpServer.removeContext(DOMAIN_API_CONTEXT);
        modelController = null;
    }

    private static int getErrorResponseCode(String failureMsg) {
        // WFLY-2037. This is very hacky; better would be something like an internal failure-http-code that
        // is set on the response from the OperationFailedException and stripped from non-HTTP interfaces.
        // But this will do for now.
        int result = INTERNAL_SERVER_ERROR;
        if (failureMsg != null && failureMsg.contains("JBAS013456")) {
            result = FORBIDDEN;
        }
        return result;
    }


    /**
     * Determine whether the prepared response should be sent, before the operation completed. This is needed in order
     * that operations like :reload() can be executed without causing communication failures.
     *
     * @param operation the operation to be executed
     * @return {@code true} if the prepared result should be sent, {@code false} otherwise
     */
    private boolean sendPreparedResponse(final ModelNode operation) {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String op = operation.get(OP).asString();
        final int size = address.size();
        if (size == 0) {
            if (op.equals("reload")) {
                return true;
            } else if (op.equals(COMPOSITE)) {
                // TODO
                return false;
            } else {
                return false;
            }
        } else if (size == 1) {
            if (address.getLastElement().getKey().equals(HOST)) {
                return op.equals("reload");
            }
        }
        return false;
    }

    /**
     * Callback to prevent the response will be sent multiple times.
     */
    private abstract static class ResponseCallback {
        private volatile boolean complete;

        void sendResponse(final OperationResponse response) {
            if (complete) {
                return;
            }
            complete = true;
            doSendResponse(response);
        }

        abstract void doSendResponse(OperationResponse response);
    }

}


