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

import java.util.Iterator;
import java.util.Set;

import org.jboss.as.test.integration.jca.JcaMgmtBase;
import org.jboss.as.test.integration.jca.JcaMgmtServerSetupTask;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;

import javax.naming.InitialContext;
import javax.resource.cci.ConnectionFactory;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * AS7-5768 -Support for RA module deployment
 * 
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a>
 * 
 */
public abstract class AbstractModuleDeploymentTestCase extends
		ContainerResourceMgmtTestBase {

	/**
	 * Define the deployment
	 * 
	 * @return The deployment archive
	 */
	public static JavaArchive createDeployment(
			Class<? extends AbstractModuleDeploymentTestCase> clazz)
			throws Exception {

		JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "multiple.jar");
		ja.addClasses(clazz, JcaMgmtServerSetupTask.class, JcaMgmtBase.class,
				MgmtOperationException.class, XMLElementReader.class,
				XMLElementWriter.class, AbstractModuleDeploymentTestCase.class,
				ModuleDeploymentTestCaseSetup.class);

		ja.addPackage(AbstractMgmtTestBase.class.getPackage());

		ja.addAsManifestResource(
				new StringAsset(
						"Dependencies: org.jboss.as.controller-client,org.jboss.dmr,org.jboss.as.cli,javax.inject.api,org.jboss.as.connector\n"),
				"MANIFEST.MF");

		return ja;

	}

	/**
	 * Test configuration
	 * 
	 * @throws Throwable
	 *             in case of an error
	 */
	public void testConnectionFactory(ConnectionFactory connectionFactory)
			throws Throwable {
		assertNotNull(connectionFactory);
		assertNotNull(connectionFactory.getConnection());
	}

	/**
	 * Tests connection in pool
	 * 
	 * @throws Exception
	 *             in case of error
	 */
	public void testConnection(String conName) throws Exception {
		final ModelNode address1 = getAddress().clone();
		address1.add("connection-definitions", conName);
		address1.protect();

		final ModelNode operation1 = new ModelNode();
		operation1.get(OP).set("test-connection-in-pool");
		operation1.get(OP_ADDR).set(address1);
		executeOperation(operation1);
	}

	/**
	 * Returns basic address of resource adapter
	 * 
	 * @return address
	 */
	abstract protected ModelNode getAddress();

	/**
	 * Finding object by JNDI name and checks, if its String representation
	 * contains expected substrings
	 * 
	 * @param jndiName
	 *            of object
	 * @param contains
	 *            - substring, must be contained
	 * @throws Exception
	 */
	public void testJndiObject(String jndiName, String... contains)
			throws Exception {
		Object o = new InitialContext().lookup(jndiName);
		assertNotNull(o);
		for (String c : contains)
			assertTrue(o.toString() + " should contain " + c, o.toString()
					.contains(c));

	}

	/**
	 * Checks Set if there is a String element, containing some substring and
	 * returns it
	 * 
	 * @param ids
	 *            - Set
	 * @param contain
	 *            - substring
	 * @return String
	 */
	public String getElementContaining(Set<String> ids, String contain) {
		Iterator<String> it = ids.iterator();
		while (it.hasNext()) {
			String t = it.next();
			if (t.contains(contain)) {
				return t;
			}
		}
		return null;
	}

}
