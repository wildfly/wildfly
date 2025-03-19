/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.ejb.moduleavailability;

//import java.util.List;
//import java.util.Map;
import java.util.Set;
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
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
// import static org.junit.Assert.assertTrue;

@RunWith(Arquillian.class)
public class ModuleAvailabilityRegistrarTestCase extends AbstractClusteringTestCase {
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

    public void test(Class<? extends ModuleAvailabilityRegistrarRetriever> beanClass) throws Exception {
        try (EJBDirectory context = new RemoteEJBDirectory(MODULE_NAME)) {
            ModuleAvailabilityRegistrarRetriever bean = context.lookupStateless(beanClass, ModuleAvailabilityRegistrarRetriever.class);
            // test the ModuleAvailabilityRegistrar ServiceProviderRegistry behaviour

            Set<String> modules = bean.getServices();
            assertEquals(2, modules.size());
//            assertTrue(names.toString(), names.contains(NODE_1));
//            assertTrue(names.toString(), names.contains(NODE_2));

            undeploy(DEPLOYMENT_1);

            modules = bean.getServices();
            assertEquals(1, modules.size());
//            assertTrue(names.contains(NODE_2));

            deploy(DEPLOYMENT_1);

            modules = bean.getServices();
            assertEquals(2, modules.size());
//            assertTrue(names.contains(NODE_1));
//            assertTrue(names.contains(NODE_2));

            stop(NODE_2);

            modules = bean.getServices();
            assertEquals(1, modules.size());
//            assertTrue(names.contains(NODE_1));

            start(NODE_2);

            modules = bean.getServices();
            assertEquals(2, modules.size());
//            assertTrue(names.contains(NODE_1));
//            assertTrue(names.contains(NODE_2));

        }
    }

}
