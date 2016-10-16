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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Set;
import javax.resource.spi.ActivationSpec;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.test.integration.jca.beanvalidation.NegativeValidationTestCase;
import org.jboss.as.test.integration.jca.beanvalidation.ra.ValidConnectionFactory;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.core.spi.mdr.MetadataRepository;
import org.jboss.jca.core.spi.rar.Endpoint;
import org.jboss.jca.core.spi.rar.MessageListener;
import org.jboss.jca.core.spi.rar.ResourceAdapterRepository;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
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
 *         modulename/main/module.jar
 */
@RunWith(Arquillian.class)
@ServerSetup(InflowJarTestCase.ModuleAcDeploymentTestCaseSetup.class)
public class InflowJarTestCase extends AbstractModuleDeploymentTestCase {

    static class ModuleAcDeploymentTestCaseSetup extends
            AbstractModuleDeploymentTestCaseSetup {

        @Override
        public void doSetup(ManagementClient managementClient) throws Exception {

            addModule(defaultPath, "module-jar.xml");
            fillModuleWithJar("ra3.xml");
            setConfiguration("inflow.xml");

        }

        @Override
        protected String getSlot() {
            return InflowJarTestCase.class.getSimpleName().toLowerCase();
        }
    }

    /**
     * Define the deployment
     *
     * @return The deployment archive
     */
    @Deployment
    public static Archive<?> createRarDeployment() throws Exception {
        JavaArchive ja = createDeployment(false);

        ResourceAdapterArchive raa = ShrinkWrap.create(
                ResourceAdapterArchive.class, "inflow.rar");
        ja.addPackage(ValidConnectionFactory.class.getPackage());
        raa.addAsLibrary(ja);
        raa.addAsManifestResource(
                NegativeValidationTestCase.class.getPackage(), "ra.xml",
                "ra.xml")
                .addAsManifestResource(
                        new StringAsset(
                                "Dependencies: javax.inject.api,org.jboss.as.connector,org.jboss.as.controller-client,org.jboss.dmr,org.jboss.as.cli\n"),
                        "MANIFEST.MF");

        return raa;
    }

    @ArquillianResource
    ServiceContainer serviceContainer;

    /**
     * Test configuration
     *
     * @throws Throwable Thrown if case of an error
     */
    @Test
    public void testRegistryConfiguration() throws Throwable {
        assertNotNull(serviceContainer);
        ServiceController<?> controller = serviceContainer
                .getService(ConnectorServices.RA_REPOSITORY_SERVICE);
        assertNotNull(controller);
        ResourceAdapterRepository repository = (ResourceAdapterRepository) controller
                .getValue();
        assertNotNull(repository);
        Set<String> ids = repository.getResourceAdapters();

        assertNotNull(ids);
        String piId = getElementContaining(ids, "MultipleResourceAdapter");
        assertNotNull(piId);

        Endpoint endpoint = repository.getEndpoint(piId);
        assertNotNull(endpoint);

        List<MessageListener> listeners = repository.getMessageListeners(piId);
        assertNotNull(listeners);
        assertEquals(1, listeners.size());

        MessageListener listener = listeners.get(0);

        ActivationSpec as = listener.getActivation().createInstance();
        assertNotNull(as);
        assertNotNull(as.getResourceAdapter());
    }

    /**
     * Tests metadata configuration
     *
     * @throws Throwable
     */
    @Test
    public void testMetadataConfiguration() throws Throwable {
        ServiceController<?> controller = serviceContainer
                .getService(ConnectorServices.IRONJACAMAR_MDR);
        assertNotNull(controller);
        MetadataRepository repository = (MetadataRepository) controller
                .getValue();
        assertNotNull(repository);
        Set<String> ids = repository.getResourceAdapters();

        assertNotNull(ids);
        String piId = getElementContaining(ids, "inflow2");
        assertNotNull(piId);
        assertNotNull(repository.getResourceAdapter(piId));
    }

    @Override
    protected ModelNode getAddress() {
        return ModuleAcDeploymentTestCaseSetup.getAddress();
    }

}
