/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
import static org.jboss.as.domain.http.server.DomainUtil.writeResponse;
import static org.jboss.as.domain.http.server.HttpServerLogger.ROOT_LOGGER;

import java.io.ByteArrayInputStream;
import java.util.Deque;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.util.Headers;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.core.security.AccessMechanism;
import org.jboss.dmr.ModelNode;

/**
 * Generic http POST handler accepting a single operation and multiple input streams passed as part of
 * a {@code multipart/form-data} message. The operation is required, the attachment streams are optional.
 *
 * Content-Disposition: form-data; name="operation"
 * (optional) Content-Type: application/dmr-encoded
 *
 * Content-Disposition: form-data; name="..."; filename="..."
 *
 * @author Emanuel Muckenhuber
 */
class DomainApiGenericOperationHandler implements HttpHandler {

    private static final String OPERATION = "operation";
    private static final String INPUT_STREAMS = "input-streams";

    private final ModelController modelController;
    private final FormParserFactory formParserFactory;

    public DomainApiGenericOperationHandler(ModelController modelController) {
        this.modelController = modelController;
        this.formParserFactory = FormParserFactory.builder().build();
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        final FormDataParser parser = formParserFactory.createParser(exchange);
        if (parser == null) {
            Common.UNSUPPORTED_MEDIA_TYPE.handleRequest(exchange);
            return;
        }
        // Parse the form data
        final FormData data = parser.parseBlocking();
        final OperationParameter.Builder operationParameterBuilder = new OperationParameter.Builder(false);

        // Process the operation
        final FormData.FormValue op = data.getFirst(OPERATION);
        final ModelNode operation;
        try {
            if (Common.APPLICATION_DMR_ENCODED.equals(op.getHeaders().getFirst(Headers.CONTENT_TYPE))) {
                operation = ModelNode.fromBase64(new ByteArrayInputStream(op.getValue().getBytes()));
                operationParameterBuilder.encode(true);
            } else {
                operation = ModelNode.fromJSONString(op.getValue());
            }
        }  catch (Exception e) {
            ROOT_LOGGER.errorf("Unable to construct ModelNode '%s'", e.getMessage());
            Common.sendError(exchange, false, e.getLocalizedMessage());
            return;
        }

        // Process the input streams
        final OperationBuilder builder = OperationBuilder.create(operation, true);
        final Deque<FormData.FormValue> contents = data.get(INPUT_STREAMS);
        if (contents != null && !contents.isEmpty()) {
            for (final FormData.FormValue value : contents) {
                builder.addFileAsAttachment(value.getFile());
            }
        }
        operationParameterBuilder.pretty(operation.hasDefined("json.pretty") && operation.get("json.pretty").asBoolean());
        // Response callback to handle the :reload operation
        final OperationParameter opParam = operationParameterBuilder.build();
        final ResponseCallback callback = new ResponseCallback() {
            @Override
            void doSendResponse(final ModelNode response) {
                if (response.hasDefined(OUTCOME) && FAILED.equals(response.get(OUTCOME).asString())) {
                    Common.sendError(exchange, opParam.isEncode(), response);
                    return;
                }
                writeResponse(exchange, 200, response, opParam);
            }
        };

        final boolean sendPreparedResponse = sendPreparedResponse(operation);
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

        ModelNode response;
        try {
            operation.get(OPERATION_HEADERS, ACCESS_MECHANISM).set(AccessMechanism.HTTP.toString());
            response = modelController.execute(operation, OperationMessageHandler.DISCARD, control, builder.build());
        } catch (Throwable t) {
            ROOT_LOGGER.modelRequestError(t);
            Common.sendError(exchange, opParam.isEncode(), t.getLocalizedMessage());
            return;
        }

        callback.sendResponse(response);
    }

    static final String RELOAD = "reload";

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
            if (op.equals(RELOAD)) {
                return true;
            } else if (op.equals(COMPOSITE)) {
                // TODO
                return false;
            } else {
                return false;
            }
        } else if (size == 1) {
            if (address.getLastElement().getKey().equals(HOST)) {
                return op.equals(RELOAD);
            }
        }
        return false;
    }

    /**
     * Callback to prevent the response will be sent multiple times.
     */
    private abstract static class ResponseCallback {
        private boolean complete;

        synchronized void sendResponse(final ModelNode response) {
            if (complete) {
                return;
            }
            complete = true;
            doSendResponse(response);
        }

        abstract void doSendResponse(ModelNode response);
    }

}
