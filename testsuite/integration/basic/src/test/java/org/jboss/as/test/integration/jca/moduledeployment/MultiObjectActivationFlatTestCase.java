/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.moduledeployment;

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
@ServerSetup(MultiObjectActivationFlatTestCase.ModuleAcDeploymentTestCaseSetup.class)
public class MultiObjectActivationFlatTestCase extends
        AbstractModuleDeploymentTestCase {

    private final String cf = "java:/testMeRA";
    private final String cf1 = "java:/testMeRA1";

    static class ModuleAcDeploymentTestCaseSetup extends
            AbstractModuleDeploymentTestCaseSetup {

        @Override
        public void doSetup(ManagementClient managementClient) throws Exception {

            super.doSetup(managementClient);
            fillModuleWithFlatClasses("ra2.xml");
            setConfiguration("multi.xml");

        }

        @Override
        protected String getSlot() {
            return MultiObjectActivationFlatTestCase.class.getSimpleName().toLowerCase();
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
    public void testConnectionFactory1() throws Throwable {
        testJndiObject(cf1, "MultipleConnectionFactory2Impl");
    }

    /**
     * Tests admin object
     *
     * @throws Exception
     */
    @Test
    public void testAdminObject() throws Exception {
        testJndiObject("java:/testAO", "MultipleAdminObject1Impl");
    }

    /**
     * Tests admin object
     *
     * @throws Exception
     */
    @Test
    public void testAdminObject1() throws Exception {
        testJndiObject("java:/testAO1", "MultipleAdminObject2Impl");
    }

    /**
     * Tests connection in pool
     *
     * @throws Exception in case of error
     */
    @Test
    @RunAsClient
    public void testConnections() throws Exception {
        testConnection(cf);
        testConnection(cf1);
    }

    @Override
    protected ModelNode getAddress() {
        return ModuleAcDeploymentTestCaseSetup.getAddress();
    }

}
