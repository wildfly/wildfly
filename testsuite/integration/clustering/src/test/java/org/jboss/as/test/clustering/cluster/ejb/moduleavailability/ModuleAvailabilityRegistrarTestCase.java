/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.ejb.moduleavailability;

import java.util.Set;
import java.util.HashSet;
import java.util.PropertyPermission;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.ejb.moduleavailability.bean.ModuleAvailabilityRegistrarRetriever;
import org.jboss.as.test.clustering.cluster.ejb.moduleavailability.bean.ModuleAvailabilityRegistrarRetrieverBean;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;
import org.jboss.as.test.shared.PermissionUtils;
import org.jboss.ejb.client.EJBModuleIdentifier;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * A test case to validate the bahavour of the ModuleAvailabilityRegistrar service, a service which uses
 * a ServiceProviderRegistar to prvide distrubuted access to all modules deployed in a cluster.
 *
 * The events of importance for this service are:
 *
 * * deployment (resp. undeployment) of EJB modules on a node in the cluster: representations of modules
 * should be added (resp. removed) from the service
 *
 * * suspend (resp. resume) of a node in the cluster: representations of all deployed modules should be
 * removed (resp. added) to the service
 *
 * NOTE: it is posssible for EJB modules to be seployed on a node when theserver is suspended, and the odules
 * represented by service should reflect this fact.
 */
@RunWith(Arquillian.class)
public class ModuleAvailabilityRegistrarTestCase extends AbstractClusteringTestCase {
    private static final Logger LOGGER = Logger.getLogger(ModuleAvailabilityRegistrarTestCase.class);
    private static final String MODULE_NAME = ModuleAvailabilityRegistrarTestCase.class.getSimpleName();

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> createDeploymentForContainer1() {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> createDeploymentForContainer2() {
        return createDeployment();
    }

    /*
     * An EJB deployment which has two functions:
     * - to allow a remote client to interrgate the ModuleAvailabilityRegistrar
     * - to represent a generic deployment on the server which provides content for the ModuleAvailabilityRegistrar
     */
    private static Archive<?> createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar")
                .addPackage(ModuleAvailabilityRegistrarRetrieverBean.class.getPackage())
                .addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(new PropertyPermission(NODE_NAME_PROPERTY, "read"), new RuntimePermission("getClassLoader")), "permissions.xml")
                ;
    }

    @Test
    public void test() throws Exception {
        this.test(ModuleAvailabilityRegistrarRetrieverBean.class);
    }

    /**
     * Test the expected behaviour of the ModuleAvailabilityRegistrar service when running in a two node cluster
     *
     * @param beanClass the server side bean providing access to the ModuleAvailabilityRegistrar service
     * @throws Exception
     */
    public void test(Class<? extends ModuleAvailabilityRegistrarRetriever> beanClass) throws Exception {
        try (EJBDirectory context = new RemoteEJBDirectory(MODULE_NAME)) {
            ModuleAvailabilityRegistrarRetriever bean = context.lookupStateless(beanClass, ModuleAvailabilityRegistrarRetriever.class);

            // the EJBModuleIdentifier of the deployments used in this test
            EJBModuleIdentifier moduleIdentifier = new EJBModuleIdentifier("",MODULE_NAME,"");
            Set<String> modules = new HashSet();
            Set<String> providers = new HashSet();

            // cluster starts with deployments on NODE_1 and NODE_2

            LOGGER.info("Calling getServices()");
            modules = bean.getServices();
            LOGGER.info("Calling getProviders()");
            providers = bean.getProviders(moduleIdentifier);
            assertEquals(1, modules.size());
            assertTrue(providers.contains(NODE_1));
            assertTrue(providers.contains(NODE_2));

            LOGGER.info("Undeploying node-1");
            undeploy(DEPLOYMENT_1);

            LOGGER.info("Calling getServices()");
            modules = bean.getServices();
            LOGGER.info("Calling getProviders()");
            providers = bean.getProviders(moduleIdentifier);
            assertEquals(1, modules.size());
            assertTrue(providers.contains(NODE_2));

            LOGGER.info("Deploying node-1");
            deploy(DEPLOYMENT_1);

            LOGGER.info("Calling getServices()");
            modules = bean.getServices();
            LOGGER.info("Calling getProviders()");
            providers = bean.getProviders(moduleIdentifier);
            assertEquals(1, modules.size());
            assertTrue(providers.contains(NODE_1));
            assertTrue(providers.contains(NODE_2));

            LOGGER.info("Stopping node-2");
            stop(NODE_2);

            LOGGER.info("Calling getServices()");
            modules = bean.getServices();
            LOGGER.info("Calling getProviders()");
            providers = bean.getProviders(moduleIdentifier);
            assertEquals(1, modules.size());
            assertTrue(providers.contains(NODE_1));

            LOGGER.info("Starting node-2");
            start(NODE_2);

            LOGGER.info("Calling getServices()");
            modules = bean.getServices();
            LOGGER.info("Calling getProviders()");
            providers = bean.getProviders(moduleIdentifier);
            assertEquals(1, modules.size());
            assertTrue(providers.contains(NODE_1));
            assertTrue(providers.contains(NODE_2));

        }
    }
}
