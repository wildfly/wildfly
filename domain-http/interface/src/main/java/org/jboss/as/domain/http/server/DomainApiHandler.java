/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.domain.http.server.DomainUtil.writeResponse;
import static org.jboss.as.domain.http.server.HttpServerLogger.ROOT_LOGGER;
import static org.jboss.as.domain.http.server.HttpServerMessages.MESSAGES;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.ETag;
import io.undertow.util.ETagUtils;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.HexConverter;
import io.undertow.util.Methods;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.core.security.AccessMechanism;
import org.jboss.dmr.ModelNode;
import org.xnio.IoUtils;
import org.xnio.streams.ChannelInputStream;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class DomainApiHandler implements HttpHandler {

    /**
     * Represents all possible management operations that can be executed using HTTP GET. Cacheable operations
     * have a {@code maxAge} property &gt; 0.
     */
    enum GetOperation {
        /*
         *  It is essential that the GET requests exposed over the HTTP interface are for read only
         *  operations that do not modify the domain model or update anything server side.
         */
        RESOURCE(READ_RESOURCE_OPERATION, 0),
        ATTRIBUTE("read-attribute", 0),
        RESOURCE_DESCRIPTION(READ_RESOURCE_DESCRIPTION_OPERATION, Common.ONE_WEEK),
        SNAPSHOTS("list-snapshots", 0),
        OPERATION_DESCRIPTION(READ_OPERATION_DESCRIPTION_OPERATION, Common.ONE_WEEK),
        OPERATION_NAMES(READ_OPERATION_NAMES_OPERATION, 0);

        private String realOperation;
        private int maxAge;

        GetOperation(String realOperation, int maxAge) {
            this.realOperation = realOperation;
            this.maxAge = maxAge;
        }

        public String realOperation() {
            return realOperation;
        }

        public int getMaxAge() {
            return maxAge;
        }
    }

    private final ModelController modelController;

    DomainApiHandler(ModelController modelController) {
        this.modelController = modelController;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) {

        final ModelNode dmr;
        ModelNode response;

        HeaderMap requestHeaders = exchange.getRequestHeaders();
        final boolean cachable;
        final boolean get = exchange.getRequestMethod().equals(Methods.GET);
        final boolean encode = Common.APPLICATION_DMR_ENCODED.equals(requestHeaders.getFirst(Headers.ACCEPT))
                || Common.APPLICATION_DMR_ENCODED.equals(requestHeaders.getFirst(Headers.CONTENT_TYPE));
        final OperationParameter.Builder operationParameterBuilder = new OperationParameter.Builder(get).encode(encode);

        try {
            if (get) {
                GetOperation operation = getOperation(exchange);
                operationParameterBuilder.maxAge(operation.getMaxAge());
                dmr = convertGetRequest(exchange, operation);
                cachable = operation.getMaxAge() > 0;
            } else {
                dmr = convertPostRequest(exchange, encode);
                cachable = false;
            }
        } catch (Exception e) {
            ROOT_LOGGER.debugf("Unable to construct ModelNode '%s'", e.getMessage());
            Common.sendError(exchange, false, e.getLocalizedMessage());
            return;
        }

        final ResponseCallback callback = new ResponseCallback() {
            @Override
            void doSendResponse(final ModelNode response) {
                if (response.hasDefined(OUTCOME) && FAILED.equals(response.get(OUTCOME).asString())) {
                    Common.sendError(exchange, encode, response);
                    return;
                }
                writeResponse(exchange, 200, response, operationParameterBuilder.build());
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
                callback.sendResponse(result);
            }
        } : ModelController.OperationTransactionControl.COMMIT;

        try {
            dmr.get(OPERATION_HEADERS, ACCESS_MECHANISM).set(AccessMechanism.HTTP.toString());
            response = modelController.execute(dmr, OperationMessageHandler.logging, control, new OperationBuilder(dmr).build());
            if (cachable) {
                // Use the MD5 of the model nodes toString() method as ETag
                MessageDigest md = MessageDigest.getInstance("MD5");
                md.update(response.toString().getBytes());
                ETag etag = new ETag(false, HexConverter.convertToHexString(md.digest()));
                operationParameterBuilder.etag(etag);
                if (!ETagUtils.handleIfNoneMatch(exchange, etag, false)) {
                    exchange.setResponseCode(304);
                    DomainUtil.writeCacheHeaders(exchange, 304, operationParameterBuilder.build());
                    exchange.endExchange();
                    return;
                }
            }
        } catch (Throwable t) {
            ROOT_LOGGER.modelRequestError(t);
            Common.sendError(exchange, encode, t.getLocalizedMessage());
            return;
        }

        callback.sendResponse(response);
    }

    private GetOperation getOperation(HttpServerExchange exchange) {
        Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();

        GetOperation operation = null;
        Deque<String> parameter = queryParameters.get(OP);
        if (parameter != null) {
            String value = parameter.getFirst();
            try {
                operation = GetOperation.valueOf(value.toUpperCase(Locale.ENGLISH).replace('-', '_'));
                value = operation.realOperation();
            } catch (Exception e) {
                throw MESSAGES.invalidOperation(e, value);
            }
        }

        // This will now only occur if no operation at all was specified on the incoming request.
        if (operation == null) {
            operation = GetOperation.RESOURCE;
        }
        return operation;
    }

    private ModelNode convertGetRequest(HttpServerExchange exchange, GetOperation operation) {
        ArrayList<String> pathSegments = decodePath(exchange.getRequestPath());
        Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();

        ModelNode dmr = new ModelNode();
        for (Entry<String, Deque<String>> entry : queryParameters.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().getFirst();
            ModelNode valueNode;
            if (key.startsWith("operation-header-")) {
                String header = key.substring("operation-header-".length());
                valueNode = dmr.get(OPERATION_HEADERS, header);
            } else {
                valueNode = dmr.get(key);
            }
            valueNode.set(!value.equals("") ? value : "true");
        }
        dmr.get(OP).set(operation.realOperation);

        ModelNode list = dmr.get(OP_ADDR).setEmptyList();
        for (int i = 1; i < pathSegments.size() - 1; i += 2) {
            list.add(pathSegments.get(i), pathSegments.get(i + 1));
        }
        return dmr;
    }

    private ModelNode convertPostRequest(HttpServerExchange exchange, boolean encode) throws IOException {
        InputStream in = new ChannelInputStream(exchange.getRequestChannel());
        try {
            return encode ? ModelNode.fromBase64(in) : ModelNode.fromJSONStream(in);
        } finally {
            IoUtils.safeClose(in);
        }
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
            return URLDecoder.decode(string, Common.UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
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

        void sendResponse(final ModelNode response) {
            if (complete) {
                return;
            }
            complete = true;
            doSendResponse(response);
        }

        abstract void doSendResponse(ModelNode response);
    }
}
