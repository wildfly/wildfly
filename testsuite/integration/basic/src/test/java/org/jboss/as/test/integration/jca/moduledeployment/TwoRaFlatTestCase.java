/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.moduledeployment;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import jakarta.annotation.Resource;
import jakarta.resource.cci.ConnectionFactory;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;

/**
 * AS7-5768 -Support for RA module deployment
 *
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a>
 *         <p>
 *         Tests for module deployment of resource adapter archive in
 *         uncompressed form with classes in flat form (under package structure)
 *         <p>
 *         Structure of module is:
 *         modulename
 *         modulename/main
 *         modulename/main/module.xml
 *         modulename/main/META-INF
 *         modulename/main/META-INF/ra.xml
 *         modulename/main/org
 *         modulename/main/org/jboss/
 *         modulename/main/org/jboss/package/
 *         modulename/main/org/jboss/package/First.class
 *         modulename/main/org/jboss/package/Second.class ...
 */
@RunWith(Arquillian.class)
@ServerSetup(TwoRaFlatTestCase.ModuleAcDeploymentTestCaseSetup.class)
public class TwoRaFlatTestCase extends AbstractModuleDeploymentTestCase {

    protected final String cf = "java:/testMeRA";
    protected final String cf1 = "java:/testMeRA1";

    static class ModuleAcDeploymentTestCaseSetup extends AbstractModuleDeploymentTestCaseSetup {

        static ModelNode address1;

        @Override
        public void doSetup(ManagementClient managementClient) throws Exception {
            super.doSetup(managementClient);
            fillModuleWithFlatClasses("ra1.xml");
            setConfiguration("second.xml");
            address1 = address.clone();
            setConfiguration("basic.xml");
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            remove(address1, managementClient);
            super.tearDown(managementClient, containerId);
        }

        @Override
        protected String getSlot() {
            return TwoRaFlatTestCase.class.getSimpleName().toLowerCase(Locale.ENGLISH);
        }
    }

    /**
     * Define the deployment
     *
     * @return The deployment archive
     */
    @Deployment
    public static JavaArchive getDeployment() throws Exception {
        return createDeployment();
    }

    @Resource(mappedName = cf)
    private ConnectionFactory connectionFactory;
    @Resource(mappedName = cf1)
    private ConnectionFactory connectionFactory1;

    /**
     * Tests connection factory
     *
     * @throws Throwable in case of an error
     */
    @Test
    public void testConnectionFactory() throws Throwable {
        testConnectionFactory(connectionFactory);
    }

    /**
     * Tests connection factory
     *
     * @throws Throwable in case of an error
     */
    @Test
    public void testConnectionFactoryProperties() throws Throwable {
        testJndiObject(cf, "MultipleConnectionFactory1Impl", "name=MCF", "name=RA");
    }

    /**
     * Tests connection factory
     *
     * @throws Throwable in case of an error
     */
    @Test
    public void testConnectionFactory1() throws Throwable {
        testConnectionFactory(connectionFactory1);
    }

    /**
     * Tests connection factory
     *
     * @throws Throwable in case of an error
     */
    @Test
    public void testConnectionFactoryProperties1() throws Throwable {
        testJndiObject(cf1, "MultipleConnectionFactory1Impl", "name=MCF", "name=RA");
    }

    /**
     * Tests admin object
     *
     * @throws Exception
     */
    @Test
    public void testAdminObject() throws Exception {
        testJndiObject("java:/testAO", "MultipleAdminObject1Impl", "name=AO");
    }

    /**
     * Tests admin object
     *
     * @throws Exception
     */
    @Test
    public void testAdminObject1() throws Exception {
        testJndiObject("java:/testAO1", "MultipleAdminObject1Impl", "name=AO");
    }

    /**
     * Tests connection in pool
     *
     * @throws Exception in case of error
     */
    @Test
    @RunAsClient
    public void testConnection1() throws Exception {
        testConnection(cf);
    }

    /**
     * Tests connection in pool
     *
     * @throws Exception in case of error
     */
    @Test
    @RunAsClient
    public void testConnection2() throws Exception {
        final ModelNode address1 = ModuleAcDeploymentTestCaseSetup.address1.clone();
        address1.add("connection-definitions", cf1);
        address1.protect();
        final ModelNode operation1 = new ModelNode();
        operation1.get(OP).set("test-connection-in-pool");
        operation1.get(OP_ADDR).set(address1);
        executeOperation(operation1);

    }

    @Override
    protected ModelNode getAddress() {
        return ModuleAcDeploymentTestCaseSetup.getAddress();
    }

}
