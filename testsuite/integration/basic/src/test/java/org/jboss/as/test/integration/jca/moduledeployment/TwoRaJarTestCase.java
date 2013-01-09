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
import org.jboss.dmr.ModelNode;
import org.junit.runner.RunWith;


/**
 * AS7-5768 -Support for RA module deployment
 * 
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a>
 * 
 *         Tests for module deployment of resource adapter archive in
 *         uncompressed form with classes, packed in .jar file
 * 
 *         Structure of module is: 
 *         modulename 
 *         modulename/main
 *         modulename/main/module.xml 
 *         modulename/main/META-INF
 *         modulename/main/META-INF/ra.xml 
 *         modulename/main/module.jar
 */
@RunWith(Arquillian.class)
@ServerSetup(TwoRaJarTestCase.ModuleAcDeploymentTestCaseSetup.class)
public class TwoRaJarTestCase extends TwoRaFlatTestCase {


	static class ModuleAcDeploymentTestCaseSetup extends
			ModuleDeploymentTestCaseSetup {
		
		public static ModelNode address1;
		
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
			remove(address1);
			super.tearDown(managementClient, containerId);
		}

	}
}
