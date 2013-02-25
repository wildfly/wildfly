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

import io.undertow.io.IoCallback;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class Common {

    static final ResponseCodeHandler METHOD_NOT_ALLOWED_HANDLER = new ResponseCodeHandler(405);
    static final ResponseCodeHandler MOVED_PERMANENTLY = new ResponseCodeHandler(301);
    static final ResponseCodeHandler TEMPORARY_REDIRECT = new ResponseCodeHandler(307);
    static final ResponseCodeHandler SERVICE_UNAVAIABLE = new ResponseCodeHandler(503);
    static final ResponseCodeHandler UNSUPPORTED_MEDIA_TYPE = new ResponseCodeHandler(415);

    static final String APPLICATION_DMR_ENCODED = "application/dmr-encoded";
    static final String APPLICATION_JSON = "application/json";
    static final String TEXT_PLAIN = "text/plain";
    static final String APPLICATION_JAVASCRIPT = "application/javascript";
    static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
    static final String IMAGE_GIF = "image/gif";
    static final String IMAGE_JPEG = "image/jpeg";
    static final String IMAGE_PNG = "image/png";
    static final String TEXT_CSS = "text/css";
    static final String TEXT_HTML = "text/html";


    static String UTF_8 = "utf-8";
    static final String GMT = "GMT";

    static void sendError(HttpServerExchange exchange, boolean isGet, String msg) {
        byte[] bytes = msg.getBytes();
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, TEXT_PLAIN + ";" + UTF_8);
        exchange.getResponseHeaders().put(Headers.CONTENT_LENGTH, String.valueOf(bytes.length));
        exchange.setResponseCode(StatusCodes.CODE_500.getCode());

        exchange.getResponseSender().send(msg, IoCallback.END_EXCHANGE);
    }
}
