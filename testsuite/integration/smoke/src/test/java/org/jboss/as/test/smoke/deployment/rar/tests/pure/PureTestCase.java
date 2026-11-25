/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.deployment.rar.tests.pure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Set;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.test.smoke.deployment.rar.inflow.PureInflowResourceAdapter;
import org.jboss.jca.core.spi.mdr.MetadataRepository;
import org.jboss.jca.core.spi.rar.ResourceAdapterRepository;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a>
 *         JBQA-5742 -Pure RA deployment test
 */
@ExtendWith(ArquillianExtension.class)
public class PureTestCase {

    /**
     * Define the deployment
     *
     * @return The deployment archive
     */
    @Deployment
    public static ResourceAdapterArchive createDeployment() {
        String deploymentName = "pure.rar";

        ResourceAdapterArchive raa =
                ShrinkWrap.create(ResourceAdapterArchive.class, deploymentName);
        JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class, "multiple.jar");
        javaArchive.addClasses(PureTestCase.class);
        javaArchive.addPackage(PureInflowResourceAdapter.class.getPackage());

        raa.addAsLibrary(javaArchive);

        raa.addAsManifestResource(PureTestCase.class.getPackage(), "ra.xml", "ra.xml")
                .addAsManifestResource(new StringAsset("Dependencies: javax.inject.api,org.jboss.as.connector\n"), "MANIFEST.MF");

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
        ServiceController<?> controller = serviceContainer.getService(ConnectorServices.RA_REPOSITORY_SERVICE);
        assertNotNull(controller);
        ResourceAdapterRepository repository = (ResourceAdapterRepository) controller.getValue();
        assertNotNull(repository);
        Set<String> ids = repository.getResourceAdapters();

        assertNotNull(ids);
        int pureInflowListener = 0;
        for (String id : ids) {
            if (id.indexOf("PureInflow") != -1) { pureInflowListener++; }
        }
        assertEquals(1, pureInflowListener);

        for (String piId : ids) {
            assertNotNull(piId);
            assertNotNull(repository.getResourceAdapter(piId));
        }

    }

    @Test
    public void testMetadataConfiguration() throws Throwable {
        ServiceController<?> controller = serviceContainer.getService(ConnectorServices.IRONJACAMAR_MDR);
        assertNotNull(controller);
        MetadataRepository repository = (MetadataRepository) controller.getValue();
        assertNotNull(repository);
        Set<String> ids = repository.getResourceAdapters();

        assertNotNull(ids);

        for (String piId : ids) {
            assertNotNull(piId);
            assertNotNull(repository.getResourceAdapter(piId));
        }
    }
}
