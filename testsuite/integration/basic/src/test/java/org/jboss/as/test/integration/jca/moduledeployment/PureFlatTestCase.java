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

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.annotation.Resource;
import javax.resource.cci.ConnectionFactory;

/**
 * AS7-5768 -Support for RA module deployment
 * 
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a>
 * 
 *         Tests for module deployment of resource adapter archive in
 *         uncompressed form with classes in flat form (under package structure)
 * 
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
@ServerSetup(PureFlatTestCase.ModuleAcDeploymentTestCaseSetup.class)
public class PureFlatTestCase extends AbstractModuleDeploymentTestCase {

	private final String cf = "java:/testMeRA";

	static class ModuleAcDeploymentTestCaseSetup extends
			ModuleDeploymentTestCaseSetup {

		@Override
		public void doSetup(ManagementClient managementClient) throws Exception {

			super.doSetup(managementClient);
			fillModuleWithFlatClasses("ra.xml");
			setConfiguration("pure.xml");

		}
	}

	/**
	 * Define the deployment
	 * 
	 * @return The deployment archive
	 */
	@Deployment
	public static JavaArchive createDeployment() throws Exception {
		return createDeployment(PureFlatTestCase.class);
	}

	@Resource(mappedName = cf)
	private ConnectionFactory connectionFactory;

	/**
	 * Tests connection factory
	 * 
	 * @throws Throwable
	 *             in case of an error
	 */
	@Test
	public void testConnectionFactory() throws Throwable {
		testConnectionFactory(connectionFactory);
	}

	/**
	 * Tests connection in pool
	 * 
	 * @throws Exception
	 *             in case of error
	 */
	@Test
	@RunAsClient
	public void testConnection() throws Exception {
		testConnection(cf);
	}

	@Override
	protected ModelNode getAddress() {
		return ModuleAcDeploymentTestCaseSetup.getAddress();
	}

}
