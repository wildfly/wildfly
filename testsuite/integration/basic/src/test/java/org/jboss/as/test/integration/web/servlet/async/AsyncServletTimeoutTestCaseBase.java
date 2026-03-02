package org.jboss.as.test.integration.web.servlet.async;

import java.io.IOException;
import java.net.URL;

import javax.naming.NamingException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.ServerSnapshot;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
public abstract class AsyncServletTimeoutTestCaseBase {
    public static final String DEPLOYMENT_NAME = "asynctimeout";
    public static final String DEPLOYMENT_NAME_WAR = DEPLOYMENT_NAME + ".war";

    protected AutoCloseable serverSnapshot;
    @ArquillianResource
    protected ManagementClient managementClient;
    @ArquillianResource
    protected URL url;

    @Deployment
    public static WebArchive deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME_WAR);
        war.addClass(AsyncLeash.class);
        war.addClass(AsyncLeashBean.class);
        war.addClass(AsyncNonDispatchServlet.class);
        return war;
    }

    @Before
    public void before() throws Exception {
        serverSnapshot = ServerSnapshot.takeSnapshot(managementClient);
    }

    @After
    public void after() throws Exception {
        serverSnapshot.close();
    }

    @Test
    public void testTimeout() throws IOException, InterruptedException, NamingException {
        final long customTimeout = getTimeout();
        setup();
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final HttpGet request = new HttpGet(getUrl());
            final HttpResponse response = httpClient.execute(request);
            Assert.assertEquals(500, response.getStatusLine().getStatusCode());
            final HttpEntity entity = response.getEntity();
            final String responseMessage = EntityUtils.toString(entity);
            AsyncLeash leash = AsyncNonDispatchServlet.lookupEJB();
            Assert.assertTrue(leash.initialized());
            Assert.assertTrue(leash.detail(), leash.isTimeout());
            Assert.assertTrue(leash.detail(), leash.timeoutTStamp() - leash.initTStamp() - customTimeout < 150);// 150 ms should
                                                                                                                // be enough?
        }
    }

    protected abstract void setup() throws IOException;

    protected abstract String getUrl();

    protected abstract long getTimeout();
}
