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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.domain.http.server.DomainUtil.writeResponse;
import static org.jboss.as.domain.http.server.HttpServerLogger.ROOT_LOGGER;
import static org.jboss.as.domain.http.server.HttpServerMessages.MESSAGES;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.Methods;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.dmr.ModelNode;
import org.xnio.IoUtils;
import org.xnio.streams.ChannelInputStream;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class DomainApiHandler implements HttpHandler {

    /**
     * Represents all possible management operations that can be executed using HTTP GET
     */
    enum GetOperation {
        /*
         *  It is essential that the GET requests exposed over the HTTP interface are for read only
         *  operations that do not modify the domain model or update anything server side.
         */
        RESOURCE(READ_RESOURCE_OPERATION),
        ATTRIBUTE("read-attribute"),
        RESOURCE_DESCRIPTION(READ_RESOURCE_DESCRIPTION_OPERATION),
        SNAPSHOTS("list-snapshots"),
        OPERATION_DESCRIPTION(READ_OPERATION_DESCRIPTION_OPERATION),
        OPERATION_NAMES(READ_OPERATION_NAMES_OPERATION);

        private String realOperation;

        GetOperation(String realOperation) {
            this.realOperation = realOperation;
        }

        public String realOperation() {
            return realOperation;
        }
    }

    private final ModelControllerClient modelController;

    DomainApiHandler(ModelControllerClient modelController) {
        this.modelController = modelController;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {

        ModelNode dmr;
        ModelNode response;

        HeaderMap requestHeaders = exchange.getRequestHeaders();
        final boolean encode = Common.APPLICATION_DMR_ENCODED.equals(requestHeaders.getFirst(Headers.ACCEPT))
                || Common.APPLICATION_DMR_ENCODED.equals(requestHeaders.getFirst(Headers.CONTENT_TYPE));

        final boolean get = exchange.getRequestMethod().equals(Methods.GET);
        try {
            dmr = get ? convertGetRequest(exchange) : convertPostRequest(exchange, encode);
        } catch (Exception e) {
            ROOT_LOGGER.debugf("Unable to construct ModelNode '%s'", e.getMessage());
            Common.sendError(exchange, get, e.getLocalizedMessage());
            return;
        }

        try {
            response = modelController.execute(new OperationBuilder(dmr).build());
        } catch (Throwable t) {
            ROOT_LOGGER.modelRequestError(t);
            Common.sendError(exchange, get, t.getLocalizedMessage());
            return;
        }

        if (response.hasDefined(OUTCOME) && FAILED.equals(response.get(OUTCOME).asString())) {
            Common.sendError(exchange, get, response.get(FAILURE_DESCRIPTION).asString());
            return;
        }

        boolean pretty = dmr.hasDefined("json.pretty") && dmr.get("json.pretty").asBoolean();

        writeResponse(exchange, get, pretty, response, 200, encode);
    }

    private ModelNode convertPostRequest(HttpServerExchange exchange, boolean encode) throws IOException {
        InputStream in = new ChannelInputStream(exchange.getRequestChannel());
        try {
            return encode ? ModelNode.fromBase64(in) : ModelNode.fromJSONStream(in);
        } finally {
            IoUtils.safeClose(in);
        }
    }

    private ModelNode convertGetRequest(HttpServerExchange exchange) {
        ArrayList<String> pathSegments = decodePath(exchange.getRequestPath());
        Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();

        GetOperation operation = null;
        ModelNode dmr = new ModelNode();
        for (Entry<String, Deque<String>> entry : queryParameters.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().getFirst();
            if (OP.equals(key)) {
                try {
                    operation = GetOperation.valueOf(value.toUpperCase(Locale.ENGLISH).replace('-', '_'));
                    value = operation.realOperation();
                } catch (Exception e) {
                    throw MESSAGES.invalidOperation(e, value);
                }
            }
            dmr.get(entry.getKey()).set(!value.equals("") ? value : "true");
        }

        // This will now only occur if no operation at all was specified on the incoming request.
        if (operation == null) {
            operation = GetOperation.RESOURCE;
            dmr.get(OP).set(operation.realOperation);
        }
        ModelNode list = dmr.get(OP_ADDR).setEmptyList();
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
            return URLDecoder.decode(string, Common.UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }


}
