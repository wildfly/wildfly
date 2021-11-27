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
@ServerSetup(MultiActivationJarTestCase.ModuleAcDeploymentTestCaseSetup.class)
public class MultiActivationJarTestCase extends MultiActivationFlatTestCase {


    static class ModuleAcDeploymentTestCaseSetup extends
            AbstractModuleDeploymentTestCaseSetup {
        @Override
        public void doSetup(ManagementClient managementClient) throws Exception {

            addModule(defaultPath, "module-jar.xml");
            fillModuleWithJar("ra1.xml");
            setConfiguration("double.xml");

        }

        @Override
        protected String getSlot() {
            return MultiActivationJarTestCase.class.getSimpleName().toLowerCase();
        }
    }
}
