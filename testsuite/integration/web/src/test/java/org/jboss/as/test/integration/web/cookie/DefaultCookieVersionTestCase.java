/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.web.cookie;

import java.net.URL;
import java.util.Arrays;
import java.util.Locale;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test case for cookie
 *
 * @author prabhat.jha@jboss.com
 * @author lbarreiro@redhat.com
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(DefaultCookieVersionTestCase.CookieVersionServerSetup.class)
public class DefaultCookieVersionTestCase {
    @ArquillianResource(CookieServlet.class)
    protected URL cookieURL;

    static class CookieVersionServerSetup implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {
            ModelNode modelNode = new ModelNode();
            modelNode.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
            modelNode.get(ModelDescriptionConstants.OP_ADDR).set(PathAddress.parseCLIStyleAddress("/subsystem=undertow/servlet-container=default").toModelNode());
            modelNode.get(ModelDescriptionConstants.NAME).set("default-cookie-version");
            modelNode.get(ModelDescriptionConstants.VALUE).set(1);
            ModelNode reuslt = managementClient.getControllerClient().execute(modelNode);
            ServerReload.executeReloadAndWaitForCompletion(managementClient);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String s) throws Exception {

            ModelNode modelNode = new ModelNode();
            modelNode.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION);
            modelNode.get(ModelDescriptionConstants.OP_ADDR).set(PathAddress.parseCLIStyleAddress("/subsystem=undertow/servlet-container=default").toModelNode());
            modelNode.get(ModelDescriptionConstants.NAME).set("default-cookie-version");
            ModelNode reuslt = managementClient.getControllerClient().execute(modelNode);
            ServerReload.executeReloadAndWaitForCompletion(managementClient);
        }
    }

    @Deployment
    public static WebArchive deployment() {

        WebArchive war = ShrinkWrap.create(WebArchive.class, "jbosstest-cookie.war");
        war.addClass(CookieReadServlet.class);
        war.addClass(CookieServlet.class);

        return war;
    }

    @Test
    public void testDefaultCookieVersion() throws Exception {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpResponse response = httpclient.execute(new HttpGet(cookieURL.toURI() + "CookieServlet"));
            if (response.getEntity() != null) {
                response.getEntity().getContent().close();
            }
            response = httpclient.execute(new HttpPost(cookieURL.toURI() + "CookieServlet"));
            Header[] cookies = response.getHeaders("set-cookie");

            for (Header i : cookies) {
                String value = i.getValue();
                Assert.assertTrue(value + Arrays.toString(cookies), value.toLowerCase(Locale.ENGLISH).contains("version=1"));
            }
        }
    }

}
