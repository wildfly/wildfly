/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.common;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.util.Base64;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class HttpRequest {
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
            throw (IOException) e.getCause();
        } finally {
            executor.shutdownNow();
            try {
                executor.awaitTermination(timeout, unit);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    public static String get(final String spec, final long timeout, final TimeUnit unit) throws IOException, ExecutionException, TimeoutException {
        final URL url = new URL(spec);
        Callable<String> task = new Callable<String>() {
            @Override
            public String call() throws Exception {
                final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true);
                return processResponse(conn);
            }
        };
        return execute(task, timeout, unit);
    }

    /**
     * Returns the URL response as a string.
     *
     * @param spec  URL spec
     * @param waitUntilAvailableMs  maximum timeout in milliseconds to wait for the URL to return non 404 response
     * @param responseTimeout  the timeout to read the response
     * @param responseTimeoutUnit  the time unit for responseTimeout
     * @return  URL response
     * @throws IOException
     * @throws ExecutionException
     * @throws TimeoutException
     */
    public static String get(final String spec, final long waitUntilAvailableMs, final long responseTimeout, final TimeUnit responseTimeoutUnit) throws IOException, ExecutionException, TimeoutException {
        final URL url = new URL(spec);
        Callable<String> task = new Callable<String>() {
            @Override
            public String call() throws Exception {
                final long startTime = System.currentTimeMillis();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true);
                while(conn.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                    if(System.currentTimeMillis() - startTime >= waitUntilAvailableMs) {
                        break;
                    }
                    try {
                        Thread.sleep(500);
                    } catch(InterruptedException e) {
                        break;
                    } finally {
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setDoInput(true);
                    }
                }
                return processResponse(conn);
            }
        };
        return execute(task, responseTimeout, responseTimeoutUnit);
    }

    public static String get(final String spec, final String username, final String password, final long timeout, final TimeUnit unit) throws IOException, TimeoutException {
        final URL url = new URL(spec);
        Callable<String> task = new Callable<String>() {
            @Override
            public String call() throws IOException {
                final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                if (username != null) {
                    final String userpassword = username + ":" + password;
                    final String basicAuthorization = Base64.encodeBytes(userpassword.getBytes());
                    conn.setRequestProperty("Authorization", "Basic " + basicAuthorization);
                }
                conn.setDoInput(true);
                return processResponse(conn);
            }
        };
        return execute(task, timeout, unit);
    }

    private static String read(final InputStream in) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        int b;
        while((b = in.read()) != -1) {
            out.write(b);
        }
        return out.toString();
    }

    private static String processResponse(HttpURLConnection conn) throws IOException {
        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            final InputStream err = conn.getErrorStream();
            try {
                throw new IOException(read(err));
            }
            finally {
                err.close();
            }
        }
        final InputStream in = conn.getInputStream();
        try {
            return read(in);
        }
        finally {
            in.close();
        }
    }

    public static String put(final String spec, final String message, final long timeout, final TimeUnit unit) throws MalformedURLException, ExecutionException, TimeoutException {
        return execRequestMethod(spec, message, timeout, unit, "PUT");
    }

    /**
     * Executes an HTTP request to write the specified message.
     *
     * @param spec The {@link URL} in String form
     * @param message Message to write
     * @param timeout Timeout value
     * @param unit Timeout units
     * @param requestMethod Name of the HTTP method to execute (ie. HEAD, GET, POST)
     * @return
     * @throws MalformedURLException
     * @throws ExecutionException
     * @throws TimeoutException
     */
    private static String execRequestMethod(final String spec, final String message, final long timeout, final TimeUnit unit, final String requestMethod) throws MalformedURLException, ExecutionException, TimeoutException {

        if(requestMethod==null||requestMethod.isEmpty()){
            throw new IllegalArgumentException("Request Method must be specified (ie. GET, PUT, DELETE etc)");
        }

        final URL url = new URL(spec);
        Callable<String> task = new Callable<String>() {
            @Override
            public String call() throws Exception {
                final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setRequestMethod(requestMethod);
                final OutputStream out = conn.getOutputStream();
                try {
                    write(out, message);
                    return processResponse(conn);
                }
                finally {
                    out.close();
                }
            }
        };
        try {
            return execute(task, timeout, unit);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String post(final String spec, final String message, final long timeout, final TimeUnit unit) throws MalformedURLException, ExecutionException, TimeoutException {
        return execRequestMethod(spec, message, timeout, unit, "POST");
    }

    public static String delete(final String spec, final String message, final long timeout, final TimeUnit unit) throws MalformedURLException, ExecutionException, TimeoutException {
        return execRequestMethod(spec, message, timeout, unit, "DELETE");
    }

    public static String head(final String spec, final String message, final long timeout, final TimeUnit unit) throws MalformedURLException, ExecutionException, TimeoutException {
        return execRequestMethod(spec, message, timeout, unit, "HEAD");
    }

    private static void write(OutputStream out, String message) throws IOException {
        final OutputStreamWriter writer = new OutputStreamWriter(out);
        writer.write(message);
        writer.flush();
    }
}
