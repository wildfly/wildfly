/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.management.http;

import static org.jboss.as.controller.PathAddress.pathAddress;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESOURCE_ADDED_NOTIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.test.integration.management.http.HttpClientUtils.delete;
import static org.jboss.as.test.integration.management.http.HttpClientUtils.get;
import static org.jboss.as.test.integration.management.http.HttpClientUtils.post;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class HttpNotificationManagementTestCase {

    private static final int MANAGEMENT_PORT = 9990;
    private static final String NOTIFICATION_CONTEXT = "/notification";

    @ArquillianResource
    URL url;

    @ContainerResource
    private ManagementClient managementClient;

    @Deployment
    public static Archive<?> getDeployment() {
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "dummy.jar");
        ja.addClass(HttpNotificationManagementTestCase.class);
        return ja;
    }

    @Test
    public void testRegisterNotificationHandler() throws Exception {

        DefaultHttpClient client = HttpClientUtils.create(url.getHost(), MANAGEMENT_PORT);

        URL notificationURL = new URL(url.getProtocol(), url.getHost(), MANAGEMENT_PORT, NOTIFICATION_CONTEXT);
        //register a notification handler for /subsystem=logging/logger=* resources
        ModelNode resources = new ModelNode();
        resources.get("resources").get(0).add("subsystem", "logging");
        resources.get("resources").get(0).add("logger", "*");
        HttpResponse response = post(client, notificationURL, resources, true);
        assertEquals(201, response.getStatusLine().getStatusCode());
        Header location = response.getFirstHeader("Location");
        assertNotNull(location);
        Header link = response.getFirstHeader("Link");
        assertNotNull(link);
        assertTrue(link.getValue().contains("rel=notifications"));

        // check the created handler
        URL handlerURL = new URL(url.getProtocol(), url.getHost(), MANAGEMENT_PORT, location.getValue());
        response = get(client, handlerURL, false);
        assertEquals(200, response.getStatusLine().getStatusCode());
        link = response.getFirstHeader("Link");
        assertNotNull(link);
        ModelNode addresses = ModelNode.fromJSONStream(response.getEntity().getContent());
        // compare path address to disambiguate model node address representations
        assertEquals(pathAddress(resources.get("resources").get(0)), pathAddress(addresses.get(0)));

        // delete the handler
        response = delete(client, handlerURL);
        assertEquals(204, response.getStatusLine().getStatusCode());

        // check the handler no longer exists
        response = get(client, handlerURL, true);
        assertEquals(404, response.getStatusLine().getStatusCode());
    }

    @Test
    public void testFetchNotifications() throws Exception {

        DefaultHttpClient client = HttpClientUtils.create(url.getHost(), MANAGEMENT_PORT);

        URL notificationURL = new URL(url.getProtocol(), url.getHost(), MANAGEMENT_PORT, NOTIFICATION_CONTEXT);
        //register a notification handler for /subsystem=logging/logger=* resources
        ModelNode resources = new ModelNode();
        resources.get("resources").get(0).add("subsystem", "logging");
        resources.get("resources").get(0).add("logger", "*");
        HttpResponse response = post(client, notificationURL, resources, true);
        assertEquals(201, response.getStatusLine().getStatusCode());
        Header location = response.getFirstHeader("Location");
        assertNotNull(location);
        Header link = response.getFirstHeader("Link");
        assertNotNull(link);
        assertTrue(link.getValue().contains("; rel=notifications"));
        String notificationsHandlerPath = link.getValue().substring(0, link.getValue().indexOf("; rel="));
        URL notificationsURL = new URL(url.getProtocol(), url.getHost(), MANAGEMENT_PORT, notificationsHandlerPath);
        System.out.println("notificationsURL = " + notificationsURL);

        response = post(client, notificationsURL, true);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertEquals(0, response.getEntity().getContentLength());

        ModelNode add = new ModelNode();
        add.get(OP_ADDR).add("subsystem", "logging");
        add.get(OP_ADDR).add("logger", "mylogger");
        add.get(OP).set(ADD);
        ModelNode result = managementClient.getControllerClient().execute(add);
        assertEquals(result.toJSONString(true), SUCCESS, result.get(OUTCOME).asString());

        response = post(client, notificationsURL, false);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertNotEquals(0, response.getEntity().getContentLength());
        ModelNode notifications = ModelNode.fromJSONStream(response.getEntity().getContent());
        assertNotNull(notifications);
        ModelNode notification = notifications.get(0);
        assertNotNull(notification);
        assertEquals(RESOURCE_ADDED_NOTIFICATION, notification.get(TYPE).asString());
        assertEquals(pathAddress(add.get(OP_ADDR)), pathAddress(notification.get("resource")));

        // check that fetching a second time the notifications will not return any
        response = post(client, notificationsURL, true);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertEquals(0, response.getEntity().getContentLength());
    }

}
