/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.ejb.mdb.activationname;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.test.integration.ejb.mdb.activationname.adapter.SimpleListener;
import org.jboss.as.test.integration.ejb.mdb.activationname.adapter.SimpleResourceAdapter;
import org.jboss.as.test.integration.ejb.mdb.activationname.mdb.SimpleMdb;
import org.jboss.jca.core.spi.rar.Endpoint;
import org.jboss.jca.core.spi.rar.ResourceAdapterRepository;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.resource.spi.ActivationSpec;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * This tests if IronJacamar follows JCA 1.7 and properly defines {@code activation name} on {@code MessageEndpointFactory}.
 * For details see {@link SimpleResourceAdapter#endpointActivation(MessageEndpointFactory, ActivationSpec)} and WFLY-8074.
 *
 * @author Ivo Studensky
 */
@RunWith(Arquillian.class)
public class ActivationNameTestCase {

    @Deployment
    public static Archive createDeplyoment() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "ear-with-simple-adapter.ear")
                .addAsModule(ShrinkWrap.create(JavaArchive.class, "simple-adapter.rar")
                        .addAsManifestResource(SimpleResourceAdapter.class.getPackage(), "ra.xml", "ra.xml")
                        .addPackage(SimpleResourceAdapter.class.getPackage()))
                .addAsModule(ShrinkWrap.create(JavaArchive.class, "mdb.jar")
                        .addClasses(SimpleMdb.class, ActivationNameTestCase.class)
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.connector, org.jboss.ironjacamar.api\n"), "MANIFEST.MF"));

        return ear;
    }

    @ArquillianResource
    ServiceContainer serviceContainer;

    @Test
    public void testSimpleResourceAdapterAvailability() throws Exception {
        ServiceController<?> controller = serviceContainer.getService(ConnectorServices.RA_REPOSITORY_SERVICE);
        assertNotNull(controller);
        ResourceAdapterRepository repository = (ResourceAdapterRepository) controller.getValue();
        assertNotNull(repository);
        Set<String> ids = repository.getResourceAdapters(SimpleListener.class);
        assertNotNull(ids);
        assertEquals(1, ids.size());

        String piId = ids.iterator().next();
        assertTrue(piId.indexOf("SimpleResourceAdapter") != -1);

        Endpoint endpoint = repository.getEndpoint(piId);
        assertNotNull(endpoint);
    }
}
