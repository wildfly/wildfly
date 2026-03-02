/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.ejb.timer;

import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.ejb.timer.beans.AutoTimerBean;
import org.jboss.as.test.clustering.cluster.ejb.timer.beans.AutoTransientTimerBean;
import org.jboss.as.test.clustering.cluster.ejb.timer.beans.ManualTimerBean;
import org.jboss.as.test.clustering.cluster.ejb.timer.beans.SingleActionPersistentTimerBean;
import org.jboss.as.test.clustering.cluster.ejb.timer.beans.SingleActionTransientTimerBean;
import org.jboss.as.test.clustering.cluster.ejb.timer.beans.TimerBean;
import org.jboss.as.test.clustering.cluster.ejb.timer.servlet.TimerServlet;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Validates failover of distributed EJB timers.
 * @author Paul Ferraro
 */
@ExtendWith(ArquillianExtension.class)
public abstract class AbstractTimerServiceTestCase extends AbstractClusteringTestCase {

    protected static WebArchive createArchive(Class<? extends AbstractTimerServiceTestCase> testClass) {
        return ShrinkWrap.create(WebArchive.class, testClass.getSimpleName() + ".war")
                .addPackage(TimerServlet.class.getPackage())
                .addPackage(EJBDirectory.class.getPackage())
                .addPackage(TimerBean.class.getPackage())
                ;
    }

    private static final Duration GRACE_PERIOD = Duration.ofSeconds(TimeoutUtil.adjust(2));
    private final String moduleName;

    protected AbstractTimerServiceTestCase() {
        this.moduleName = this.getClass().getSimpleName();
    }

    @Test
    public void test(@ArquillianResource(TimerServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1, @ArquillianResource(TimerServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2) throws Exception {

        Map<String, URI> uris = Map.of(NODE_1, TimerServlet.createURI(baseURL1, this.moduleName), NODE_2, TimerServlet.createURI(baseURL2, this.moduleName));

        List<Class<? extends TimerBean>> singleActionTimerBeanClasses = List.of(SingleActionPersistentTimerBean.class, SingleActionTransientTimerBean.class);

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {

            TimeUnit.SECONDS.sleep(GRACE_PERIOD.getSeconds());

            // Create manual timers on node 1 only
            try (CloseableHttpResponse response = client.execute(new HttpPut(uris.get(NODE_1)))) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            }

            for (Map.Entry<String, URI> entry : uris.entrySet()) {
                try (CloseableHttpResponse response = client.execute(new HttpHead(entry.getValue()))) {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    for (Class<? extends TimerBean> beanClass : TimerServlet.TIMER_CLASSES) {
                        int count = Integer.parseInt(response.getFirstHeader(beanClass.getName()).getValue());
                        if (TimerServlet.MANUAL_TRANSIENT_TIMER_CLASSES.contains(beanClass) && entry.getKey().equals(NODE_2)) {
                            assertEquals(0, count, entry.getKey() + ": " + beanClass.getName());
                        } else {
                            assertEquals(1, count, entry.getKey() + ": " + beanClass.getName());
                        }
                    }
                }
            }

            TimeUnit.SECONDS.sleep(GRACE_PERIOD.getSeconds());

            Map<Class<? extends TimerBean>, Map<String, List<Instant>>> timeouts = new IdentityHashMap<>();
            for (Class<? extends TimerBean> beanClass : TimerServlet.TIMER_CLASSES) {
                timeouts.put(beanClass, new TreeMap<>());
            }

            for (Map.Entry<String, URI> entry : uris.entrySet()) {
                try (CloseableHttpResponse response = client.execute(new HttpGet(entry.getValue()))) {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    for (Map.Entry<Class<? extends TimerBean>, Map<String, List<Instant>>> beanEntry : timeouts.entrySet()) {
                        beanEntry.getValue().put(entry.getKey(), parseTimeouts(response.getHeaders(beanEntry.getKey().getName())));
                    }
                }
            }

            // Single action timer should have exactly 1 timeout on node 1
            for (Class<? extends TimerBean> beanClass : singleActionTimerBeanClasses) {
                Map<String, List<Instant>> singleActionTimeouts = timeouts.remove(beanClass);
                assertEquals(1, singleActionTimeouts.get(NODE_1).size(), singleActionTimeouts.toString());
                assertEquals(0, singleActionTimeouts.get(NODE_2).size(), singleActionTimeouts.toString());
            }

            for (Map.Entry<Class<? extends TimerBean>, Map<String, List<Instant>>> beanEntry : timeouts.entrySet()) {
                if (ManualTimerBean.class.isAssignableFrom(beanEntry.getKey())) {
                    // Other timer timeouts should only have been received on node 1
                    assertFalse(beanEntry.getValue().get(NODE_1).isEmpty(), beanEntry.toString());
                    assertTrue(beanEntry.getValue().get(NODE_2).isEmpty(), beanEntry.toString());
                } else if (AutoTransientTimerBean.class.equals(beanEntry.getKey())) {
                    // Transient auto-timers will exist on both nodes
                    assertFalse(beanEntry.getValue().get(NODE_1).isEmpty(), beanEntry.toString());
                    assertFalse(beanEntry.getValue().get(NODE_2).isEmpty(), beanEntry.toString());
                } else {
                    // Auto-timers might have been rescheduled during startup
                    assertTrue(!beanEntry.getValue().get(NODE_1).isEmpty() || !beanEntry.getValue().get(NODE_2).isEmpty(), beanEntry.toString());
                }
            }

            for (Map.Entry<String, URI> entry : uris.entrySet()) {
                try (CloseableHttpResponse response = client.execute(new HttpHead(entry.getValue()))) {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    for (Class<? extends TimerBean> beanClass : TimerServlet.TIMER_CLASSES) {
                        int count = Integer.parseInt(response.getFirstHeader(beanClass.getName()).getValue());
                        if (TimerServlet.SINGLE_ACTION_TIMER_CLASSES.contains(beanClass) || (TimerServlet.MANUAL_TRANSIENT_TIMER_CLASSES.contains(beanClass) && entry.getKey().equals(NODE_2))) {
                            assertEquals(0, count, entry.getKey() + ": " + beanClass.getName());
                        } else {
                            assertEquals(1, count, entry.getKey() + ": " + beanClass.getName());
                        }
                    }
                }
            }

            TimeUnit.SECONDS.sleep(GRACE_PERIOD.getSeconds());

            for (Map<String, List<Instant>> beanTimeouts : timeouts.values()) {
                beanTimeouts.clear();
            }
            for (Class<? extends TimerBean> singleActionTimerBeanClass : singleActionTimerBeanClasses) {
                timeouts.put(singleActionTimerBeanClass, new TreeMap<>());
            }

            for (Map.Entry<String, URI> entry : uris.entrySet()) {
                try (CloseableHttpResponse response = client.execute(new HttpGet(entry.getValue()))) {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    for (Map.Entry<Class<? extends TimerBean>, Map<String, List<Instant>>> beanEntry : timeouts.entrySet()) {
                        beanEntry.getValue().put(entry.getKey(), parseTimeouts(response.getHeaders(beanEntry.getKey().getName())));
                    }
                }
            }

            // Single action timers will already have expired
            for (Class<? extends TimerBean> singleActionTimerBeanClass : singleActionTimerBeanClasses) {
                Map<String, List<Instant>> singleActionTimers = timeouts.remove(singleActionTimerBeanClass);
                assertEquals(0, singleActionTimers.get(NODE_1).size(), singleActionTimers.toString());
                assertEquals(0, singleActionTimers.get(NODE_2).size(), singleActionTimers.toString());
            }

            // Other timer timeouts should only have been received on one member or the other
            for (Map.Entry<Class<? extends TimerBean>, Map<String, List<Instant>>> beanEntry : timeouts.entrySet()) {
                if (ManualTimerBean.class.isAssignableFrom(beanEntry.getKey())) {
                    assertFalse(beanEntry.getValue().get(NODE_1).isEmpty(), beanEntry.toString());
                    assertTrue(beanEntry.getValue().get(NODE_2).isEmpty(), beanEntry.toString());
                } else {
                    // Auto-timers might have been rescheduled during startup
                    assertTrue(!beanEntry.getValue().get(NODE_1).isEmpty() || !beanEntry.getValue().get(NODE_2).isEmpty(), beanEntry.toString());
                }
            }

            this.stop(NODE_1);

            TimeUnit.SECONDS.sleep(GRACE_PERIOD.getSeconds());

            try (CloseableHttpResponse response = client.execute(new HttpHead(uris.get(NODE_2)))) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                for (Class<? extends TimerBean> beanClass : TimerServlet.TIMER_CLASSES) {
                    int count = Integer.parseInt(response.getFirstHeader(beanClass.getName()).getValue());
                    if (TimerServlet.SINGLE_ACTION_TIMER_CLASSES.contains(beanClass) || TimerServlet.MANUAL_TRANSIENT_TIMER_CLASSES.contains(beanClass)) {
                        assertEquals(0, count, beanClass.getName());
                    } else {
                        assertEquals(1, count, beanClass.getName());
                    }
                }
            }

            for (Map<String, List<Instant>> beanTimeouts : timeouts.values()) {
                beanTimeouts.clear();
            }

            try (CloseableHttpResponse response = client.execute(new HttpGet(uris.get(NODE_2)))) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                for (Map.Entry<Class<? extends TimerBean>, Map<String, List<Instant>>> beanEntry : timeouts.entrySet()) {
                    beanEntry.getValue().put(NODE_2, parseTimeouts(response.getHeaders(beanEntry.getKey().getName())));
                }
            }

            for (Map.Entry<Class<? extends TimerBean>, Map<String, List<Instant>>> entry : timeouts.entrySet()) {
                if (TimerServlet.PERSISTENT_TIMER_CLASSES.contains(entry.getKey()) || AutoTimerBean.class.isAssignableFrom(entry.getKey())) {
                    assertNotEquals(0, entry.getValue().get(NODE_2).size(), entry.toString());
                } else {
                    // Manual transient timers were never created on node 2
                    assertEquals(0, entry.getValue().get(NODE_2).size(), entry.toString());
                }
            }

            this.start(NODE_1);

            TimeUnit.SECONDS.sleep(GRACE_PERIOD.getSeconds());

            for (Map<String, List<Instant>> beanTimeouts : timeouts.values()) {
                beanTimeouts.clear();
            }
            for (Map.Entry<String, URI> entry : uris.entrySet()) {
                try (CloseableHttpResponse response = client.execute(new HttpGet(entry.getValue()))) {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    for (Map.Entry<Class<? extends TimerBean>, Map<String, List<Instant>>> beanEntry : timeouts.entrySet()) {
                        beanEntry.getValue().put(entry.getKey(), parseTimeouts(response.getHeaders(beanEntry.getKey().getName())));
                    }
                }
            }

            for (Map.Entry<Class<? extends TimerBean>, Map<String, List<Instant>>> entry : timeouts.entrySet()) {
                if (AutoTransientTimerBean.class.equals(entry.getKey())) {
                    // Manual auto timers will be triggered on both nodes
                    assertFalse(entry.getValue().get(NODE_1).isEmpty(), entry.toString());
                    assertFalse(entry.getValue().get(NODE_2).isEmpty(), entry.toString());
                } else if (TimerServlet.TRANSIENT_TIMER_CLASSES.contains(entry.getKey())) {
                    // Manual transient timers will not exist on either node
                    assertTrue(entry.getValue().get(NODE_1).isEmpty(), entry.toString());
                    assertTrue(entry.getValue().get(NODE_2).isEmpty(), entry.toString());
                } else {
                    // Verify that at least 1 timeout was received on either member
                    assertFalse(entry.getValue().get(NODE_1).isEmpty() && entry.getValue().get(NODE_2).isEmpty(), entry.toString());
                }
            }

            TimeUnit.SECONDS.sleep(GRACE_PERIOD.getSeconds());

            for (Map<String, List<Instant>> beanTimeouts : timeouts.values()) {
                beanTimeouts.clear();
            }
            for (Map.Entry<String, URI> entry : uris.entrySet()) {
                try (CloseableHttpResponse response = client.execute(new HttpGet(entry.getValue()))) {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    for (Map.Entry<Class<? extends TimerBean>, Map<String, List<Instant>>> beanEntry : timeouts.entrySet()) {
                        beanEntry.getValue().put(entry.getKey(), parseTimeouts(response.getHeaders(beanEntry.getKey().getName())));
                    }
                }
            }

            for (Map.Entry<Class<? extends TimerBean>, Map<String, List<Instant>>> entry : timeouts.entrySet()) {
                if (AutoTransientTimerBean.class.equals(entry.getKey())) {
                    // Manual auto timers will be triggered on both nodes
                    assertFalse(entry.getValue().get(NODE_1).isEmpty(), entry.toString());
                    assertFalse(entry.getValue().get(NODE_2).isEmpty(), entry.toString());
                } else if (TimerServlet.TRANSIENT_TIMER_CLASSES.contains(entry.getKey())) {
                    // Manual transient timers will not exist on either node
                    assertTrue(entry.getValue().get(NODE_1).isEmpty(), entry.toString());
                    assertTrue(entry.getValue().get(NODE_2).isEmpty(), entry.toString());
                } else {
                    // Verify that at least 1 timeout was received on a single member
                    assertTrue(entry.getValue().get(NODE_1).isEmpty() ^ entry.getValue().get(NODE_2).isEmpty(), entry.toString());
                }
            }

            this.stop(NODE_2);

            TimeUnit.SECONDS.sleep(GRACE_PERIOD.getSeconds());

            for (Map<String, List<Instant>> beanTimeouts : timeouts.values()) {
                beanTimeouts.clear();
            }

            try (CloseableHttpResponse response = client.execute(new HttpGet(uris.get(NODE_1)))) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                for (Map.Entry<Class<? extends TimerBean>, Map<String, List<Instant>>> beanEntry : timeouts.entrySet()) {
                    beanEntry.getValue().put(NODE_1, parseTimeouts(response.getHeaders(beanEntry.getKey().getName())));
                }
            }

            for (Map.Entry<Class<? extends TimerBean>, Map<String, List<Instant>>> entry : timeouts.entrySet()) {
                if (TimerServlet.PERSISTENT_TIMER_CLASSES.contains(entry.getKey()) || AutoTimerBean.class.isAssignableFrom(entry.getKey())) {
                    assertNotEquals(0, entry.getValue().get(NODE_1).size(), entry.toString());
                } else {
                    // Manual transient timers were never created on node 2
                    assertEquals(0, entry.getValue().get(NODE_1).size(), entry.toString());
                }
            }

            this.start(NODE_2);

            TimeUnit.SECONDS.sleep(GRACE_PERIOD.getSeconds());

            try (CloseableHttpResponse response = client.execute(new HttpDelete(uris.get(NODE_1)))) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            }

            Instant cancellation = Instant.now();

            TimeUnit.SECONDS.sleep(GRACE_PERIOD.getSeconds());

            for (Map<String, List<Instant>> beanTimeouts : timeouts.values()) {
                beanTimeouts.clear();
            }

            for (Map.Entry<String, URI> entry : uris.entrySet()) {
                try (CloseableHttpResponse response = client.execute(new HttpGet(entry.getValue()))) {
                    assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    for (Map.Entry<Class<? extends TimerBean>, Map<String, List<Instant>>> beanEntry : timeouts.entrySet()) {
                        beanEntry.getValue().put(entry.getKey(), parseTimeouts(response.getHeaders(beanEntry.getKey().getName())));
                    }
                }
            }

            // Ensure all timeouts were received before cancellation was initiated.
            for (Map.Entry<Class<? extends TimerBean>, Map<String, List<Instant>>> entry : timeouts.entrySet()) {
                for (String node : uris.keySet()) {
                    if (ManualTimerBean.class.isAssignableFrom(entry.getKey())) {
                        assertTrue(entry.getValue().get(node).stream().allMatch(instant -> instant.isBefore(cancellation)), cancellation + " " + entry.toString());
                    } else {
                        assertTrue(!entry.getValue().get(NODE_1).isEmpty() || !entry.getValue().get(NODE_2).isEmpty(), entry.toString());
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static List<Instant> parseTimeouts(Header[] headers) {
        List<Instant> timeouts = new ArrayList<>(headers.length);
        for (Header header : headers) {
            timeouts.add(DateUtils.parseDate(header.getValue()).toInstant());
        }
        return timeouts;
    }
}
