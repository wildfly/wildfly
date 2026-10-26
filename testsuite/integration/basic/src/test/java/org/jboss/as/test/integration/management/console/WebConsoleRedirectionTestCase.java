/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.management.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.undertow.util.Headers;

import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.client.methods.HttpGet;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.TestSuiteEnvironment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Tomas Hofman (thofman@redhat.com)
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class WebConsoleRedirectionTestCase {

    @SuppressWarnings("unused")
    @ContainerResource
    private ManagementClient managementClient;

    @Test
    public void testRedirectionInAdminMode() throws Exception {
        ServerReload.executeReloadAndWaitForCompletion(managementClient, true);
        try {
            final HttpURLConnection connection = getConnection();
            assertEquals(HttpURLConnection.HTTP_MOVED_TEMP, connection.getResponseCode());
            String location = connection.getHeaderFields().get(Headers.LOCATION_STRING).get(0);
            assertEquals("/consoleerror/noConsoleForAdminModeError.html", location);
        } finally {
            ServerReload.executeReloadAndWaitForCompletion(managementClient, false);
        }
    }

    @Test
    public void testRedirectionInNormalMode() throws Exception {
        final HttpURLConnection connection = getConnection();
        assertEquals(HttpURLConnection.HTTP_MOVED_TEMP, connection.getResponseCode());
        String location = connection.getHeaderFields().get(Headers.LOCATION_STRING).get(0);
        assertEquals("/console/index.html", location);
    }

    private HttpURLConnection getConnection() throws Exception {
        final URL url = new URL("http://" + TestSuiteEnvironment.getServerAddress() + ":" + TestSuiteEnvironment.getServerPort() + "/");
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        assertNotNull(connection);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod(HttpGet.METHOD_NAME);
        connection.connect();
        return connection;
    }

}
