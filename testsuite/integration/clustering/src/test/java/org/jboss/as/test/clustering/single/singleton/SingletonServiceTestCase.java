/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.single.singleton;

import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.NODE_1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.URL;
import java.time.Duration;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.cluster.singleton.service.ElectedMemberServiceServlet;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Validates that a singleton service works in a non-clustered environment.
 * @author Paul Ferraro
 */
@ExtendWith(ArquillianExtension.class)
public class SingletonServiceTestCase {
    private static final String MODULE_NAME = SingletonServiceTestCase.class.getSimpleName();
    private static final ConditionFactory AWAIT = Awaitility.await().atMost(TimeoutUtil.adjust(Duration.ofSeconds(15))).pollInterval(Duration.ofMillis(100)).pollInSameThread();

    @Deployment(testable = false)
    public static Archive<?> deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, MODULE_NAME + ".war");
        war.addPackage(ElectedMemberServiceServlet.class.getPackage());
        war.addClass(ElectedMemberServiceActivator.class);
        war.addAsServiceProvider(org.jboss.msc.service.ServiceActivator.class, ElectedMemberServiceActivator.class);
        return war;
    }

    @Test
    void singletonService(@ArquillianResource(ElectedMemberServiceServlet.class) URL baseURL) throws Exception {

        // URLs look like "http://IP:PORT/singleton/service"
        URI defaultURI = ElectedMemberServiceServlet.createURI(baseURL, org.jboss.as.test.clustering.cluster.singleton.service.ElectedMemberServiceActivator.DEFAULT_SERVICE_NAME);
        URI quorumURI = ElectedMemberServiceServlet.createURI(baseURL, org.jboss.as.test.clustering.cluster.singleton.service.ElectedMemberServiceActivator.QUORUM_SERVICE_NAME);

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            AWAIT.untilAsserted(() -> {
                try (CloseableHttpResponse response = client.execute(new HttpGet(defaultURI))) {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    assertTrue(response.containsHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER));
                    assertEquals(NODE_1, response.getFirstHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER).getValue());
                }
            });

            AWAIT.untilAsserted(() -> {
                // Service should be started regardless of whether a quorum was required.
                try (CloseableHttpResponse response = client.execute(new HttpGet(quorumURI))) {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    assertTrue(response.containsHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER));
                    assertEquals(NODE_1, response.getFirstHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER).getValue());
                }
            });
        }
    }
}
