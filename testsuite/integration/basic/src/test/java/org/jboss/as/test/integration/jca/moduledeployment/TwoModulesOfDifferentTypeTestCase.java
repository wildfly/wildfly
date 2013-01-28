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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.jca.JcaMgmtBase;
import org.jboss.as.test.integration.jca.JcaMgmtServerSetupTask;
import org.jboss.as.test.integration.jca.moduledeployment.TwoModulesFlatTestCase.ModuleAcDeploymentTestCaseSetup;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.junit.Test;
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
@ServerSetup(TwoModulesOfDifferentTypeTestCase.ModuleAcDeploymentTestCaseSetup.class)
public class TwoModulesOfDifferentTypeTestCase extends TwoModulesFlatTestCase {


	static class ModuleAcDeploymentTestCaseSetup extends
			ModuleDeploymentTestCaseSetup {
		
		public static ModelNode address1;
		
		@Override
		public void doSetup(ManagementClient managementClient) throws Exception {

			super.doSetup(managementClient);
			fillModuleWithFlatClasses("ra1.xml");
			addModule("org/jboss/ironjacamar/ra16out1", "module1-jar.xml");
			fillModuleWithJar("ra1.xml");
			setConfiguration("mod-2.xml");
			address1 = address.clone();
			setConfiguration("basic.xml");

		}
		
		@Override
		public void tearDown(ManagementClient managementClient,
				String containerId) throws Exception {
			super.tearDown(managementClient, containerId);
			remove(address1);
			removeModule("org/jboss/ironjacamar/ra16out1");
		}

	}

	/**
	 * Define the deployment
	 * 
	 * @return The deployment archive
	 */
	@Deployment
	public static JavaArchive createDeployment() throws Exception {
		JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "multiple.jar");
		ja.addClasses(JcaMgmtServerSetupTask.class, JcaMgmtBase.class,
				MgmtOperationException.class, XMLElementReader.class,
				XMLElementWriter.class);

		ja.addPackage(AbstractMgmtTestBase.class.getPackage())
			.addPackage(AbstractModuleDeploymentTestCase.class.getPackage());

		ja.addAsManifestResource(
				new StringAsset(
						"Dependencies: org.jboss.as.controller-client,org.jboss.dmr,org.jboss.as.cli,javax.inject.api,org.jboss.as.connector\n"),
				"MANIFEST.MF");

		return ja;

	}

	/**
	 * Tests connection in pool
	 * 
	 * @throws Exception
	 *             in case of error
	 */
	@Test
	@RunAsClient
	public void testConnection2() throws Exception {
		final ModelNode address1 = ModuleAcDeploymentTestCaseSetup.address1
				.clone();
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
