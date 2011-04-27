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

import static org.jboss.as.domain.http.server.Constants.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.jboss.as.domain.http.server.Constants.APPLICATION_JAVASCRIPT;
import static org.jboss.as.domain.http.server.Constants.APPLICATION_OCTET_STREAM;
import static org.jboss.as.domain.http.server.Constants.CONTENT_TYPE;
import static org.jboss.as.domain.http.server.Constants.FOUND;
import static org.jboss.as.domain.http.server.Constants.GET;
import static org.jboss.as.domain.http.server.Constants.IMAGE_GIF;
import static org.jboss.as.domain.http.server.Constants.IMAGE_JPEG;
import static org.jboss.as.domain.http.server.Constants.IMAGE_PNG;
import static org.jboss.as.domain.http.server.Constants.LOCATION;
import static org.jboss.as.domain.http.server.Constants.METHOD_NOT_ALLOWED;
import static org.jboss.as.domain.http.server.Constants.NOT_FOUND;
import static org.jboss.as.domain.http.server.Constants.OK;
import static org.jboss.as.domain.http.server.Constants.TEXT_CSS;
import static org.jboss.as.domain.http.server.Constants.TEXT_HTML;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.com.sun.net.httpserver.Headers;
import org.jboss.com.sun.net.httpserver.HttpExchange;
import org.jboss.com.sun.net.httpserver.HttpServer;

/**
 * @author Heiko Braun
 * @date 3/14/11
 */
public class ConsoleHandler implements ManagementHttpHandler {

    public static final String CONTEXT = "/console";

    private ClassLoader loader = null;

    private static Map<String, String> contentTypeMapping = new ConcurrentHashMap<String, String>();

    static {
        contentTypeMapping.put(".js",   APPLICATION_JAVASCRIPT);
        contentTypeMapping.put(".html", TEXT_HTML);
        contentTypeMapping.put(".htm",  TEXT_HTML);
        contentTypeMapping.put(".css",  TEXT_CSS);
        contentTypeMapping.put(".gif",  IMAGE_GIF);
        contentTypeMapping.put(".png",  IMAGE_PNG);
        contentTypeMapping.put(".jpeg", IMAGE_JPEG);
    }

    public ConsoleHandler() {
    }

    public ConsoleHandler(ClassLoader loader) {
        this.loader = loader;
    }

    @Override
    public void handle(HttpExchange http) throws IOException {
        final URI uri = http.getRequestURI();
        final String requestMethod = http.getRequestMethod();

        // only GET supported
        if (!GET.equals(requestMethod)) {
            http.sendResponseHeaders(METHOD_NOT_ALLOWED, -1);
            return;
        }

        // normalize to request resource
        String path = uri.getPath();
        String resource = path.substring(CONTEXT.length(), path.length());
        if(resource.startsWith("/")) resource = resource.substring(1);


        if(resource.equals("")) {
            // "/console" request redirect to "/console/index.html"

            InetSocketAddress address = http.getHttpContext().getServer().getAddress();
            String hostName = address.getHostName();
            int port = address.getPort();
            final Headers responseHeaders = http.getResponseHeaders();
            responseHeaders.add(CONTENT_TYPE, TEXT_HTML);
            responseHeaders.add(LOCATION, "http://"+hostName + ":"+port+"/console/index.html");
            http.sendResponseHeaders(FOUND, 0);


            OutputStream outputStream = http.getResponseBody();
            outputStream.flush();
            safeClose(outputStream);

            return;
        } else if(resource.indexOf(".")==-1) {
            respond404(http);
        }

        // load resource
        InputStream inputStream = getLoader().getResourceAsStream(resource);
        if(inputStream!=null) {

            final Headers responseHeaders = http.getResponseHeaders();
            responseHeaders.add(CONTENT_TYPE, resolveContentType(path));
            responseHeaders.add(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            http.sendResponseHeaders(OK, 0);

            OutputStream outputStream = http.getResponseBody();

            int nextChar;
            while ( ( nextChar = inputStream.read() ) != -1  ) {
                outputStream.write(nextChar);
            }

            outputStream.flush();
            safeClose(outputStream);
            safeClose(inputStream);

        } else {
            respond404(http);
        }

    }

    private void safeClose(Closeable close) {
        try {
            close.close();
        } catch (Throwable eat) {
        }
    }

    private String resolveContentType(String resource) {
        assert resource.indexOf(".")!=-1 : "Invalid resource";

        String contentType = null;
        for(String suffix : contentTypeMapping.keySet()) {
            if(resource.endsWith(suffix)) {
                contentType = contentTypeMapping.get(suffix);
                break;
            }
        }

        if(null==contentType) contentType = APPLICATION_OCTET_STREAM;

        return contentType;
    }

    private void respond404(HttpExchange http) throws IOException {

        final Headers responseHeaders = http.getResponseHeaders();
        responseHeaders.add(CONTENT_TYPE, TEXT_HTML);
        responseHeaders.add(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        http.sendResponseHeaders(NOT_FOUND, 0);
        OutputStream out = http.getResponseBody();
        out.flush();
        safeClose(out);
    }

    private ClassLoader getLoader() {
        if(loader!=null)
            return loader;
        else
            return ConsoleHandler.class.getClassLoader();
    }

    @Override
    public void start(HttpServer httpServer) {
        httpServer.createContext(CONTEXT, this);
    }

    @Override
    public void stop(HttpServer httpServer) {
        httpServer.removeContext(CONTEXT);
    }
}
