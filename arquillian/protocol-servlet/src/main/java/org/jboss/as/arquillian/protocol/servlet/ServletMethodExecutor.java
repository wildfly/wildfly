/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.arquillian.protocol.servlet;

import java.io.ObjectInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import org.jboss.arquillian.container.test.spi.ContainerMethodExecutor;
import org.jboss.arquillian.test.spi.TestMethodExecutor;
import org.jboss.arquillian.test.spi.TestResult;

/**
 * ServletMethodExecutor
 *
 * @author <a href="mailto:aslak@conduct.no">Aslak Knutsen</a>
 * @version $Revision: $
 */
public class ServletMethodExecutor implements ContainerMethodExecutor {
    private URL baseURL;

    public ServletMethodExecutor(URL baseURL) {
        this.baseURL = baseURL;
    }

    public TestResult invoke(TestMethodExecutor testMethodExecutor) {
        if (testMethodExecutor == null) {
            throw new IllegalArgumentException("TestMethodExecutor must be specified");
        }

        Class<?> testClass = testMethodExecutor.getInstance().getClass();
        WebContext context = testClass.getAnnotation(WebContext.class);
        String ctx = context != null ? context.value() : "test";
        String url = baseURL.toExternalForm() + ctx + "/ArquillianServletRunner" + "?outputMode=serializedObject&className="
                + testClass.getName() + "&methodName=" + testMethodExecutor.getMethod().getName();

        try {
            return execute(url);
        } catch (Exception e) {
            throw new IllegalStateException("Error launching test " + testClass.getName() + " " + testMethodExecutor.getMethod(),
                    e);
        }
    }

    private TestResult execute(String url) throws Exception {
        long timeoutTime = System.currentTimeMillis() + 1000;
        boolean interrupted = false;
        while (timeoutTime > System.currentTimeMillis()) {
            URLConnection connection = new URL(url).openConnection();
            if (!(connection instanceof HttpURLConnection)) {
                throw new IllegalStateException("Not an http connection! " + connection);
            }
            HttpURLConnection httpConnection = (HttpURLConnection) connection;
            httpConnection.setUseCaches(false);
            httpConnection.setDefaultUseCaches(false);
            try {
                httpConnection.connect();
                if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    ObjectInputStream ois = new ObjectInputStream(httpConnection.getInputStream());
                    Object o;
                    try {
                        o = ois.readObject();
                    } finally {
                        ois.close();
                    }

                    if (!(o instanceof TestResult)) {
                        throw new IllegalStateException("Error reading test results - expected a TestResult but got " + o);
                    }
                    return (TestResult) o;
                } else if (httpConnection.getResponseCode() != HttpURLConnection.HTTP_NOT_FOUND) {
                    throw new IllegalStateException("Error launching test at " + url + ". " + "Got "
                            + httpConnection.getResponseCode() + " (" + httpConnection.getResponseMessage() + ")");
                }
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            } finally {
                httpConnection.disconnect();
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        throw new IllegalStateException("Error launching test at " + url + ". Kept on getting 404s.");
    }
}
