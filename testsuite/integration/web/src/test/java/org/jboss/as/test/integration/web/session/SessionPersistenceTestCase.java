/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.session;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 */
@RunWith(Arquillian.class)
@RunAsClient
@Ignore
public class SessionPersistenceTestCase {

    @ArquillianResource
    public Deployer deployer;


    @Deployment(name = "web", managed = false, testable = false)
    public static Archive<?> dependent() {
        return ShrinkWrap.create(WebArchive.class, "sessionPersistence.war")
                .addClasses(SessionTestServlet.class);
    }

    @Test
    public void testLifeCycle() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet get = new HttpGet("http://" + TestSuiteEnvironment.getServerAddress() + ":8080/sessionPersistence/SessionPersistenceServlet");
            deployer.deploy("web");
            String result = runGet(get, client);
            assertEquals("0", result);
            result = runGet(get, client);
            assertEquals("1", result);
            result = runGet(get, client);
            assertEquals("2", result);
            deployer.undeploy("web");
            deployer.deploy("web");
            result = runGet(get, client);
            assertEquals("3", result);
            result = runGet(get, client);
            assertEquals("4", result);
        }
    }

    private String runGet(HttpGet get, HttpClient client) throws IOException {
        HttpResponse res = client.execute(get);
        return EntityUtils.toString(res.getEntity());
    }
}
