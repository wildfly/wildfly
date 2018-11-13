/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.microprofile.metrics.base;

import static org.junit.Assert.assertTrue;
import static org.wildfly.test.integration.microprofile.metrics.MetricsHelper.getJSONMetrics;
import static org.wildfly.test.integration.microprofile.metrics.MetricsHelper.getMetricValueFromJSONOutput;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test required base metrics that are always present.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class MicroProfileMetricsBaseTestCase {

    // Use an empty deployment as the test deals with base metrics only
    @Deployment(name = "MicroProfileMetricsBaseTestCase", managed = false)
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "MicroProfileMetricsBaseTestCase.war");
        return war;
    }

    @ContainerResource
    ManagementClient managementClient;

    @Test
    public void testBaseMetrics() throws Exception {
        long start = System.currentTimeMillis();
        long sleep = 50;

        String metrics = getJSONMetrics(managementClient, "base", true);
        double uptime1 = getMetricValueFromJSONOutput(metrics, "jvm.uptime");
        assertTrue(uptime1 > 0);

        Thread.sleep(sleep);

        metrics = getJSONMetrics(managementClient, "base", true);
        double uptime2 = getMetricValueFromJSONOutput(metrics, "jvm.uptime");

        long interval = System.currentTimeMillis() - start;

        assertTrue( uptime2 > uptime1);
        assertTrue((uptime2 - uptime1) >= sleep);
        assertTrue((uptime2 - uptime1) <= interval);
    }
}
