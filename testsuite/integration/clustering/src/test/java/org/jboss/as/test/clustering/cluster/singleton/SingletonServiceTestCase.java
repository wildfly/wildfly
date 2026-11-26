/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.singleton;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URL;
import java.time.Duration;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.singleton.service.ElectedMemberServiceActivator;
import org.jboss.as.test.clustering.cluster.singleton.service.ElectedMemberServiceServlet;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ArquillianExtension.class)
public class SingletonServiceTestCase extends AbstractClusteringTestCase {
    private static final ConditionFactory AWAIT = Awaitility.await().atMost(TimeoutUtil.adjust(Duration.ofSeconds(15))).pollInterval(Duration.ofMillis(100)).pollInSameThread();
    private static final String MODULE_NAME = SingletonServiceTestCase.class.getSimpleName();

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> deployment1() {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> deployment2() {
        return createDeployment();
    }

    private static Archive<?> createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, MODULE_NAME + ".war");
        war.addPackage(ElectedMemberServiceServlet.class.getPackage());
        war.addAsServiceProvider(org.jboss.msc.service.ServiceActivator.class, ElectedMemberServiceActivator.class);
        return war;
    }

    @Test
    void singletonService(
            @ArquillianResource(ElectedMemberServiceServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource(ElectedMemberServiceServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2,
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_1) ManagementClient client1,
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_2) ManagementClient client2)
            throws Exception {

        // Needed to be able to inject ArquillianResource
        stop(NODE_2);

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            AWAIT.untilAsserted(() -> {
                try (CloseableHttpResponse response = client.execute(new HttpGet(ElectedMemberServiceServlet.createURI(baseURL1, ElectedMemberServiceActivator.DEFAULT_SERVICE_NAME, NODE_1)))) {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    assertTrue(response.containsHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER));
                    assertEquals(NODE_1, response.getFirstHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER).getValue());
                }
            });

            AWAIT.untilAsserted(() -> {
                try (CloseableHttpResponse response = client.execute(new HttpGet(ElectedMemberServiceServlet.createURI(baseURL1, ElectedMemberServiceActivator.QUORUM_SERVICE_NAME)))) {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    assertFalse(response.containsHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER));
                }
            });

            start(NODE_2);

            AWAIT.untilAsserted(() -> {
                try (CloseableHttpResponse response = client.execute(new HttpGet(ElectedMemberServiceServlet.createURI(baseURL1, ElectedMemberServiceActivator.DEFAULT_SERVICE_NAME, NODE_1)))) {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    assertTrue(response.containsHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER));
                    assertEquals(NODE_2, response.getFirstHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER).getValue());
                }
            });

            AWAIT.untilAsserted(() -> {
                try (CloseableHttpResponse response = client.execute(new HttpGet(ElectedMemberServiceServlet.createURI(baseURL1, ElectedMemberServiceActivator.QUORUM_SERVICE_NAME, NODE_2)))) {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    assertTrue(response.containsHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER));
                    assertEquals(NODE_2, response.getFirstHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER).getValue());
                }
            });

            AWAIT.untilAsserted(() -> {
                try (CloseableHttpResponse response = client.execute(new HttpGet(ElectedMemberServiceServlet.createURI(baseURL2, ElectedMemberServiceActivator.DEFAULT_SERVICE_NAME, NODE_2)))) {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    assertTrue(response.containsHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER));
                    assertEquals(NODE_2, response.getFirstHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER).getValue());
                }
            });

            AWAIT.untilAsserted(() -> {
                try (CloseableHttpResponse response = client.execute(new HttpGet(ElectedMemberServiceServlet.createURI(baseURL2, ElectedMemberServiceActivator.QUORUM_SERVICE_NAME, NODE_2)))) {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    assertTrue(response.containsHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER));
                    assertEquals(NODE_2, response.getFirstHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER).getValue());
                }
            });

            stop(NODE_2);

            AWAIT.untilAsserted(() -> {
                try (CloseableHttpResponse response = client.execute(new HttpGet(ElectedMemberServiceServlet.createURI(baseURL1, ElectedMemberServiceActivator.DEFAULT_SERVICE_NAME, NODE_1)))) {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    assertTrue(response.containsHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER));
                    assertEquals(NODE_1, response.getFirstHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER).getValue());
                }
            });

            AWAIT.untilAsserted(() -> {
                try (CloseableHttpResponse response = client.execute(new HttpGet(ElectedMemberServiceServlet.createURI(baseURL1, ElectedMemberServiceActivator.QUORUM_SERVICE_NAME)))) {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    assertFalse(response.containsHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER));
                }
            });

            start(NODE_2);

            AWAIT.untilAsserted(() -> {
                try (CloseableHttpResponse response = client.execute(new HttpGet(ElectedMemberServiceServlet.createURI(baseURL1, ElectedMemberServiceActivator.DEFAULT_SERVICE_NAME, NODE_2)))) {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    assertTrue(response.containsHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER));
                    assertEquals(NODE_2, response.getFirstHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER).getValue());
                }
            });

            AWAIT.untilAsserted(() -> {
                try (CloseableHttpResponse response = client.execute(new HttpGet(ElectedMemberServiceServlet.createURI(baseURL1, ElectedMemberServiceActivator.QUORUM_SERVICE_NAME, NODE_2)))) {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    assertTrue(response.containsHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER));
                    assertEquals(NODE_2, response.getFirstHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER).getValue());
                }
            });

            AWAIT.untilAsserted(() -> {
                try (CloseableHttpResponse response = client.execute(new HttpGet(ElectedMemberServiceServlet.createURI(baseURL2, ElectedMemberServiceActivator.DEFAULT_SERVICE_NAME, NODE_2)))) {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    assertTrue(response.containsHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER));
                    assertEquals(NODE_2, response.getFirstHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER).getValue());
                }
            });

            AWAIT.untilAsserted(() -> {
                try (CloseableHttpResponse response = client.execute(new HttpGet(ElectedMemberServiceServlet.createURI(baseURL2, ElectedMemberServiceActivator.QUORUM_SERVICE_NAME, NODE_2)))) {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    assertTrue(response.containsHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER));
                    assertEquals(NODE_2, response.getFirstHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER).getValue());
                }
            });

            stop(NODE_1);

            AWAIT.untilAsserted(() -> {
                try (CloseableHttpResponse response = client.execute(new HttpGet(ElectedMemberServiceServlet.createURI(baseURL2, ElectedMemberServiceActivator.DEFAULT_SERVICE_NAME, NODE_2)))) {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    assertTrue(response.containsHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER));
                    assertEquals(NODE_2, response.getFirstHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER).getValue());
                }
            });

            AWAIT.untilAsserted(() -> {
                try (CloseableHttpResponse response = client.execute(new HttpGet(ElectedMemberServiceServlet.createURI(baseURL2, ElectedMemberServiceActivator.QUORUM_SERVICE_NAME)))) {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    assertFalse(response.containsHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER));
                }
            });

            start(NODE_1);

            AWAIT.untilAsserted(() -> {
                try (CloseableHttpResponse response = client.execute(new HttpGet(ElectedMemberServiceServlet.createURI(baseURL1, ElectedMemberServiceActivator.DEFAULT_SERVICE_NAME, NODE_2)))) {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    assertTrue(response.containsHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER));
                    assertEquals(NODE_2, response.getFirstHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER).getValue());
                }
            });

            AWAIT.untilAsserted(() -> {
                try (CloseableHttpResponse response = client.execute(new HttpGet(ElectedMemberServiceServlet.createURI(baseURL1, ElectedMemberServiceActivator.QUORUM_SERVICE_NAME, NODE_2)))) {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    assertTrue(response.containsHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER));
                    assertEquals(NODE_2, response.getFirstHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER).getValue());
                }
            });

            AWAIT.untilAsserted(() -> {
                try (CloseableHttpResponse response = client.execute(new HttpGet(ElectedMemberServiceServlet.createURI(baseURL2, ElectedMemberServiceActivator.DEFAULT_SERVICE_NAME, NODE_2)))) {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    assertTrue(response.containsHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER));
                    assertEquals(NODE_2, response.getFirstHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER).getValue());
                }
            });

            AWAIT.untilAsserted(() -> {
                try (CloseableHttpResponse response = client.execute(new HttpGet(ElectedMemberServiceServlet.createURI(baseURL2, ElectedMemberServiceActivator.QUORUM_SERVICE_NAME, NODE_2)))) {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    assertTrue(response.containsHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER));
                    assertEquals(NODE_2, response.getFirstHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER).getValue());
                }
            });

            suspend(client2);

            try {
                AWAIT.untilAsserted(() -> {
                    try (CloseableHttpResponse response = client.execute(new HttpGet(ElectedMemberServiceServlet.createURI(baseURL1, ElectedMemberServiceActivator.DEFAULT_SERVICE_NAME, NODE_1)))) {
                        assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                        assertTrue(response.containsHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER));
                        assertEquals(NODE_1, response.getFirstHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER).getValue());
                    }
                });

                AWAIT.untilAsserted(() -> {
                    try (CloseableHttpResponse response = client.execute(new HttpGet(ElectedMemberServiceServlet.createURI(baseURL1, ElectedMemberServiceActivator.QUORUM_SERVICE_NAME)))) {
                        assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                        assertFalse(response.containsHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER));
                    }
                });
            } finally {
                resume(client2);
            }

            AWAIT.untilAsserted(() -> {
                try (CloseableHttpResponse response = client.execute(new HttpGet(ElectedMemberServiceServlet.createURI(baseURL1, ElectedMemberServiceActivator.DEFAULT_SERVICE_NAME, NODE_2)))) {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    assertTrue(response.containsHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER));
                    assertEquals(NODE_2, response.getFirstHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER).getValue());
                }
            });

            AWAIT.untilAsserted(() -> {
                try (CloseableHttpResponse response = client.execute(new HttpGet(ElectedMemberServiceServlet.createURI(baseURL1, ElectedMemberServiceActivator.QUORUM_SERVICE_NAME, NODE_2)))) {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    assertTrue(response.containsHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER));
                    assertEquals(NODE_2, response.getFirstHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER).getValue());
                }
            });

            AWAIT.untilAsserted(() -> {
                try (CloseableHttpResponse response = client.execute(new HttpGet(ElectedMemberServiceServlet.createURI(baseURL2, ElectedMemberServiceActivator.DEFAULT_SERVICE_NAME, NODE_2)))) {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    assertTrue(response.containsHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER));
                    assertEquals(NODE_2, response.getFirstHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER).getValue());
                }
            });

            AWAIT.untilAsserted(() -> {
                try (CloseableHttpResponse response = client.execute(new HttpGet(ElectedMemberServiceServlet.createURI(baseURL2, ElectedMemberServiceActivator.QUORUM_SERVICE_NAME, NODE_2)))) {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    assertTrue(response.containsHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER));
                    assertEquals(NODE_2, response.getFirstHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER).getValue());
                }
            });
       }
    }
}
