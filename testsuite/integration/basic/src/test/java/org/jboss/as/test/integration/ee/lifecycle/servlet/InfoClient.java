/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.lifecycle.servlet;

import java.net.URL;
import java.net.URLConnection;

public final class InfoClient {

    private static String infoContext = null;

    public static void setInfoContext(String infoContext) {
        InfoClient.infoContext = infoContext;
    }

    public static void notify(String event) {
        try {
            URLConnection connection = new URL(infoContext + "InfoServlet" + "?event=" + event).openConnection();
            connection.getInputStream().close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
