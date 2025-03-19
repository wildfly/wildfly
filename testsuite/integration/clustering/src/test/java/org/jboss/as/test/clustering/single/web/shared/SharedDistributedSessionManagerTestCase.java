/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.single.web.shared;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.single.web.SimpleServlet;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URI;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Test starts server with deployed an ear consisted of two war with Shared distributed session manager setup with session timeout
 * After send a get request and upon expiration, session destroy must be invoked only once and not two as the number of deployed war
 * Test for https://issues.redhat.com/browse/WFLY-18914.
 */
@RunWith(Arquillian.class)
public class SharedDistributedSessionManagerTestCase {

    private static final String MODULE = SharedDistributedSessionManagerTestCase.class.getSimpleName();
    private static final String MODULE_1 = "app1";
    private static final String MODULE_2 = "app2";

    @ArquillianResource
    URL url;

    @Deployment(testable = false)
    public static Archive<?> deployment() {

        WebArchive war1 = ShrinkWrap.create(WebArchive.class, MODULE_1 + ".war")
                .addClass(SessionDestroyCounter.class)
                .addClass(CustomHttpSessionListener.class)
                .addClass(CounterServlet.class)
                .setWebXML(SimpleServlet.class.getPackage(), "web.xml")
                .addAsWebResource(new StringAsset(""), "test.jsp");

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, MODULE_2 + ".war")
                .setWebXML(new StringAsset("<web-app></web-app>"))
                .addAsWebResource(new StringAsset(""), "test.jsp");

        return ShrinkWrap.create(EnterpriseArchive.class, MODULE + ".ear")
                .addAsModule(war1)
                .addAsModule(war2)
                .addAsManifestResource(SharedDistributedSessionManagerTestCase.class.getPackage(), "jboss-all-timeout.xml", "jboss-all.xml");
    }

    @Test
    public void testSessionDestroyedInvokesOnlyOnce() throws Exception {

        URI baseURI = new URI(url.toExternalForm() + "/" + MODULE_1);
        CloseableHttpClient client = HttpClientBuilder.create().build();
        try {
            client.execute(new HttpGet(baseURI + "/test.jsp"));

            //we need to wait 1 minute for session expiration
            TimeUnit.SECONDS.sleep(65);

            HttpResponse response = client.execute(new HttpGet(baseURI + "/count"));
            int sessionDestroyCount = Integer.parseInt(EntityUtils.toString(response.getEntity()));
            assertEquals("Session destroy invoked more than once due to " +
                    "https://issues.redhat.com/browse/WFLY-18914", 1, sessionDestroyCount);
        } finally {
            client.close();
        }
    }
}

