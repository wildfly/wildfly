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

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormData.FormValue;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.util.Headers;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.dmr.ModelNode;
import org.xnio.IoUtils;
import org.xnio.streams.ChannelOutputStream;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.domain.http.server.HttpServerLogger.ROOT_LOGGER;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class DomainApiUploadHandler implements HttpHandler {

    private final ModelControllerClient modelController;

    public DomainApiUploadHandler(ModelControllerClient modelController) {
        this.modelController = modelController;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        final FormDataParser parser = exchange.getAttachment(FormDataParser.ATTACHMENT_KEY);
        FormData data = parser.parseBlocking();
        for (String fieldName : data) {
            //Get all the files
            FormValue value = data.getFirst(fieldName);
            if (value.isFile()) {
                ModelNode response = null;
                InputStream in = new BufferedInputStream(new FileInputStream(value.getFile()));
                try {
                    final ModelNode dmr = new ModelNode();
                    dmr.get("operation").set("upload-deployment-stream");
                    dmr.get("address").setEmptyList();
                    dmr.get("input-stream-index").set(0);

                    OperationBuilder operation = new OperationBuilder(dmr);
                    operation.addInputStream(in);
                    response = modelController.execute(operation.build());
                    if (!response.get(OUTCOME).asString().equals(SUCCESS)){
                        Common.sendError(exchange, false, response.get(FAILURE_DESCRIPTION).asString());
                        return;
                    }
                } catch (Throwable t) {
                    // TODO Consider draining input stream
                    ROOT_LOGGER.uploadError(t);
                    Common.sendError(exchange, false, t.getLocalizedMessage());
                    return;
                } finally {
                    IoUtils.safeClose(in);
                }

                // TODO Determine what format the response should be in for a deployment upload request.
                writeResponse(exchange, response, Common.TEXT_HTML);
                return; //Ignore later files
            }
        }
        Common.sendError(exchange, false, "No file found"); //TODO i18n
    }

    static void writeResponse(HttpServerExchange exchange, ModelNode response, String contentType) {
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, contentType  + ";" + Common.UTF_8);
        exchange.setResponseCode(200);

        //TODO Content-Length?

        PrintWriter print = new PrintWriter(new ChannelOutputStream(exchange.getResponseChannel()));
        try {
            response.writeJSONString(print, true);
        } finally {
            print.flush();
            IoUtils.safeClose(print);
        }
    }
}
