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

import static org.jboss.as.domain.http.server.HttpServerMessages.MESSAGES;
import io.undertow.io.UndertowOutputStream;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.HttpHandlers;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.blocking.BlockingHttpHandler;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.xnio.IoUtils;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class ResourceHandler implements BlockingHttpHandler {


    private static Map<String, String> contentTypeMapping = new ConcurrentHashMap<String, String>();
    private static final String FORMAT_STRING = "EEE, dd MMM yyyy HH:mm:ss z";

    private final String context;
    private final String defaultResource;
    private final ClassLoader loader;

    private long lastExpiryDate = 0;
    private String lastExpiryHeader = null;

    private final Map<String, ResourceHandle> buffer = new ConcurrentHashMap<String, ResourceHandle>();

    static {
        contentTypeMapping.put(".js",   Common.APPLICATION_JAVASCRIPT);
        contentTypeMapping.put(".html", Common.TEXT_HTML);
        contentTypeMapping.put(".htm",  Common.TEXT_HTML);
        contentTypeMapping.put(".css",  Common.TEXT_CSS);
        contentTypeMapping.put(".gif",  Common.IMAGE_GIF);
        contentTypeMapping.put(".png",  Common.IMAGE_PNG);
        contentTypeMapping.put(".jpeg", Common.IMAGE_JPEG);
    }

    ResourceHandler(final String context, final String defaultResource, final ClassLoader loader) {
        this.context = context;
        this.defaultResource = defaultResource;
        this.loader = loader;
    }

    String getDefaultPath() {
        return context + defaultResource;
    }

    protected String getContext() {
        return context;
    }

    public void handleBlockingRequest(HttpServerExchange exchange) {
        final HttpString requestMethod = exchange.getRequestMethod();

        // only GET supported
        if (!Methods.GET.equals(requestMethod)) {
            HttpHandlers.executeHandler(Common.METHOD_NOT_ALLOWED_HANDLER, exchange);
            return;
        }

        // normalize to request resource
        String path = exchange.getRelativePath();
        String resource = path.startsWith("/") ? path.substring(1) : path;

        if (resource.equals("")) {
            /*
             * This is a request to the root of the context, redirect to the
             * default resource.
             */
            HeaderMap responseHeaders = exchange.getResponseHeaders();
            responseHeaders.add(Headers.LOCATION, getDefaultPath());
            HttpHandlers.executeHandler(Common.MOVED_PERMANENTLY, exchange);
            return;
        } else if (!resource.contains(".")) {
            HttpHandlers.executeHandler(ResponseCodeHandler.HANDLE_404, exchange);
            return;
        }

        /*
         * This allows a sub-class of the ResourceHandler to store resources it may need in META-INF
         * without these resources being served up to remote clients unchecked.
         */
        if (resource.startsWith("META-INF")) {
            //Forbidden
            HttpHandlers.executeHandler(ResponseCodeHandler.HANDLE_403, exchange);
            return;
        }

        // load resource
        ResourceHandle handle = getResourceHandle(resource);

        if(handle.getInputStream() != null) {

            InputStream inputStream = handle.getInputStream();
            try {
                final HeaderMap responseHeaders = exchange.getResponseHeaders();
                responseHeaders.add(Headers.CONTENT_TYPE, resolveContentType(path));

                // provide the ability to cache GWT artifacts
                if(!skipCache(resource)){

                    if(System.currentTimeMillis()>lastExpiryDate) {
                        lastExpiryDate = calculateExpiryDate();
                        lastExpiryHeader = createDateFormat().format(new Date(lastExpiryDate));
                    }

                    responseHeaders.add(Headers.CACHE_CONTROL, "public, max-age=2678400");
                    responseHeaders.add(Headers.EXPIRES, lastExpiryHeader);
                } else {
                    responseHeaders.add(Headers.CACHE_CONTROL, "no-cache");
                }

                //responseHeaders.add(LAST_MODIFIED_HEADER, lastModified);
                responseHeaders.add(Headers.CONTENT_LENGTH, String.valueOf(handle.getSize()));

                exchange.setResponseCode(StatusCodes.CODE_200.getCode());

                // nio write
                OutputStream outputStream = new UndertowOutputStream(exchange);
                try {
                    fastChannelCopy(inputStream, outputStream);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                } finally {
                    safeClose(outputStream);
                }
            } finally {
                IoUtils.safeClose(inputStream);
            }

        } else {
            HttpHandlers.executeHandler(ResponseCodeHandler.HANDLE_404, exchange);
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
        df.setTimeZone(TimeZone.getTimeZone(Common.GMT));
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

        if(null==contentType) {
            contentType = Common.APPLICATION_OCTET_STREAM;
        }

        return contentType;
    }

    private ClassLoader getLoader() {
            return loader;
    }

    protected static ClassLoader getClassLoader(final ModuleLoader moduleLoader, final String module, final String slot) throws ModuleLoadException {
        ModuleIdentifier id = ModuleIdentifier.create(module, slot);
        ClassLoader cl = moduleLoader.loadModule(id).getClassLoader();

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
