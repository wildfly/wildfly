/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat Middleware LLC, and individual contributors
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

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Locale;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test case for default cookie version configuration.
 *
 * @author Stuart Douglas
 * @author Jan Stourac
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(SnapshotRestoreSetupTask.class)
public class DefaultCookieVersionTestCase {
    @ArquillianResource(SimpleCookieServlet.class)
    protected URL cookieURL;

    private static String DEF_SERVLET_ADDR = "/subsystem=undertow/servlet-container=default";

    @Deployment
    public static WebArchive deployment() {

        WebArchive war = ShrinkWrap.create(WebArchive.class, "jbosstest-cookie.war");
        war.addClass(SimpleCookieServlet.class);
        war.addClass(CookieEchoServlet.class);

        return war;
    }

    @ContainerResource
    private static ManagementClient managementClient;

    @Test
    public void testDefaultCookieVersion0() throws Exception {
        commonDefaultCookieVersion(0);
    }

    @Test
    public void testDefaultCookieVersion1() throws Exception {
        commonDefaultCookieVersion(1);
    }

    @Test
    public void testSendCookieVersion0() throws Exception {
        commonSendCookieVersion(0);
    }

    @Test
    public void testSendCookieVersion1() throws Exception {
        commonSendCookieVersion(1);
    }

    private void commonDefaultCookieVersion(int cookieVersion) throws IOException, URISyntaxException {
        configureDefaultCookieVersion(cookieVersion);

        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpResponse response = httpclient.execute(new HttpGet(cookieURL.toURI() + "SimpleCookieServlet"));
            if (response.getEntity() != null) {
                response.getEntity().getContent().close();
            }
            response = httpclient.execute(new HttpPost(cookieURL.toURI() + "SimpleCookieServlet"));
            Header[] cookies = response.getHeaders("set-cookie");

            Assert.assertTrue(cookies.length > 0);

            for (Header i : cookies) {
                String value = i.getValue();
                if (cookieVersion >= 1) {
                    Assert.assertTrue(value + Arrays.toString(cookies), value.toLowerCase(Locale.ENGLISH).contains
                            ("version=" + cookieVersion));
                } else {
                    Assert.assertFalse(value + Arrays.toString(cookies), value.toLowerCase(Locale.ENGLISH).contains
                            ("version"));
                }
            }
        }
    }

    private void commonSendCookieVersion(int cookieVersion) throws IOException, URISyntaxException {
        configureDefaultCookieVersion(cookieVersion);

        BasicCookieStore basicCookieStore = new BasicCookieStore();
        BasicClientCookie cookie = new BasicClientCookie("testCookie", "testCookieValue");
        cookie.setVersion(cookieVersion);
        cookie.setDomain(cookieURL.getHost());
        basicCookieStore.addCookie(cookie);

        try (CloseableHttpClient httpclient = HttpClientBuilder.create().setDefaultCookieStore(basicCookieStore)
                .build()) {
            HttpResponse response = httpclient.execute(new HttpGet(cookieURL.toURI() + "CookieEchoServlet"));
            if (response.getEntity() != null) {
                Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(cookieVersion + "", EntityUtils.toString(response.getEntity()));
            }
        }
    }

    private static void configureDefaultCookieVersion(Integer cookieVer) throws IOException {
        ModelNode modelNode = new ModelNode();

        modelNode.get(ModelDescriptionConstants.OP_ADDR).set(PathAddress.parseCLIStyleAddress(DEF_SERVLET_ADDR)
                .toModelNode());
        modelNode.get(ModelDescriptionConstants.NAME).set("default-cookie-version");

        if (cookieVer != null) {
            modelNode.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
            modelNode.get(ModelDescriptionConstants.VALUE).set(cookieVer);
        } else {
            modelNode.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION);
        }

        ModelNode opRes = managementClient.getControllerClient().execute(modelNode);
        Assert.assertEquals(opRes.toString(), "success", opRes.get(ModelDescriptionConstants.OUTCOME).asString());
        ServerReload.executeReloadAndWaitForCompletion(managementClient);
    }

}
