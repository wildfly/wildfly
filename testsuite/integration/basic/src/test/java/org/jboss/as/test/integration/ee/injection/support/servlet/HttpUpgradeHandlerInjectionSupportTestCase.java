/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.support.servlet;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.ee.injection.support.InjectionSupportTestCase;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * The code of this test is largely based on the <a
 * href="https://weblogs.java.net/blog/swchan2/archive/2013/05/07/protocol-upgrade-servlet-31-example">example</a> by Servlet 3.1 spec lead Shing Wai Chan.
 *
 * @author Martin Kouba
 */
@RunAsClient
@RunWith(Arquillian.class)
public class HttpUpgradeHandlerInjectionSupportTestCase extends InjectionSupportTestCase {

    private static final String CRLF = "\r\n";

    @Deployment
    public static WebArchive createTestArchive() {
        return createTestArchiveBase().addClasses(TestHttpUpgradeHandler.class, TestReadListener.class, TestUpgradeServlet.class);
    }

    @Test
    public void testInjectionSupport() throws IOException, ExecutionException, TimeoutException {

        String host;
        String contextRoot;
        int port;
        Socket socket = null;
        BufferedReader in = null;
        BufferedWriter out = null;
        String response;

        Matcher matcher = Pattern.compile("http://(.*):(\\d{1,5})/(.*)").matcher(contextPath.toString());
        if (matcher.find()) {
            host = matcher.group(1);
            port = Integer.valueOf(matcher.group(2));
            contextRoot = matcher.group(3);
        } else {
            throw new AssertionError("Cannot parse the test archive URL");
        }

        try {

            socket = new Socket(host, port);
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

            // Initial HTTP upgrade request
            out.write("GET /" + contextRoot + "TestUpgradeServlet HTTP/1.1" + CRLF);
            out.write("Host: " + host + ":" + port + CRLF);
            out.write("Upgrade: foo" + CRLF);
            out.write("Connection: Upgrade" + CRLF);
            out.write(CRLF);
            out.flush();

            // Receive the protocol upgrade response
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            String line = null;
            while ((line = in.readLine()) != null) {
                if ("".equals(line)) {
                    break;
                }
            }

            // Send dummy request
            out.write("dummy request#");
            out.flush();

            // Receive the dummy response
            StringBuilder buffer = new StringBuilder();
            while (!(line = in.readLine()).equals("END")) {
                buffer.append(line);
            }
            response = buffer.toString();

        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        assertTrue(response.contains("isPostConstructCallbackInvoked: true"));
        assertTrue(response.contains("isInterceptorInvoked: true"));
        assertTrue(response.contains("isInjectionOk: true"));
    }

}