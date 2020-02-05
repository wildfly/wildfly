/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ws.microprofile.metrics;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STATISTICS_ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.wildfly.test.microprofile.util.MetricsHelper.getJSONMetrics;
import static org.wildfly.test.microprofile.util.MetricsHelper.getMetricValueFromJSONOutput;
import static org.wildfly.test.microprofile.util.MetricsHelper.getPrometheusMetrics;
import static org.jboss.as.test.shared.ServerReload.reloadIfRequired;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.ws.basic.EndpointIface;
import org.jboss.as.test.integration.ws.basic.HelloObject;
import org.jboss.as.test.integration.ws.basic.PojoEndpoint;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test WS with MP metrics
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ MicroProfileMetricsWSTestCase.EnablesUndertowStatistics.class })
public class MicroProfileMetricsWSTestCase {

    static class EnablesUndertowStatistics implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            managementClient.getControllerClient().execute(enableStatistics(true));
            reloadIfRequired(managementClient);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            managementClient.getControllerClient().execute(enableStatistics(false));
            reloadIfRequired(managementClient);
        }

        private ModelNode enableStatistics(boolean enabled) {
            final ModelNode address = Operations.createAddress(SUBSYSTEM, "undertow");
            return Operations.createWriteAttributeOperation(address, STATISTICS_ENABLED, enabled);
        }
    }

    @Deployment(name = "jaxws-basic-pojo", managed = false)
    public static Archive<?> deployment() {
        WebArchive pojoWar = ShrinkWrap.create(WebArchive.class, "jaxws-basic-pojo.war").addClasses(EndpointIface.class,
                PojoEndpoint.class, HelloObject.class);
        pojoWar.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return pojoWar;
    }

    @BeforeClass
    public static void skipSecurityManager() {
        AssumeTestGroupUtil.assumeSecurityManagerDisabled();
    }

    @ContainerResource
    ManagementClient managementClient;

    @ArquillianResource
    private Deployer deployer;


    @Test
    @InSequence(1)
    public void testApplicationMetricBeforeDeployment() throws Exception {
        getPrometheusMetrics(managementClient, "application", false);
        getJSONMetrics(managementClient, "application", false);

        // deploy the archive
        deployer.deploy("jaxws-basic-pojo");
    }

    @Test
    @InSequence(2)
    @OperateOnDeployment("jaxws-basic-pojo")
    public void testDeploymenTest(@ArquillianResource URL url) throws Exception {
        QName serviceName = new QName("http://jbossws.org/basic", "POJOService");
        URL wsdlURL = new URL(url, "POJOService?wsdl");
        Service service = Service.create(wsdlURL, serviceName);
        EndpointIface proxy = service.getPort(EndpointIface.class);
        checkRequestCount(2, true);

        Assert.assertEquals("Hello World!", proxy.helloString("World"));
        String metrics = getJSONMetrics(managementClient, "application", true);
        double counter = getMetricValueFromJSONOutput(metrics, "helloString");
        assertEquals(1.0, counter, 0.0);

        HelloObject helloObject = new HelloObject("Kermit");
        Assert.assertEquals("Hello Kermit!", proxy.helloBean(helloObject).getMessage());
        metrics = getJSONMetrics(managementClient, "application", true);
        counter = getMetricValueFromJSONOutput(metrics, "helloBean");
        assertEquals(1.0, counter, 0.0);
        checkRequestCount(4, true);
    }

    private void checkRequestCount(int expectedCount, boolean deploymentMetricMustExist) throws IOException {
        String prometheusMetricName = "wildfly_undertow_request_count_total";
        String metrics = getPrometheusMetrics(managementClient, "", true);
        for (String line : metrics.split("\\R")) {
            if (line.startsWith(prometheusMetricName)) {
                String[] split = line.split("\\s+");
                String labels = split[0].substring((prometheusMetricName).length());

                // we are only interested by the metric for this deployment
                if (labels.contains("deployment=\"jaxws-basic-pojo.war\"")) {
                    if (deploymentMetricMustExist) {
                        Double value = Double.valueOf(split[1]);
                        assertTrue(labels.contains("deployment=\"jaxws-basic-pojo.war\""));
                        assertTrue(labels.contains("subdeployment=\"jaxws-basic-pojo.war\""));
                        assertEquals(Integer.valueOf(expectedCount).doubleValue(), value, 0);

                        return;
                    } else {
                        fail("Metric for the deployment must not exist");
                    }
                }
            }
        }

        if (deploymentMetricMustExist) {
            fail(prometheusMetricName + "metric not found for deployment");
        }
    }

}
