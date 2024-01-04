/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.moduledeployment;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.junit.runner.RunWith;


/**
 * AS7-5768 -Support for RA module deployment
 *
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a>
 *         <p>
 *         Tests for module deployment of resource adapter archive in
 *         uncompressed form with classes, packed in .jar file
 *         <p>
 *         Structure of module is:
 *         modulename
 *         modulename/main
 *         modulename/main/module.xml
 *         modulename/main/META-INF
 *         modulename/main/META-INF/ra.xml
 *         modulename/main/module.jar
 */
@RunWith(Arquillian.class)
@ServerSetup(BasicJarTestCase.ModuleAcDeploymentTestCaseSetup.class)
public class BasicJarTestCase extends BasicFlatTestCase {


    static class ModuleAcDeploymentTestCaseSetup extends AbstractModuleDeploymentTestCaseSetup {
        @Override
        public void doSetup(ManagementClient managementClient) throws Exception {

            addModule(defaultPath, "module-jar.xml");
            fillModuleWithJar("ra1.xml");
            setConfiguration("basic.xml");

        }

        @Override
        protected String getSlot() {
            return BasicJarTestCase.class.getSimpleName().toLowerCase();
        }
    }
}
