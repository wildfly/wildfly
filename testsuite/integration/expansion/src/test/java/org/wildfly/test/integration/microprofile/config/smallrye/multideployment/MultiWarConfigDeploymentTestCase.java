/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.microprofile.config.smallrye.multideployment;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.junit.Assert.assertEquals;

import java.net.URL;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(MultiWarConfigDeploymentTestCase.GlobalConfigSourceSetupTask.class)
public class MultiWarConfigDeploymentTestCase {

    @ArquillianResource
    @OperateOnDeployment("d1")
    private URL url1;

    @ArquillianResource
    @OperateOnDeployment("d2")
    private URL url2;

    @Deployment(name = "d1", order = 1, testable = false)
    public static Archive<?> createDeployment1() {
        return ShrinkWrap.create(WebArchive.class, "d1.war")
                .addClasses(ConfigResource.class, CustomConfigSource1.class, CustomConfigSourceProvider1.class, JaxRsActivator.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(new StringAsset(
                        "config_ordinal=700\n" +
                        "prop.local1.war.unique=war1-value\n" +
                        "prop.global.system.overridden.by.war=war1-wins-system\n" +
                        "prop.global.subsystem.overridden.by.war=war1-wins-subsystem\n" +
                        "prop.local.precedence.test=from-war1\n"), "microprofile-config.properties")
                .addAsManifestResource(new StringAsset(CustomConfigSource1.class.getName()),
                        "services/org.eclipse.microprofile.config.spi.ConfigSource")
                .addAsManifestResource(new StringAsset(CustomConfigSourceProvider1.class.getName()),
                        "services/org.eclipse.microprofile.config.spi.ConfigSourceProvider");
    }

    @Deployment(name = "d2", order = 2, testable = false)
    public static Archive<?> createDeployment2() {
        return ShrinkWrap.create(WebArchive.class, "d2.war")
                .addClasses(ConfigResource.class, CustomConfigSource2.class, CustomConfigSourceProvider2.class, JaxRsActivator.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(new StringAsset(
                        "config_ordinal=700\n" +
                        "prop.local2.war.unique=war2-value\n" +
                        "prop.global.system.overridden.by.war=war2-wins-system\n" +
                        "prop.global.subsystem.overridden.by.war=war2-wins-subsystem\n" +
                        "prop.local.precedence.test=from-war2\n"), "microprofile-config.properties")
                .addAsManifestResource(new StringAsset(CustomConfigSource2.class.getName()),
                        "services/org.eclipse.microprofile.config.spi.ConfigSource")
                .addAsManifestResource(new StringAsset(CustomConfigSourceProvider2.class.getName()),
                        "services/org.eclipse.microprofile.config.spi.ConfigSourceProvider");
    }

    @Test
    public void testConfigIsolationAndPrecedenceWar1() throws Exception {
        Client client = ClientBuilder.newClient();
        try {
            String d1BaseUrl = url1.toString() + "config/";

            // 1. Verify un-overridden global properties
            assertEquals("subsystem-global-value",
                    getConfigValue(client, d1BaseUrl, "prop.global.subsystem.untouched"));
            assertEquals("system-global-value",
                    getConfigValue(client, d1BaseUrl, "prop.global.system.untouched"));

            // 2. Verify unique local properties (isolation)
            assertEquals("war1-value",
                    getConfigValue(client, d1BaseUrl, "prop.local1.war.unique"));
            assertEquals("cs1-value",
                    getConfigValue(client, d1BaseUrl, "prop.local1.cs.unique"));
            assertEquals("csp1-value",
                    getConfigValue(client, d1BaseUrl, "prop.local1.csp.unique"));

            // Verify isolation - d1 should not see d2's properties
            assertNotFound(client, d1BaseUrl, "prop.local2.war.unique");
            assertNotFound(client, d1BaseUrl, "prop.local2.cs.unique");
            assertNotFound(client, d1BaseUrl, "prop.local2.csp.unique");

            // 3. Verify property precedence (overrides)
            // Local vs. Local precedence (ConfigSourceProvider[800] > ConfigSource[750] > .properties[700])
            assertEquals("from-csp1",
                    getConfigValue(client, d1BaseUrl, "prop.local.precedence.test"));

            // Local vs. Global (System Properties[400])
            assertEquals("war1-wins-system",
                    getConfigValue(client, d1BaseUrl, "prop.global.system.overridden.by.war"));
            assertEquals("cs1-wins-system",
                    getConfigValue(client, d1BaseUrl, "prop.global.system.overridden.by.cs"));
            assertEquals("csp1-wins-system",
                    getConfigValue(client, d1BaseUrl, "prop.global.system.overridden.by.csp"));

            // Local vs. Global (Subsystem Properties[~600])
            assertEquals("war1-wins-subsystem",
                    getConfigValue(client, d1BaseUrl, "prop.global.subsystem.overridden.by.war"));
            assertEquals("cs1-wins-subsystem",
                    getConfigValue(client, d1BaseUrl, "prop.global.subsystem.overridden.by.cs"));
            assertEquals("csp1-wins-subsystem",
                    getConfigValue(client, d1BaseUrl, "prop.global.subsystem.overridden.by.csp"));
        } finally {
            client.close();
        }
    }

    @Test
    public void testConfigIsolationAndPrecedenceWar2() throws Exception {
        Client client = ClientBuilder.newClient();
        try {
            String d2BaseUrl = url2.toString() + "config/";

            // 1. Verify un-overridden global properties
            assertEquals("subsystem-global-value",
                    getConfigValue(client, d2BaseUrl, "prop.global.subsystem.untouched"));
            assertEquals("system-global-value",
                    getConfigValue(client, d2BaseUrl, "prop.global.system.untouched"));

            // 2. Verify unique local properties (isolation)
            assertEquals("war2-value",
                    getConfigValue(client, d2BaseUrl, "prop.local2.war.unique"));
            assertEquals("cs2-value",
                    getConfigValue(client, d2BaseUrl, "prop.local2.cs.unique"));
            assertEquals("csp2-value",
                    getConfigValue(client, d2BaseUrl, "prop.local2.csp.unique"));

            // Verify isolation - d2 should not see d1's properties
            assertNotFound(client, d2BaseUrl, "prop.local1.war.unique");
            assertNotFound(client, d2BaseUrl, "prop.local1.cs.unique");
            assertNotFound(client, d2BaseUrl, "prop.local1.csp.unique");

            // 3. Verify property precedence (overrides)
            // Local vs. Local precedence (ConfigSourceProvider[800] > ConfigSource[750] > .properties[700])
            assertEquals("from-csp2",
                    getConfigValue(client, d2BaseUrl, "prop.local.precedence.test"));

            // Local vs. Global (System Properties[400])
            assertEquals("war2-wins-system",
                    getConfigValue(client, d2BaseUrl, "prop.global.system.overridden.by.war"));
            assertEquals("cs2-wins-system",
                    getConfigValue(client, d2BaseUrl, "prop.global.system.overridden.by.cs"));
            assertEquals("csp2-wins-system",
                    getConfigValue(client, d2BaseUrl, "prop.global.system.overridden.by.csp"));

            // Local vs. Global (Subsystem Properties[~600])
            assertEquals("war2-wins-subsystem",
                    getConfigValue(client, d2BaseUrl, "prop.global.subsystem.overridden.by.war"));
            assertEquals("cs2-wins-subsystem",
                    getConfigValue(client, d2BaseUrl, "prop.global.subsystem.overridden.by.cs"));
            assertEquals("csp2-wins-subsystem",
                    getConfigValue(client, d2BaseUrl, "prop.global.subsystem.overridden.by.csp"));
        } finally {
            client.close();
        }
    }

    private String getConfigValue(Client client, String baseUrl, String key) {
        Response response = client.target(baseUrl + key).request().get();
        assertEquals(200, response.getStatus());
        return response.readEntity(String.class);
    }

    private void assertNotFound(Client client, String baseUrl, String key) {
        Response response = client.target(baseUrl + key).request().get();
        assertEquals(404, response.getStatus());
    }

    public static class GlobalConfigSourceSetupTask implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            ModelNode op = new ModelNode();
            op.get(OP_ADDR).add(SUBSYSTEM, "microprofile-config-smallrye");
            op.get(OP_ADDR).add("config-source", "test");
            op.get(OP).set(ADD);
            op.get("ordinal").set(600);
            op.get("properties").get("prop.global.subsystem.untouched").set("subsystem-global-value");
            op.get("properties").get("prop.global.subsystem.overridden.by.war").set("subsystem-value");
            op.get("properties").get("prop.global.subsystem.overridden.by.cs").set("subsystem-value");
            op.get("properties").get("prop.global.subsystem.overridden.by.csp").set("subsystem-value");
            managementClient.getControllerClient().execute(op);

            addSystemProperty(managementClient, "prop.global.system.untouched", "system-global-value");
            addSystemProperty(managementClient, "prop.global.system.overridden.by.war", "system-value");
            addSystemProperty(managementClient, "prop.global.system.overridden.by.cs", "system-value");
            addSystemProperty(managementClient, "prop.global.system.overridden.by.csp", "system-value");
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            ModelNode op = new ModelNode();
            op.get(OP_ADDR).add(SUBSYSTEM, "microprofile-config-smallrye");
            op.get(OP_ADDR).add("config-source", "test");
            op.get(OP).set(REMOVE);
            managementClient.getControllerClient().execute(op);

            removeSystemProperty(managementClient, "prop.global.system.untouched");
            removeSystemProperty(managementClient, "prop.global.system.overridden.by.war");
            removeSystemProperty(managementClient, "prop.global.system.overridden.by.cs");
            removeSystemProperty(managementClient, "prop.global.system.overridden.by.csp");
        }

        private void addSystemProperty(ManagementClient client, String name, String value) throws Exception {
            ModelNode op = Util.createAddOperation(PathAddress.pathAddress(SYSTEM_PROPERTY, name));
            op.get(VALUE).set(value);
            client.getControllerClient().execute(op);
        }

        private void removeSystemProperty(ManagementClient client, String name) throws Exception {
            ModelNode op = Util.createRemoveOperation(PathAddress.pathAddress(SYSTEM_PROPERTY, name));
            client.getControllerClient().execute(op);
        }
    }
}