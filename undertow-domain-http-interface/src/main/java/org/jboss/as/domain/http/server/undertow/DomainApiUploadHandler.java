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
package org.jboss.as.domain.http.server.undertow;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.domain.http.server.undertow.UndertowHttpServerLogger.ROOT_LOGGER;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.blocking.BlockingHttpHandler;
import io.undertow.server.handlers.blocking.BlockingHttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormData.FormValue;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.dmr.ModelNode;
import org.xnio.IoUtils;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class DomainApiUploadHandler implements BlockingHttpHandler{

    private final ModelControllerClient modelController;

    public DomainApiUploadHandler(ModelControllerClient modelController) {
        this.modelController = modelController;
    }

    @Override
    public void handleRequest(BlockingHttpServerExchange blockingExchange) throws Exception {
        final HttpServerExchange exchange = blockingExchange.getExchange();
        final FormDataParser parser = exchange.getAttachment(FormDataParser.ATTACHMENT_KEY);
        FormData data = parser.parse().get();
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
                        Common.sendError(blockingExchange, false, response.get(FAILURE_DESCRIPTION).asString());
                        return;
                    }
                } catch (Throwable t) {
                    // TODO Consider draining input stream
                    ROOT_LOGGER.uploadError(t);
                    Common.sendError(blockingExchange, false, t.getLocalizedMessage());
                    return;
                } finally {
                    IoUtils.safeClose(in);
                }

                // TODO Determine what format the response should be in for a deployment upload request.
                writeResponse(blockingExchange, response, Common.TEXT_HTML);
                return; //Ignore later files
            }
        }
        Common.sendError(blockingExchange, false, "No file found"); //TODO i18n
    }

    static void writeResponse(BlockingHttpServerExchange blockingExchange, ModelNode response, String contentType) {
        HttpServerExchange exchange = blockingExchange.getExchange();
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, contentType  + ";" + Common.UTF_8);
        exchange.setResponseCode(StatusCodes.CODE_200.getCode());

        //TODO Content-Length?

        PrintWriter print = new PrintWriter(blockingExchange.getOutputStream());
        try {
            response.writeJSONString(print, true);
        } finally {
            print.flush();
            IoUtils.safeClose(print);
        }
    }
}
