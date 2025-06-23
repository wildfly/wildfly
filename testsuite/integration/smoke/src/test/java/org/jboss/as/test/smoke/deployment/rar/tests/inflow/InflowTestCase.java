/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.deployment.rar.tests.inflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.Set;
import jakarta.resource.spi.ActivationSpec;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.test.smoke.deployment.rar.inflow.PureInflowResourceAdapter;
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a>
 *         JBQA-5741 -Inflow RA deployment test
 */
@ExtendWith(ArquillianExtension.class)
public class InflowTestCase {


    /**
     * Define the deployment
     *
     * @return The deployment archive
     */
    @Deployment
    public static ResourceAdapterArchive createDeployment() throws Exception {
        String deploymentName = "inflow.rar";

        ResourceAdapterArchive raa =
                ShrinkWrap.create(ResourceAdapterArchive.class, deploymentName);
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "multiple.jar");
        ja.addPackage(PureInflowResourceAdapter.class.getPackage()).
                addClasses(InflowTestCase.class);
        raa.addAsLibrary(ja);

        raa.addAsManifestResource(InflowTestCase.class.getPackage(), "ra.xml", "ra.xml")
                .addAsManifestResource(InflowTestCase.class.getPackage(), "ironjacamar.xml", "ironjacamar.xml")
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
        Set<String> ids = repository.getResourceAdapters(jakarta.jms.MessageListener.class);

        assertNotNull(ids);
        int pureInflowListener = 0;
        for (String id : ids) {
            if (id.indexOf("PureInflow") != -1) { pureInflowListener++; }
        }
        assertEquals(1, pureInflowListener);

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
