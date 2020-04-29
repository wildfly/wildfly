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

package org.wildfly.test.integration.microprofile.metrics.config;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.test.shared.ServerReload.executeReloadAndWaitForCompletion;
import static org.junit.Assert.assertTrue;
import static org.wildfly.test.integration.microprofile.metrics.MetricsHelper.getPrometheusMetrics;

import java.net.URL;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.metrics.TestApplication;
import org.wildfly.test.integration.microprofile.metrics.config.resource.ResourceSimple;

/**
 * Test that global metrics tag defined in a config-source by the mp.metrics.tag config property
 * in microprofile-config-smallrye subsystem are taken into account.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({MicroProfileMetricsGlobalTagsTestCase.AddMPMetricsTags.class})
public class MicroProfileMetricsGlobalTagsTestCase {


    static class AddMPMetricsTags implements ServerSetupTask {

        static final String MY_GLOBAL_METRIC_TAG = UUID.randomUUID().toString();

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            managementClient.getControllerClient().execute(addOrRemoveMPMetricsConfigSource(true));
            // force reload so that vendor and base metrics are registered at boot time with the tags from the config source
            executeReloadAndWaitForCompletion(managementClient);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            managementClient.getControllerClient().execute(addOrRemoveMPMetricsConfigSource(false));
            executeReloadAndWaitForCompletion(managementClient);
        }

        private ModelNode addOrRemoveMPMetricsConfigSource(boolean add) {
            final ModelNode address = Operations.createAddress(SUBSYSTEM, "microprofile-config-smallrye", "config-source", "mp-metrics");

            if (add) {
                ModelNode addOperation = Operations.createAddOperation(address);
                addOperation.get(PROPERTIES).add("mp.metrics.tags", "my_metric_tag=" + MY_GLOBAL_METRIC_TAG);
                return addOperation;
            }
            return Operations.createRemoveOperation(address);
        }
    }

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "MicroProfileMetricsGlobalTagsTestCase.war")
                .addClasses(TestApplication.class)
                .addClass(ResourceSimple.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }

    @BeforeClass
    public static void skipSecurityManager() {
        AssumeTestGroupUtil.assumeSecurityManagerDisabled();
    }

    @ContainerResource
    ManagementClient managementClient;

    @ArquillianResource URL url;

    @Test
    public void testMetricsContainsGlobalTags() throws Exception {
        String tagInPrometheusOutput = "my_metric_tag=\"" + AddMPMetricsTags.MY_GLOBAL_METRIC_TAG + "\"";

        performCall(url);

        String applicationMetrics = getPrometheusMetrics(managementClient, "application", true);
        assertTrue(applicationMetrics, applicationMetrics.contains(tagInPrometheusOutput));

        String baseMetrics = getPrometheusMetrics(managementClient, "base", true);
        assertTrue(baseMetrics, baseMetrics.contains(tagInPrometheusOutput));

        String vendorMetrics = getPrometheusMetrics(managementClient, "vendor", true);
        assertTrue(vendorMetrics, vendorMetrics.contains(tagInPrometheusOutput));
    }

    private static String performCall(URL url) throws Exception {
        URL appURL = new URL(url.toExternalForm() + "microprofile-metrics-app/hello2");
        return HttpRequest.get(appURL.toExternalForm(), 10, TimeUnit.SECONDS);
    }

}
