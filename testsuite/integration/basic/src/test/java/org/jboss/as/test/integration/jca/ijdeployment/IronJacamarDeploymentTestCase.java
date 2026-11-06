/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.ijdeployment;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.test.integration.management.jca.ComplexPropertiesParseUtils.checkModelParams;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.test.integration.jca.beanvalidation.ra.ValidResourceAdapter;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * JBQA-6277 -IronJacamar deployments subsystem test case
 *
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a>
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class IronJacamarDeploymentTestCase extends ContainerResourceMgmtTestBase {

    /**
     * Define the deployment
     *
     * @return The deployment archive
     */
    @Deployment
    public static ResourceAdapterArchive createDeployment() {
        String deploymentName = "ij.rar";

        ResourceAdapterArchive raa = ShrinkWrap.create(ResourceAdapterArchive.class, deploymentName);
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "ij.jar");
        ja.addPackage(ValidResourceAdapter.class.getPackage());
        raa.addAsLibrary(ja);

        raa.addAsManifestResource(IronJacamarDeploymentTestCase.class.getPackage(), "ra.xml", "ra.xml")
                .addAsManifestResource(IronJacamarDeploymentTestCase.class.getPackage(), "ironjacamar.xml", "ironjacamar.xml")
                .addAsManifestResource(
                        new StringAsset(
                                "Dependencies: javax.inject.api,org.jboss.as.connector\n"),
                        "MANIFEST.MF");

        return raa;
    }

    /**
     * Test configuration - if all properties propagated to the model
     *
     * @throws Throwable Thrown if case of an error
     */
    @Test
    public void testConfiguration() throws Throwable {
        ModelNode address = new ModelNode();
        address.add("deployment", "ij.rar").add("subsystem", "resource-adapters").add("ironjacamar", "ironjacamar")
                .add("resource-adapter", "ij.rar");
        ModelNode operation = new ModelNode();
        operation.get(OP).set("read-resource");
        operation.get(OP_ADDR).set(address);
        operation.get(RECURSIVE).set(true);
        operation.get(INCLUDE_RUNTIME).set(true);
        ModelNode result = executeOperation(operation);

        assertEquals(result.get("bootstrap-context").asString(), "default", "Bootstrap-context value is wrong");
        assertEquals(result.get("transaction-support").asString(), "XATransaction", "Transaction-support value is wrong");
        assertEquals(result.get("config-properties", "raProperty", "value").asString(), "4",
                "RA config property value is wrong");
        assertEquals(
                result.get("beanvalidationgroups").asString(),
                "[\"org.jboss.as.test.integration.jca.beanvalidation.ra.ValidGroup\",\"org.jboss.as.test.integration.jca.beanvalidation.ra.ValidGroup1\"]",
                "Bean validation groups set wrong");

        ModelNode node = result.get("admin-objects", "java:jboss/VAO");
        Properties params = getAOProperties(true);
        assertTrue(checkModelParams(node, params), "compare failed, node:" + node.asString() + "\nparams:" + params);
        assertEquals(node.get("config-properties", "aoProperty", "value").asString(), "admin",
                "AO config property value is wrong");

        node = result.get("admin-objects", "java:jboss/VAO1");
        params = getAOProperties(false);
        assertTrue(checkModelParams(node, params), "compare failed, node:" + node.asString() + "\nparams:" + params);
        assertEquals(node.get("config-properties", "aoProperty", "value").asString(), "admin1",
                "AO1 config property value is wrong");

        node = result.get("connection-definitions", "java:jboss/VCF");
        params = getCFProperties(true);
        assertTrue(checkModelParams(node, params), "compare failed, node:" + node.asString() + "\nparams:" + params);
        assertEquals(node.get("config-properties", "cfProperty", "value").asString(), "first",
                "CF config property value is wrong");
        assertEquals("C", node.get("recovery-plugin-properties", "Property")
                .asString(), "Recovery plugin property value is wrong");

        node = result.get("connection-definitions", "java:jboss/VCF1");
        params = getCFProperties(false);
        assertTrue(checkModelParams(node, params), "compare failed, node:" + node.asString() + "\nparams:" + params);
        assertEquals(node.get("config-properties", "cfProperty", "value").asString(), "2nd",
                "CF1 config property value is wrong");
        assertEquals("C", node.get("recovery-plugin-properties", "Property")
                .asString(), "Recovery plugin 1 property value is wrong");

    }

    /**
     * Get Properties for admin object
     *
     * @param firstAO - true - for first, false - for second
     * @return
     */
    public Properties getAOProperties(boolean firstAO) {
        Properties prop = new Properties();
        String add = (firstAO ? "" : "1");
        String bool = String.valueOf(firstAO);
        prop.put("class-name", "org.jboss.as.test.integration.jca.beanvalidation.ra.ValidAdminObjectImpl" + add);
        prop.put("jndi-name", "java:jboss/VAO" + add);
        prop.put("enabled", bool);
        prop.put("use-java-context", bool);
        return prop;
    }

    /**
     * Get Properties for connection factory
     *
     * @param firstCF - true - for first, false - for second
     * @return
     */
    public Properties getCFProperties(boolean firstCF) {
        Properties prop = new Properties();
        String add = (firstCF ? "" : "1");
        String bool = String.valueOf(firstCF);
        prop.put("class-name", "org.jboss.as.test.integration.jca.beanvalidation.ra.ValidManagedConnectionFactory" + add);
        prop.put("jndi-name", "java:jboss/VCF" + add);
        prop.put("enabled", bool);
        prop.put("use-java-context", bool);
        prop.put("use-ccm", bool);
        prop.put("min-pool-size", "1");
        prop.put("max-pool-size", "5");
        prop.put("pool-prefill", bool);
        prop.put("pool-use-strict-min", bool);
        prop.put("same-rm-override", bool);
        prop.put("interleaving", bool);
        prop.put("no-tx-separate-pool", bool);
        prop.put("pad-xid", bool);
        prop.put("wrap-xa-resource", bool);
        prop.put("flush-strategy", firstCF ? "IDLE_CONNECTIONS" : "ENTIRE_POOL");
        if (true) {
            prop.put("security-application", bool);
            prop.put("recovery-username", "sa");
            prop.put("recovery-password", "sa-pass");
        } else {
            prop.put("security-domain", "HsqlDbRealm");
            prop.put("recovery-security-domain", "HsqlDbRealm");
        }
        prop.put("blocking-timeout-wait-millis", "5000");
        prop.put("idle-timeout-minutes", "4");
        prop.put("allocation-retry", "2");
        prop.put("allocation-retry-wait-millis", "3000");
        prop.put("xa-resource-timeout", "300");
        prop.put("background-validation", bool);
        prop.put("background-validation-millis", "5000");
        prop.put("use-fast-fail", bool);
        prop.put("no-recovery", bool);
        prop.put("recovery-plugin-class-name", "someClass2");

        return prop;
    }

}
