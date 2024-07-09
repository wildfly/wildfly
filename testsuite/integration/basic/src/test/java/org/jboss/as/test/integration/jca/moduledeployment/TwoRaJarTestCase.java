/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.moduledeployment;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.dmr.ModelNode;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;


/**
 * AS7-5768 -Support for RA module deployment
 *
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a>
 *         <p/>
 *         Tests for module deployment of resource adapter archive in
 *         uncompressed form with classes, packed in .jar file
 *         <p/>
 *         Structure of module is:
 *         modulename
 *         modulename/main
 *         modulename/main/module.xml
 *         modulename/main/META-INF
 *         modulename/main/META-INF/ra.xml
 *         modulename/main/module.jar
 */
@RunWith(Arquillian.class)
@ServerSetup(TwoRaJarTestCase.ModuleAcDeploymentTestCaseSetup1.class)
public class TwoRaJarTestCase extends TwoRaFlatTestCase {

    static class ModuleAcDeploymentTestCaseSetup1 extends AbstractModuleDeploymentTestCaseSetup {

        static ModelNode address1;

        @Override
        public void doSetup(ManagementClient managementClient) throws Exception {
            addModule(defaultPath, "module-jar.xml");
            fillModuleWithJar("ra1.xml");
            setConfiguration("second.xml");
            address1 = address.clone();
            setConfiguration("basic.xml");
        }

        @Override
        public void tearDown(ManagementClient managementClient,
                             String containerId) throws Exception {
            remove(address1, managementClient);
            super.tearDown(managementClient, containerId);
        }

        @Override
        protected String getSlot() {
            return TwoRaJarTestCase.class.getSimpleName().toLowerCase(Locale.ENGLISH);
        }
    }

    /**
     * Tests connection in pool
     *
     * @throws Exception in case of error
     */
    @Test
    @RunAsClient
    public void testConnection2() throws Exception {
        final ModelNode address1 = ModuleAcDeploymentTestCaseSetup1.address1.clone();
        address1.add("connection-definitions", cf1);
        address1.protect();
        final ModelNode operation1 = new ModelNode();
        operation1.get(OP).set("test-connection-in-pool");
        operation1.get(OP_ADDR).set(address1);
        executeOperation(operation1);

    }

    @Override
    protected ModelNode getAddress() {
        return ModuleAcDeploymentTestCaseSetup1.getAddress();
    }

}
