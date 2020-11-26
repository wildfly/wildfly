/*
 *
 *  * JBoss, Home of Professional Open Source.
 *  * Copyright 2020 Red Hat, Inc., and individual contributors
 *  * as indicated by the @author tags.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package org.wildfly.test.integration.microprofile.metrics.vendor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.microprofile.config.Config;

/**
 * Reads the management interface /metrics context during servlet context init. As that runs during deploy,
 * that means it runs during boot of a server with this deployment present. So this allows us
 * to check /metrics behavior during server boot. Also is a servlet the test client can invoke to retrieve
 * results.
 */
@WebServlet("BootCheckServlet")
@WebListener
public class BootCheckApplication extends HttpServlet implements ServletContextListener {

    private static final Logger logger = Logger.getLogger(BootCheckApplication.class.getPackage().getName());

    // Use statics for these because WildFly actually creates two instances of this class,
    // one for the ServletContextListener and one for the Servlet. So need statics to share data
    private static boolean cleanInit;
    private static boolean connected;
    private static int overallResponseCode;
    private static boolean overallSawVendor;
    private static int scopedResponseCode;
    private static boolean scopedSawVendor;

    // Use MP Config to provide the URL of the management socket.
    // Doing this turns this into a test of access to MP Config in a ServletContextListener contextInitialized() method.
    @Inject
    Config config;

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        logger.info("Initializing");

        try {
            //ServletConfig cfg = getServletConfig();
            //String managementURL = cfg.getInitParameter("managementURL");
            String managementURL = config.getValue("managementURL", String.class);

            long timeout = System.currentTimeMillis() + 20000;
            Exception failedConnectException = null;
            String overallResponseContent = null;
            do {
                connected = false;
                failedConnectException = null;
                Response overallResponse = null;
                try {
                    overallResponse = invoke(managementURL + "/metrics");
                } catch (Exception e) {
                    failedConnectException = e;
                }
                if (overallResponse != null) {
                    connected = true;
                    overallResponseCode = overallResponse.responseCode;
                    overallResponseContent = overallResponse.content;
                    if (overallResponseCode == 200) {
                        overallSawVendor = checkSawVendor(overallResponse.content, true);
                    }

                    // Now try a scoped request
                    Response scopedResponse = invoke(managementURL + "/metrics/vendor");
                    scopedResponseCode = scopedResponse.responseCode;
                    if (scopedResponseCode == 200) {
                        scopedSawVendor = checkSawVendor(scopedResponse.content, true);
                    }
                } else {
                    // Management socket may not be open yet; try again
                    //noinspection BusyWait
                    Thread.sleep(20);
                }
            } while ((!connected || overallResponseCode != 200 || overallResponseContent == null || overallResponseContent.isEmpty()) && System.currentTimeMillis() < timeout);

            if (failedConnectException != null) {
                // throw on the last failure in order to provide a diagnostic
                throw failedConnectException;
            } else if (overallResponseContent == null || overallResponseContent.isEmpty()) {
                throw new IllegalStateException("No overall response content available");
            }

            logger.info("Initialized");

            cleanInit = true;

        } catch (Throwable t) {
            // The test will fail because cleanInit=true won't be written
            // Just log the error here so the problem can be investigated
            logger.log(Level.SEVERE, "Failed initializing", t);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        logger.info("Handling GET");

        // Report what we saw during init()
        PrintWriter writer = resp.getWriter();
        writer.println("connected=" + connected);
        writer.println("cleanInit=" + cleanInit);
        if (cleanInit) {
            writer.println("overallResponseCode=" + overallResponseCode);
            writer.println("overallSawVendor=" + overallSawVendor);
            writer.println("scopedResponseCode=" + scopedResponseCode);
            writer.println("scopedSawVendor=" + scopedSawVendor);
        }
    }

    private Response invoke(final String url) throws IOException {

        HttpClientBuilder builder = HttpClients.custom();
        builder.setDefaultRequestConfig(RequestConfig.copy(RequestConfig.DEFAULT).setConnectTimeout(5000).build());
        builder.setDefaultSocketConfig(SocketConfig.copy(SocketConfig.DEFAULT).setSoTimeout(5000).build());
        try (CloseableHttpClient client = builder.build()) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("testSuite", "testSuitePassword"));
            HttpClientContext hcContext = HttpClientContext.create();
            hcContext.setCredentialsProvider(credentialsProvider);


            CloseableHttpResponse resp = client.execute(new HttpGet(url), hcContext);
            int respCode = resp.getStatusLine().getStatusCode();
            HttpEntity entity = resp.getEntity();
            String content = entity == null ? null : EntityUtils.toString(entity);
            resp.close();
            return new Response(respCode, content);
        }
    }

    private boolean checkSawVendor(String content, boolean log) throws IOException {

        try (Reader contentReader = new StringReader(content);
             BufferedReader reader = new BufferedReader(contentReader)) {
            List<String> cache = new ArrayList<>();
            boolean result = reader.lines().filter(cache::add).anyMatch(line ->
                    (line.startsWith("wildfly_undertow") || line.startsWith("jboss_undertow"))
                        && line.contains(MicroProfileVendorMetricsBootTestCase.FIRST));
            if (log) {
                for (String line: cache) {
                    logger.info(line);
                }
            }
            return result;
        }
    }

    private static class Response {
        private final int responseCode;
        private final String content;

        private Response(int responseCode, String content) {
            this.responseCode = responseCode;
            this.content = content;
        }
    }
}
