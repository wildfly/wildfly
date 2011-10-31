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


import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.com.sun.net.httpserver.Headers;
import org.jboss.com.sun.net.httpserver.HttpExchange;
import org.jboss.com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import static org.jboss.as.domain.http.server.Constants.*;
import static org.jboss.as.domain.http.server.HttpServerMessages.MESSAGES;

/**
 * @author Heiko Braun
 * @date 3/14/11
 */
public class ConsoleHandler implements ManagementHttpHandler {

    public  static final String CONTEXT = "/console";
    private static final String EXPIRES_HEADER = "Expires";
    private static final String LAST_MODIFIED_HEADER = "Last-Modified";
    private static final String NOCACHE_JS = ".nocache.js";
    private static final String GMT = "GMT";
    private static final String CACHE_CONTROL_HEADER = "Cache-Control";
    private static final String CONTENT_LENGTH_HEADER = "Content-Length";

    private ClassLoader loader = null;

    private static Map<String, String> contentTypeMapping = new ConcurrentHashMap<String, String>();

    private long lastExpiryDate = 0;
    private String lastExpiryHeader = null;

    private static String LAST_MODIFIED;
    private static final String FORMAT_STRING = "EEE, dd MMM yyyy HH:mm:ss z";

    private static Map<String, ResourceHandle> buffer = new ConcurrentHashMap<String, ResourceHandle>();

    static {


        LAST_MODIFIED = createDateFormat().format(new Date());

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

        if (resource.equals("")) {
            // "/console" request redirect to "/console/index.html"

            Headers responseHeaders = http.getResponseHeaders();
            responseHeaders.add(LOCATION, "/console/index.html");
            http.sendResponseHeaders(MOVED_PERMENANTLY, 0);
            http.close();

            return;
        } else if (resource.indexOf(".") == -1) {
            respond404(http);
        }

        // load resource
        ResourceHandle handle = getResourceHandle(resource);

        if(handle.getInputStream()!=null) {

            InputStream inputStream = handle.getInputStream();

            final Headers responseHeaders = http.getResponseHeaders();
            responseHeaders.add(CONTENT_TYPE, resolveContentType(path));

            boolean skipcache = resource.endsWith(NOCACHE_JS);

            // provide the ability to cache GWT artifacts
            if(!skipcache){

                if(System.currentTimeMillis()>lastExpiryDate) {
                    lastExpiryDate = calculateExpiryDate();
                    lastExpiryHeader = createDateFormat().format(new Date(lastExpiryDate));
                }

                responseHeaders.add(CACHE_CONTROL_HEADER, "private, max-age=2678400, must-revalidate");
                responseHeaders.add(EXPIRES_HEADER, lastExpiryHeader);
            }

            responseHeaders.add(LAST_MODIFIED_HEADER, LAST_MODIFIED);
            responseHeaders.add(CONTENT_LENGTH_HEADER, String.valueOf(handle.getSize()));

            http.sendResponseHeaders(OK, 0);

            // nio write
            OutputStream outputStream = http.getResponseBody();
            fastChannelCopy(inputStream, outputStream);
            outputStream.flush();

            safeClose(outputStream);
            safeClose(inputStream);

        } else {
            respond404(http);
        }

    }

    private ResourceHandle getResourceHandle(String resource) {


        ResourceHandle handle = buffer.get(resource);

        if(handle==null){

            InputStream resourceStream = getLoader().getResourceAsStream(resource);

            if(resourceStream!=null) {

                try {
                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    fastChannelCopy(resourceStream, bout);
                    bout.flush();
                    bout.close();
                    resourceStream.close();
                    handle = new ResourceHandle(bout.toByteArray());
                } catch (IOException e) {
                    throw MESSAGES.failedReadingResource(e, resource);
                }

                 buffer.put(resource, handle);
            }
            else {
                // 404
                handle = new ResourceHandle(null);
            }
        }

        return handle;
    }

    private static DateFormat createDateFormat(){
        DateFormat df = new SimpleDateFormat(FORMAT_STRING, Locale.US);
        df.setTimeZone(TimeZone.getTimeZone(GMT));
        return df;
    }

    private static long calculateExpiryDate() {
        Calendar cal = Calendar.getInstance();
        cal.roll(Calendar.MONTH, 1);
        return cal.getTime().getTime();
    }

    public static void fastChannelCopy(final InputStream in, final OutputStream out) throws IOException {

        final ReadableByteChannel src = Channels.newChannel(in);
        final WritableByteChannel dest = Channels.newChannel(out);

        try {
            final ByteBuffer buffer = ByteBuffer.allocate(8 * 1024);
            while (src.read(buffer) != -1) {
                buffer.flip();
                dest.write(buffer);
                buffer.compact();
            }
            buffer.flip();

            while (buffer.hasRemaining()) {
                dest.write(buffer);
            }
        } finally {
            safeClose(src);
            safeClose(dest);
        }
    }

    private static void safeClose(Closeable close) {
        try {
            if(close!=null)
                close.close();
        } catch (Throwable eat) {
        }
    }

    private String resolveContentType(String resource) {
        assert resource.indexOf(".")!=-1 : MESSAGES.invalidResource();

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

    public void start(HttpServer httpServer, SecurityRealm securityRealm) {
        httpServer.createContext(CONTEXT, this);
    }

    public void stop(HttpServer httpServer) {
        httpServer.removeContext(CONTEXT);
    }

    class ResourceHandle {

        private final byte[] content;

        ResourceHandle(byte[] content) {
            this.content = content;
        }

        public int getSize() {
            return content.length;
        }

        public InputStream getInputStream() {
            return content!=null ? new ByteArrayInputStream(content) : null;
        }
    }
}
