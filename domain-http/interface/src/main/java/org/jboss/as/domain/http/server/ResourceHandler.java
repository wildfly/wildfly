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


import static org.jboss.as.domain.http.server.Constants.APPLICATION_JAVASCRIPT;
import static org.jboss.as.domain.http.server.Constants.APPLICATION_OCTET_STREAM;
import static org.jboss.as.domain.http.server.Constants.CONTENT_TYPE;
import static org.jboss.as.domain.http.server.Constants.FORBIDDEN;
import static org.jboss.as.domain.http.server.Constants.GET;
import static org.jboss.as.domain.http.server.Constants.IMAGE_GIF;
import static org.jboss.as.domain.http.server.Constants.IMAGE_JPEG;
import static org.jboss.as.domain.http.server.Constants.IMAGE_PNG;
import static org.jboss.as.domain.http.server.Constants.LOCATION;
import static org.jboss.as.domain.http.server.Constants.METHOD_NOT_ALLOWED;
import static org.jboss.as.domain.http.server.Constants.MOVED_PERMENANTLY;
import static org.jboss.as.domain.http.server.Constants.NOT_FOUND;
import static org.jboss.as.domain.http.server.Constants.OK;
import static org.jboss.as.domain.http.server.Constants.TEXT_CSS;
import static org.jboss.as.domain.http.server.Constants.TEXT_HTML;
import static org.jboss.as.domain.http.server.HttpServerMessages.MESSAGES;

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

import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.com.sun.net.httpserver.Headers;
import org.jboss.com.sun.net.httpserver.HttpExchange;
import org.jboss.com.sun.net.httpserver.HttpServer;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;

/**
 * A generic handler to server up resources requested using a GET request.
 *
 * The ClassLoader provided in the constructor should only have access to
 * resources being server by this handler.
 *
 * @author Heiko Braun
 * @date 3/14/11
 */
class ResourceHandler implements ManagementHttpHandler {

    private static final String EXPIRES_HEADER = "Expires";
    private static final String LAST_MODIFIED_HEADER = "Last-Modified";
    private static final String GMT = "GMT";
    private static final String CACHE_CONTROL_HEADER = "Cache-Control";
    private static final String CONTENT_LENGTH_HEADER = "Content-Length";

    private static Map<String, String> contentTypeMapping = new ConcurrentHashMap<String, String>();
    private static final String FORMAT_STRING = "EEE, dd MMM yyyy HH:mm:ss z";

    private final String context;
    private final String defaultResource;
    private final ClassLoader loader;

    private final String lastModified;
    private long lastExpiryDate = 0;
    private String lastExpiryHeader = null;

    private final Map<String, ResourceHandle> buffer = new ConcurrentHashMap<String, ResourceHandle>();

    static {
        contentTypeMapping.put(".js",   APPLICATION_JAVASCRIPT);
        contentTypeMapping.put(".html", TEXT_HTML);
        contentTypeMapping.put(".htm",  TEXT_HTML);
        contentTypeMapping.put(".css",  TEXT_CSS);
        contentTypeMapping.put(".gif",  IMAGE_GIF);
        contentTypeMapping.put(".png",  IMAGE_PNG);
        contentTypeMapping.put(".jpeg", IMAGE_JPEG);
    }

    public ResourceHandler(final String context, final String defaultResource, final ClassLoader loader) {
        this.context = context;
        this.defaultResource = defaultResource;
        this.loader = loader;

        lastModified = createDateFormat().format(new Date());
    }

    String getDefaultUrl() {
        return context + defaultResource;
    }

    protected String getContext() {
        return context;
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
        String resource = path.substring(context.length(), path.length());
        if(resource.startsWith("/")) resource = resource.substring(1);

        if (resource.equals("")) {
            /*
             * This is a request to the root of the context, redirect to the
             * default resource.
             */
            Headers responseHeaders = http.getResponseHeaders();
            responseHeaders.add(LOCATION, getDefaultUrl());
            http.sendResponseHeaders(MOVED_PERMENANTLY, 0);
            http.close();

            return;
        } else if (!resource.contains(".")) {
            respond404(http);
        }

        /*
         * This allows a sub-class of the ResourceHandler to store resources it may need in META-INF
         * without these resources being served up to remote clients unchecked.
         */
        if (resource.startsWith("META-INF")) {
            http.sendResponseHeaders(FORBIDDEN, 0);
            http.close();

            return;
        }

        // load resource
        ResourceHandle handle = getResourceHandle(resource);

        if(handle.getInputStream()!=null) {

            InputStream inputStream = handle.getInputStream();

            final Headers responseHeaders = http.getResponseHeaders();
            responseHeaders.add(CONTENT_TYPE, resolveContentType(path));

            // provide the ability to cache GWT artifacts
            if(!skipCache(resource)){

                if(System.currentTimeMillis()>lastExpiryDate) {
                    lastExpiryDate = calculateExpiryDate();
                    lastExpiryHeader = createDateFormat().format(new Date(lastExpiryDate));
                }

                responseHeaders.add(CACHE_CONTROL_HEADER, "private, max-age=2678400, must-revalidate");
                responseHeaders.add(EXPIRES_HEADER, lastExpiryHeader);
            }

            responseHeaders.add(LAST_MODIFIED_HEADER, lastModified);
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
            return loader;
    }

    public void start(HttpServer httpServer, SecurityRealm securityRealm) {
        httpServer.createContext(context, this);
    }

    public void stop(HttpServer httpServer) {
        httpServer.removeContext(context);
    }

    protected static ClassLoader getClassLoader(final String module, final String slot) throws ModuleLoadException {
        ModuleIdentifier id = ModuleIdentifier.create(module, slot);
        ClassLoader cl = Module.getCallerModuleLoader().loadModule(id).getClassLoader();

        return cl;
    }

    protected boolean skipCache(String resource) {
        return false;
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
