/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.elytron.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utility method for handling HTTP invocations.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class HttpUtil {

    public static String get(final String spec, final String username, final String password, final long timeout, final TimeUnit unit) throws IOException, TimeoutException {
        final Map<String, String> headers;
        if (username != null) {
            final String userpassword = username + ":" + password;
            final String basicAuthorization = java.util.Base64.getEncoder().encodeToString(userpassword.getBytes());
            headers = Collections.singletonMap("Authorization", "Basic " + basicAuthorization);
        } else {
            headers = Collections.emptyMap();
        }

        return get(spec, headers, timeout, unit, new AtomicReference<String>());
    }

    public static String get(final String spec, final Map<String, String> headers, final long timeout, final TimeUnit unit, final AtomicReference<String> sessionId) throws IOException, TimeoutException {
        final URL url = new URL(spec);
        Callable<String> task = new Callable<String>() {
            @Override
            public String call() throws IOException {
                final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                headers.entrySet().forEach(e -> conn.addRequestProperty(e.getKey(), e.getValue()));
                String cookie = sessionId.get();
                if (cookie != null) {
                    conn.addRequestProperty("Cookie", cookie);
                }
                conn.setDoInput(true);
                return processResponse(conn, sessionId);
            }
        };
        return execute(task, timeout, unit);
    }

    private static String execute(final Callable<String> task, final long timeout, final TimeUnit unit) throws TimeoutException, IOException {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final Future<String> result = executor.submit(task);
        try {
            return result.get(timeout, unit);
        } catch (TimeoutException e) {
            result.cancel(true);
            throw e;
        } catch (InterruptedException e) {
            // should not happen
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            // by virtue of the Callable redefinition above I can cast
            throw new IOException(e);
        } finally {
            executor.shutdownNow();
            try {
                executor.awaitTermination(timeout, unit);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    public static String read(final InputStream in) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            out.write(b);
        }
        return out.toString();
    }

    public static String processResponse(HttpURLConnection conn, AtomicReference<String> sessionId) throws IOException {
        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            final InputStream err = conn.getErrorStream();
            try {
                String response = err != null ? read(err) : null;
                throw new IOException(String.format("HTTP Status %d Response: %s", responseCode, response));
            } finally {
                if (err != null) {
                    err.close();
                }
            }
        }

        List<String> cookies = conn.getHeaderFields().get("Set-Cookie");
        if (cookies != null) {
            for (String cookie : cookies) {
                if (cookie.startsWith("JSESSIONID=")) {
                    sessionId.set(cookie.substring(0, cookie.indexOf(';')));
                    break;
                }
            }
        }

        final InputStream in = conn.getInputStream();
        try {
            return read(in);
        } finally {
            in.close();
        }
    }

}
