/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2024, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.ejb.timer;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.ejb.timer.beans.AbstractManualTimerBean;
import org.jboss.as.test.clustering.cluster.ejb.timer.beans.AbstractTimerBean;
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
public class ConsolidateMissedTimersTestCase extends AbstractClusteringTestCase {
    private static final String MODULE_NAME = ConsolidateMissedTimersTestCase.class.getSimpleName();
    private static final Duration GRACE_PERIOD = Duration.ofSeconds(TimeoutUtil.adjust(2));

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> deployment() {
        return ShrinkWrap.create(WebArchive.class, MODULE_NAME + ".war")
                .addClasses(BurstTimerServlet.class, AbstractTimerServlet.class)
                .addPackage(EJBDirectory.class.getPackage())
                .addClasses(BurstPersistentTimerBean.class, AbstractManualTimerBean.class, AbstractTimerBean.class, ManualTimerBean.class, TimerBean.class, TimerRecorder.class)
                ;
    }

    public ConsolidateMissedTimersTestCase() {
        super(Set.of(NODE_1));
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
                Assert.assertEquals(1, parseTimeouts(response.getHeaders(BurstPersistentTimerBean.class.getName())).size());
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
