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
package org.jboss.as.test.integration.jca.beanvalidation;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Set;
import javax.annotation.Resource;
import javax.resource.spi.ActivationSpec;

import org.hibernate.validator.HibernateValidatorPermission;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.test.integration.jca.beanvalidation.ra.ValidActivationSpec;
import org.jboss.as.test.integration.jca.beanvalidation.ra.ValidAdminObjectInterface;
import org.jboss.as.test.integration.jca.beanvalidation.ra.ValidConnection;
import org.jboss.as.test.integration.jca.beanvalidation.ra.ValidConnectionFactory;
import org.jboss.as.test.integration.jca.beanvalidation.ra.ValidMessageEndpoint;
import org.jboss.as.test.integration.jca.beanvalidation.ra.ValidMessageEndpointFactory;
import org.jboss.jca.core.spi.mdr.MetadataRepository;
import org.jboss.jca.core.spi.rar.Endpoint;
import org.jboss.jca.core.spi.rar.MessageListener;
import org.jboss.jca.core.spi.rar.ResourceAdapterRepository;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a> JBQA-5904
 */
@RunWith(Arquillian.class)
public class PositiveValidationTestCase {

    @ArquillianResource
    ServiceContainer serviceContainer;

    /**
     * Define the deployment
     *
     * @return The deployment archive
     */
    @Deployment
    public static ResourceAdapterArchive createDeployment() throws Exception {
        String deploymentName = "valid.rar";

        ResourceAdapterArchive raa = ShrinkWrap.create(ResourceAdapterArchive.class, deploymentName);
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "valid.jar");
        ja.addPackage(ValidConnectionFactory.class.getPackage()).addClasses(PositiveValidationTestCase.class);
        raa.addAsLibrary(ja);

        raa.addAsManifestResource(PositiveValidationTestCase.class.getPackage(), "ra.xml", "ra.xml")
                .addAsManifestResource(PositiveValidationTestCase.class.getPackage(), "ironjacamar.xml", "ironjacamar.xml")
                .addAsManifestResource(new StringAsset("Dependencies: javax.inject.api,org.jboss.as.connector\n"), "MANIFEST.MF");

        raa.addAsManifestResource(createPermissionsXmlAsset(
                HibernateValidatorPermission.ACCESS_PRIVATE_MEMBERS
        ), "permissions.xml");

        return raa;
    }

    @Resource(mappedName = "java:jboss/VCF")
    private ValidConnectionFactory connectionFactory;
    @Resource(mappedName = "java:jboss/VAO")
    ValidAdminObjectInterface adminObject;

    /**
     * Test configuration
     *
     * @throws Throwable Thrown if case of an error
     */
    @Test
    public void testConfiguration() throws Throwable {
        assertNotNull("CF not found", connectionFactory);
        assertNotNull("AO not found", adminObject);
        ValidConnection con = connectionFactory.getConnection();
        assertEquals("admin", adminObject.getAoProperty());
        assertEquals(4, con.getResourceAdapterProperty());
        assertEquals("prop", con.getManagedConnectionFactoryProperty());
        con.close();
    }

    @Test
    public void testRegistryConfiguration() throws Throwable {
        ServiceController<?> controller = serviceContainer.getService(ConnectorServices.RA_REPOSITORY_SERVICE);
        assertNotNull(controller);
        ResourceAdapterRepository repository = (ResourceAdapterRepository) controller.getValue();
        assertNotNull(repository);
        Set<String> ids = repository.getResourceAdapters(javax.jms.MessageListener.class);

        assertNotNull(ids);

        String piId = ids.iterator().next();
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

        ValidActivationSpec vas = (ValidActivationSpec) as;

        ValidMessageEndpoint me = new ValidMessageEndpoint();
        ValidMessageEndpointFactory mef = new ValidMessageEndpointFactory(me);

        endpoint.activate(mef, vas);
        endpoint.deactivate(mef, vas);
    }

    @Test
    public void testMetadataConfiguration() throws Throwable {
        ServiceController<?> controller = serviceContainer.getService(ConnectorServices.IRONJACAMAR_MDR);
        assertNotNull(controller);
        MetadataRepository repository = (MetadataRepository) controller.getValue();
        assertNotNull(repository);
        Set<String> ids = repository.getResourceAdapters();

        assertNotNull(ids);

        String piId = ids.iterator().next();
        assertNotNull(piId);
        assertNotNull(repository.getResourceAdapter(piId));
    }
}
