/*
 * JBoss, Home of Professional Open Source
 * Copyright 2017, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.jsf.doctype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.jsf.subsystem.JSFExtension;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for disallowing and allowing DOCTYPE declarations in JSF apps.
 *
 * @author Farah Juma
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DoctypeDeclTestCase {
    @ArquillianResource
    private URL url;

    @ArquillianResource
    private ManagementClient managementClient;

    private final Pattern viewStatePattern = Pattern.compile("id=\".*javax.faces.ViewState.*\" value=\"([^\"]*)\"");

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "jsf-test.war");
        war.addPackage(DoctypeDeclTestCase.class.getPackage());
        // register.xhtml contains a DOCTYPE declaration
        war.addAsWebResource(DoctypeDeclTestCase.class.getPackage(), "register.xhtml", "register.xhtml");
        war.addAsWebResource(DoctypeDeclTestCase.class.getPackage(), "confirmation.xhtml", "confirmation.xhtml");
        war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }

    @Test
    public void testDoctypeDeclDisallowed() throws Exception {
        writeDisallowDoctypeDeclAttributeAndReload(true);
        // ensure an internal server error occurs
        register("Bob", 500);
        undefineDisallowDoctypeDeclAttributeAndReload();
    }

    @Test
    public void testDoctypeDeclAllowed() throws Exception {
        writeDisallowDoctypeDeclAttributeAndReload(false);
        String responseString = register("Bob", 200);
        assertTrue(responseString.contains("registered"));
        undefineDisallowDoctypeDeclAttributeAndReload();
    }

    private String register(String name, int expectedStatusCode) throws Exception {
        DefaultHttpClient client = new DefaultHttpClient();

        try {
            // Create and execute a GET request
            String jsfViewState = null;
            String requestUrl = url.toString() + "register.jsf";
            HttpGet getRequest = new HttpGet(requestUrl);
            HttpResponse response = client.execute(getRequest);
            try {
                // Get the JSF view state
                String responseString = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
                Matcher jsfViewMatcher = viewStatePattern.matcher(responseString);
                if (jsfViewMatcher.find()) {
                    jsfViewState = jsfViewMatcher.group(1);
                }
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // Create and execute a POST request with the given name
            HttpPost post = new HttpPost(requestUrl);
            List<NameValuePair> list = new ArrayList<NameValuePair>();
            list.add(new BasicNameValuePair("javax.faces.ViewState", jsfViewState));
            list.add(new BasicNameValuePair("register", "register"));
            list.add(new BasicNameValuePair("register:inputName", name));
            list.add(new BasicNameValuePair("register:registerButton", "Register"));
            post.setEntity(new StringEntity(URLEncodedUtils.format(list, "UTF-8"), ContentType.APPLICATION_FORM_URLENCODED));
            response = client.execute(post);

            try {
                assertEquals(expectedStatusCode, response.getStatusLine().getStatusCode());
                return IOUtils.toString(response.getEntity().getContent(), "UTF-8");
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
        } finally {
            HttpClientUtils.closeQuietly(client);
        }
    }

    private void writeDisallowDoctypeDeclAttributeAndReload(boolean value) throws Exception {
        final ModelNode address = Operations.createAddress(ClientConstants.SUBSYSTEM, JSFExtension.SUBSYSTEM_NAME);
        final ModelNode op = Operations.createWriteAttributeOperation(address, "disallow-doctype-decl", value);
        ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
        ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
    }

    private void undefineDisallowDoctypeDeclAttributeAndReload() throws Exception {
        final ModelNode address = Operations.createAddress(ClientConstants.SUBSYSTEM, JSFExtension.SUBSYSTEM_NAME);
        final ModelNode op = Operations.createUndefineAttributeOperation(address, "disallow-doctype-decl");
        ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
        ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
    }
}
