/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.single.ejb.timer;

import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.DEPLOYMENT_1;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.clustering.cluster.ejb.timer.beans.AbstractManualTimerBean;
import org.jboss.as.test.clustering.cluster.ejb.timer.beans.AbstractTimerBean;
import org.jboss.as.test.clustering.cluster.ejb.timer.beans.BurstPersistentCalendarTimerBean;
import org.jboss.as.test.clustering.cluster.ejb.timer.beans.BurstPersistentIntervalTimerBean;
import org.jboss.as.test.clustering.cluster.ejb.timer.beans.BurstPersistentTimerBean;
import org.jboss.as.test.clustering.cluster.ejb.timer.beans.ManualTimerBean;
import org.jboss.as.test.clustering.cluster.ejb.timer.beans.TimerBean;
import org.jboss.as.test.clustering.cluster.ejb.timer.beans.TimerRecorder;
import org.jboss.as.test.clustering.cluster.ejb.timer.servlet.AbstractTimerServlet;
import org.jboss.as.test.clustering.cluster.ejb.timer.servlet.BurstTimerServlet;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Validates that the distributed timer service does not reschedule a timer whose next timeout is in the past.
 * This has the effect of consolidating missed timer events into a single application callback.
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
public class CoalesceMissedTimeoutsTestCase {
    private static final String MODULE_NAME = CoalesceMissedTimeoutsTestCase.class.getSimpleName();
    private static final Duration GRACE_PERIOD = Duration.ofSeconds(TimeoutUtil.adjust(2));

    @Deployment(name = DEPLOYMENT_1, testable = false)
    public static Archive<?> deployment() {
        return ShrinkWrap.create(WebArchive.class, MODULE_NAME + ".war")
                .addClasses(BurstTimerServlet.class, AbstractTimerServlet.class)
                .addPackage(EJBDirectory.class.getPackage())
                .addClasses(BurstPersistentCalendarTimerBean.class, BurstPersistentIntervalTimerBean.class, BurstPersistentTimerBean.class, AbstractManualTimerBean.class, AbstractTimerBean.class, ManualTimerBean.class, TimerBean.class, TimerRecorder.class)
                ;
    }

    @Test
    public void test(@ArquillianResource(BurstTimerServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL, @ArquillianResource @OperateOnDeployment(DEPLOYMENT_1) ManagementClient managementClient) throws IOException, URISyntaxException {

        URI uri = BurstTimerServlet.createURI(baseURL, MODULE_NAME);

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {

            // Burst timer will begin firing 5 seconds after creation.
            try (CloseableHttpResponse response = client.execute(new HttpPut(uri))) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            }

            suspend(managementClient);

            // After 10 seconds all timeouts should have elapsed
            TimeUnit.SECONDS.sleep(BurstPersistentTimerBean.START_DURATION.plus(BurstPersistentTimerBean.BURST_DURATION).getSeconds());

            resume(managementClient);

            TimeUnit.SECONDS.sleep(GRACE_PERIOD.getSeconds());

            try (CloseableHttpResponse response = client.execute(new HttpGet(uri))) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                // Missed timeouts should have been consolidated into a single application callback
                Assert.assertEquals(1, parseTimeouts(response.getHeaders(BurstPersistentIntervalTimerBean.class.getName())).size());
                Assert.assertEquals(1, parseTimeouts(response.getHeaders(BurstPersistentCalendarTimerBean.class.getName())).size());
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

    private static void suspend(ManagementClient client) throws IOException {
        execute(client, ModelDescriptionConstants.SUSPEND);
    }

    private static void resume(ManagementClient client) throws IOException {
        execute(client, ModelDescriptionConstants.RESUME);
    }

    private static void execute(ManagementClient client, String operation) throws IOException {
        ModelNode result = client.getControllerClient().execute(Util.createOperation(operation, PathAddress.EMPTY_ADDRESS));
        Assert.assertEquals(result.toString(), ModelDescriptionConstants.SUCCESS, result.get(ModelDescriptionConstants.OUTCOME).asString());
    }
}
