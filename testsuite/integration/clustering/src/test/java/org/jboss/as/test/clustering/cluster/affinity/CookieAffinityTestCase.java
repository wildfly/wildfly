/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.affinity;

import java.net.URI;
import java.net.URL;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.clustering.ClusterTestUtil;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.single.web.Mutable;
import org.jboss.as.test.clustering.single.web.SimpleServlet;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.as.test.shared.ManagementServerSetupTask;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test to verify that a custom affinity cookie can be configured and be used to drive session affinity.
 * Works synthetically without a load-balancer.
 *
 * @author Radoslav Husar
 * @see <a href="https://issues.redhat.com/browse/WFLY-16043">WFLY-16043</a>
 */
@RunWith(Arquillian.class)
@ServerSetup(CookieAffinityTestCase.ServerSetupTask.class)
public class CookieAffinityTestCase extends AbstractClusteringTestCase {

    private static final String MODULE_NAME = CookieAffinityTestCase.class.getSimpleName();
    private static final String DEPLOYMENT_NAME = MODULE_NAME + ".war";
    private static final String AFFINITY_COOKIE_NAME = "custom-cookie-name";

    public CookieAffinityTestCase() {
        super(NODE_1_2);
    }

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> deployment1() {
        return deployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> deployment2() {
        return deployment();
    }

    private static Archive<?> deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME)
                .addClasses(SimpleServlet.class, Mutable.class)
                .setWebXML(SimpleServlet.class.getPackage(), "web.xml");
        ClusterTestUtil.addTopologyListenerDependencies(war);
        return war;
    }

    protected static String parseCookieAffinity(HttpResponse response) {
        for (Header header : response.getAllHeaders()) {
            if (!header.getName().equals("Set-Cookie")) continue;

            String setCookieValue = header.getValue();

            String cookieName = setCookieValue.substring(0, setCookieValue.indexOf('='));
            if (cookieName.equals(AFFINITY_COOKIE_NAME)) {
                return setCookieValue.substring(setCookieValue.indexOf('=') + 1, setCookieValue.indexOf(';'));
            }
        }

        return null;
    }

    /**
     * Test routing by
     * (1) creating a session on a node 1 - verify route is set
     * (2) verify subsequent request
     * (3) verify requesting same session on a different node that correct affinity is overridden.
     */
    @Test
    public void test(@ArquillianResource() @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
                     @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2) throws Exception {

        URI uri1 = SimpleServlet.createURI(baseURL1);
        URI uri2 = SimpleServlet.createURI(baseURL2);

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {

            int value = 1;
            String sessionAffinity;

            // 1 -> node 1
            HttpResponse response = client.execute(new HttpGet(uri1));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(value++, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));

                String affinity = parseCookieAffinity(response);
                log.infof("Response #1: %s; affinity=%s", response, affinity);
                Assert.assertNotNull(affinity);

                sessionAffinity = affinity;
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // 2
            response = client.execute(new HttpGet(uri1));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(value++, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));

                String affinity = parseCookieAffinity(response);
                log.infof("Response #2: %s; affinity=%s", response, affinity);

                Assert.assertNotNull(affinity);
                Assert.assertEquals(sessionAffinity, affinity);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // 3 -> node 2
            response = client.execute(new HttpGet(uri2));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(value++, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));

                String affinity = parseCookieAffinity(response);
                log.infof("Response #3: %s; affinity=%s", response, affinity);

                Assert.assertNotNull(affinity);
                Assert.assertEquals(sessionAffinity, affinity);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
        }
    }

    static class ServerSetupTask extends ManagementServerSetupTask {
        ServerSetupTask() {
            super(NODE_1_2, createContainerConfigurationBuilder()
                    .setupScript(createScriptBuilder()
                            .startBatch()
                            .add("/subsystem=undertow/servlet-container=default/setting=affinity-cookie:add(name=%s)", AFFINITY_COOKIE_NAME)
                            .endBatch()
                            .build())
                    .tearDownScript(createScriptBuilder()
                            .startBatch()
                            .add("/subsystem=undertow/servlet-container=default/setting=affinity-cookie:remove()")
                            .endBatch()
                            .build())
                    .build());
        }
    }

}
