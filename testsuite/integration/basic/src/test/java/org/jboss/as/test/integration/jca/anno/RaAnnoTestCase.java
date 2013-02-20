/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.jca.anno;

import org.jboss.jca.common.metadata.ra.ra16.Connector16Impl;
import org.jboss.jca.core.spi.mdr.MetadataRepository;
import org.jboss.jca.core.spi.rar.Endpoint;
import org.jboss.jca.core.spi.rar.MessageListener;
import org.jboss.jca.core.spi.rar.ResourceAdapterRepository;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.resource.spi.ActivationSpec;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.connector.subsystems.resourceadapters.Namespace;
import org.jboss.as.connector.subsystems.resourceadapters.ResourceAdapterSubsystemParser;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.test.integration.jca.annorar.AnnoActivationSpec;
import org.jboss.as.test.integration.jca.annorar.AnnoAdminObject;
import org.jboss.as.test.integration.jca.annorar.AnnoConnectionFactory;
import org.jboss.as.test.integration.jca.annorar.AnnoConnectionImpl;
import org.jboss.as.test.integration.jca.annorar.AnnoManagedConnectionFactory;
import org.jboss.as.test.integration.jca.annorar.AnnoMessageListener;
import org.jboss.as.test.integration.jca.annorar.AnnoMessageListener1;
import org.jboss.as.test.integration.jca.annorar.AnnoResourceAdapter;
import org.jboss.as.test.integration.management.base.AbstractMgmtServerSetupTask;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.shared.FileUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Activation of annotated RA, overridden by descriptor
 * 
 * @author <a href="mailto:vrastsel@redhat.com">Vladimir Rastseluev</a>
 * 
 */
@RunWith(Arquillian.class)
@ServerSetup(RaAnnoTestCase.NoRaAnnoTestCaseSetup.class)
public class RaAnnoTestCase extends ContainerResourceMgmtTestBase {

	/** The logger */
	private static Logger log = Logger.getLogger("RaAnnoTestCase");

	static class NoRaAnnoTestCaseSetup extends AbstractMgmtServerSetupTask {

		private ModelNode address;
		@Override
		public void doSetup(final ManagementClient managementClient)
				throws Exception {
			String xml = FileUtils.readFile(RaAnnoTestCase.class,
					"ra16anno.xml");
			List<ModelNode> operations = xmlToModelOperations(xml,
					Namespace.RESOURCEADAPTERS_1_0.getUriString(),
					new ResourceAdapterSubsystemParser());
			address = operations.get(1).get("address");
			executeOperation(operationListToCompositeOperation(operations));
		}

		@Override
		public void tearDown(final ManagementClient managementClient,
				final String containerId) throws Exception {

			remove(address);
		}
	}

	/**
	 * Define the deployment
	 * 
	 * @return The deployment archive
	 * @throws Exception
	 *             in case of error
	 */
	@Deployment
	public static ResourceAdapterArchive createDeployment() throws Exception {
		ResourceAdapterArchive raa = ShrinkWrap.create(
				ResourceAdapterArchive.class, "ra16anno.rar");
		JavaArchive ja = ShrinkWrap.create(JavaArchive.class);
		ja.addClasses(MgmtOperationException.class, XMLElementReader.class,
				XMLElementWriter.class, RaAnnoTestCase.class);
		ja.addPackage(AbstractMgmtTestBase.class.getPackage()).addPackage(
				AnnoConnectionFactory.class.getPackage());
		raa.addAsLibrary(ja)
				.addAsManifestResource(RaAnnoTestCase.class.getPackage(), "ra.xml", "ra.xml")
				.addAsManifestResource(
						new StringAsset(
								"Dependencies: javax.inject.api,org.jboss.as.connector,org.jboss.as.controller-client,org.jboss.dmr,org.jboss.as.cli\n"),
						"MANIFEST.MF");
		return raa;
	}

	/** Resource */
	@Resource(mappedName = "java:/eis/ra16anno")
	private AnnoConnectionFactory connectionFactory1;

	/** Resource */
	@Resource(mappedName = "java:/eis/ao/ra16anno")
	private AnnoAdminObject adminObject;

	@Inject
	public ServiceContainer serviceContainer;

	/**
	 * Test getConnection
	 * 
	 * @exception Throwable
	 *                Thrown if case of an error
	 */
	@Test
	public void testGetConnection1() throws Throwable {
		assertNotNull(connectionFactory1);
		AnnoConnectionImpl connection1 = (AnnoConnectionImpl) connectionFactory1
				.getConnection();
		assertNotNull(connection1);
		AnnoManagedConnectionFactory mcf = connection1.getMCF();
		assertNotNull(mcf);
		log.info("MCF:" + mcf + "//1//" + mcf.getFirst() + "//2//"
				+ mcf.getSecond());
		assertEquals((byte) 23, (byte) mcf.getFirst());
		assertEquals((short) 55, (short) mcf.getSecond());
		connection1.close();
	}

	/**
	 * Test admin objects
	 * 
	 * @exception Throwable
	 *                Thrown if case of an error
	 */
	@Test
	public void testAdminOjbect() throws Throwable {
		assertNotNull(adminObject);
		log.info("AO:" + adminObject + "//1//" + adminObject.getFirst()
				+ "//2//" + adminObject.getSecond());
		assertEquals((long) 54321, (long) adminObject.getFirst());
		assertEquals(true, adminObject.getSecond());
	}

	/**
	 * test activation 1
	 * 
	 * @exception Throwable
	 *                Thrown if case of an error
	 */
	@Test
	public void testActivation1() throws Throwable {
		testActivation(AnnoMessageListener.class);
	}

	/**
	 * test activation 2
	 * 
	 * @exception Throwable
	 *                Thrown if case of an error
	 */
	//@Test
	public void testActivation2() throws Throwable {
		testActivation(AnnoMessageListener1.class);
	}

	/**
	 * Test activation
	 * 
	 * @param clazz
	 *            class name
	 * 
	 * @exception Throwable
	 *                Thrown if case of an error
	 */
	public void testActivation(Class clazz) throws Throwable {
		ServiceController<?> controller = serviceContainer
				.getService(ConnectorServices.RA_REPOSITORY_SERVICE);
		assertNotNull(controller);
		ResourceAdapterRepository raRepository = (ResourceAdapterRepository) controller
				.getValue();
		Set<String> ids = raRepository.getResourceAdapters(clazz);

		assertNotNull(ids);
		assertEquals(1, ids.size());

		String piId = ids.iterator().next();
		assertNotNull(piId);

		Endpoint endpoint = raRepository.getEndpoint(piId);
		assertNotNull(endpoint);

		List<MessageListener> listeners = raRepository
				.getMessageListeners(piId);
		assertNotNull(listeners);
		assertEquals(1, listeners.size());

		MessageListener listener = listeners.get(0);

		ActivationSpec as = listener.getActivation().createInstance();
		assertNotNull(as);
		assertNotNull(as.getResourceAdapter());

		AnnoActivationSpec tas = (AnnoActivationSpec) as;
		log.info("AS:" + tas + "//1//" + tas.getFirst() + "//2//"
				+ tas.getSecond());
		assertEquals(new Character('U'), tas.getFirst());
		assertEquals(new Double(4.4), tas.getSecond());
		assertTrue(tas.getResourceAdapter() instanceof AnnoResourceAdapter);
		AnnoResourceAdapter tra = (AnnoResourceAdapter) tas
				.getResourceAdapter();
		log.info("RA:" + tra + "//1//" + tra.getFirst() + "//2//"
				+ tra.getSecond());
		assertEquals("G", tra.getFirst());
		assertEquals(new Integer(99), tra.getSecond());
	}

	/**
	 * Test metadata
	 * 
	 * @exception Throwable
	 *                Thrown if case of an error
	 */
	@Test
	public void testMetaData() throws Throwable {
		ServiceController<?> controller = serviceContainer
				.getService(ConnectorServices.IRONJACAMAR_MDR);
		assertNotNull(controller);
		MetadataRepository mdr = (MetadataRepository) controller.getValue();
		assertNotNull(mdr);
		Set<String> ids = mdr.getResourceAdapters();

		assertNotNull(ids);
		assertTrue(ids.size() > 0);
		String piId = getElementContaining(ids, "ra16anno");
		assertNotNull(mdr.getResourceAdapter(piId));
		assertTrue(mdr.getResourceAdapter(piId) instanceof Connector16Impl);
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
