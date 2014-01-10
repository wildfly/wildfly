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

import io.undertow.io.IoCallback;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.util.Headers;
import org.jboss.dmr.ModelNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static io.undertow.server.handlers.ResponseCodeHandler.HANDLE_404;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class Common {

    public static final ResponseCodeHandler MOVED_PERMANENTLY = new ResponseCodeHandler(301);
    public static final ResponseCodeHandler TEMPORARY_REDIRECT = new ResponseCodeHandler(307);
    public static final ResponseCodeHandler UNAUTHORIZED = new ResponseCodeHandler(403);
    public static final ResponseCodeHandler NOT_FOUND = HANDLE_404;
    public static final ResponseCodeHandler METHOD_NOT_ALLOWED_HANDLER = new ResponseCodeHandler(405);
    public static final ResponseCodeHandler UNSUPPORTED_MEDIA_TYPE = new ResponseCodeHandler(415);
    public static final ResponseCodeHandler INTERNAL_SERVER_ERROR = new ResponseCodeHandler(500);
    public static final ResponseCodeHandler SERVICE_UNAVAIABLE = new ResponseCodeHandler(503);

    static final String APPLICATION_DMR_ENCODED = "application/dmr-encoded";
    static final String APPLICATION_JSON = "application/json";
    static final String TEXT_PLAIN = "text/plain";
    static final String TEXT_HTML = "text/html";
    static final int ONE_WEEK = 7 * 24 * 60 * 60;


    static String UTF_8 = "utf-8";

    static void sendError(HttpServerExchange exchange, boolean encode, String msg) {
        int errorCode = getErrorResponseCode(msg);
        sendError(exchange, encode, new ModelNode(msg), errorCode);
    }

    static void sendError(HttpServerExchange exchange, boolean encode, ModelNode msg) {
        int errorCode = getErrorResponseCode(msg.asString());
        sendError(exchange, encode, msg, errorCode);
    }

    private static void sendError(HttpServerExchange exchange, boolean encode, ModelNode msg, int errorCode) {
        if(encode) {

            try {
                ModelNode response = new ModelNode();
                response.set(msg);

                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                response.writeBase64(bout);
                byte[] bytes = bout.toByteArray();

                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, APPLICATION_DMR_ENCODED+ "; charset=" + UTF_8);
                exchange.getResponseHeaders().put(Headers.CONTENT_LENGTH, String.valueOf(bytes.length));
                exchange.setResponseCode(errorCode);

                exchange.getResponseSender().send(new String(bytes), IoCallback.END_EXCHANGE);

            } catch (IOException e) {
                // fallback, should not happen
                sendError(exchange, false, msg);
            }

        }
        else {
            StringWriter stringWriter = new StringWriter();
            final PrintWriter print = new PrintWriter(stringWriter);
            try {
                msg.writeJSONString(print, false);
            } finally {
                print.flush();
                stringWriter.flush();
                print.close();
            }

            String msgString = stringWriter.toString();
            byte[] bytes = msgString.getBytes();
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, TEXT_PLAIN + "; charset=" + UTF_8);
            exchange.getResponseHeaders().put(Headers.CONTENT_LENGTH, String.valueOf(bytes.length));
            exchange.setResponseCode(errorCode);

            exchange.getResponseSender().send(msgString, IoCallback.END_EXCHANGE);
        }
    }

    private static int getErrorResponseCode(String failureMsg) {
        // WFLY-2037. This is very hacky; better would be something like an internal failure-http-code that
        // is set on the response from the OperationFailedException and stripped from non-HTTP interfaces.
        // But this will do for now.
        int result = 500;
        if (failureMsg != null && failureMsg.contains("JBAS013456")) {
            result = 403;
        }
        return result;
    }

}
