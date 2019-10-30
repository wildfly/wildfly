/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.jca.ijdeployment;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.test.integration.management.jca.ComplexPropertiesParseUtils.checkModelParams;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.jca.beanvalidation.ra.ValidResourceAdapter;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * JBQA-6277 -IronJacamar deployments subsystem test case
 *
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class IronJacamarDeploymentTestCase extends ContainerResourceMgmtTestBase {

    /**
     * Define the deployment
     *
     * @return The deployment archive
     */
    @Deployment
    public static ResourceAdapterArchive createDeployment() throws Exception {
        String deploymentName = "ij.rar";

        ResourceAdapterArchive raa = ShrinkWrap.create(ResourceAdapterArchive.class, deploymentName);
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "ij.jar");
        ja.addPackage(ValidResourceAdapter.class.getPackage()).addClasses(IronJacamarDeploymentTestCase.class,
                MgmtOperationException.class, XMLElementReader.class, XMLElementWriter.class);
        ja.addPackage(AbstractMgmtTestBase.class.getPackage());
        raa.addAsLibrary(ja);

        raa.addAsManifestResource(IronJacamarDeploymentTestCase.class.getPackage(), "ra.xml", "ra.xml")
                .addAsManifestResource(IronJacamarDeploymentTestCase.class.getPackage(), "ironjacamar.xml", "ironjacamar.xml")
                .addAsManifestResource(
                        new StringAsset(
                                "Dependencies: org.jboss.as.controller-client,org.jboss.dmr,org.jboss.as.cli,javax.inject.api,org.jboss.as.connector\n"),
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
        ModelNode result = executeOperation(operation);

        assertEquals("Bootstrap-context value is wrong", result.get("bootstrap-context").asString(), "default");
        assertEquals("Transaction-support value is wrong", result.get("transaction-support").asString(), "XATransaction");
        assertEquals("RA config property value is wrong", result.get("config-properties", "raProperty", "value").asString(),
                "4");
        assertEquals(
                "Bean validation groups set wrong",
                result.get("beanvalidationgroups").asString(),
                "[\"org.jboss.as.test.integration.jca.beanvalidation.ra.ValidGroup\",\"org.jboss.as.test.integration.jca.beanvalidation.ra.ValidGroup1\"]");

        ModelNode node = result.get("admin-objects", "java:jboss/VAO");
        Properties params = getAOProperties(true);
        assertTrue("compare failed, node:" + node.asString() + "\nparams:" + params, checkModelParams(node, params));
        assertEquals("AO config property value is wrong", node.get("config-properties", "aoProperty", "value").asString(),
                "admin");

        node = result.get("admin-objects", "java:jboss/VAO1");
        params = getAOProperties(false);
        assertTrue("compare failed, node:" + node.asString() + "\nparams:" + params, checkModelParams(node, params));
        assertEquals("AO1 config property value is wrong", node.get("config-properties", "aoProperty", "value").asString(),
                "admin1");

        node = result.get("connection-definitions", "java:jboss/VCF");
        params = getCFProperties(true);
        assertTrue("compare failed, node:" + node.asString() + "\nparams:" + params, checkModelParams(node, params));
        assertEquals("CF config property value is wrong", node.get("config-properties", "cfProperty", "value").asString(),
                "first");
        assertEquals("Recovery plugin property value is wrong", "C", node.get("recovery-plugin-properties", "Property")
                .asString());

        node = result.get("connection-definitions", "java:jboss/VCF1");
        params = getCFProperties(false);
        assertTrue("compare failed, node:" + node.asString() + "\nparams:" + params, checkModelParams(node, params));
        assertEquals("CF1 config property value is wrong", node.get("config-properties", "cfProperty", "value").asString(),
                "2nd");
        assertEquals("Recovery plugin 1 property value is wrong", "C", node.get("recovery-plugin-properties", "Property")
                .asString());

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
        if (firstCF) {
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
