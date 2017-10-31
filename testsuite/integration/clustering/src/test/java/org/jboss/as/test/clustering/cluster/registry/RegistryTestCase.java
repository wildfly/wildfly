package org.jboss.as.test.clustering.cluster.registry;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.PropertyPermission;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.clustering.cluster.ClusterAbstractTestCase;
import org.jboss.as.test.clustering.cluster.registry.bean.RegistryRetriever;
import org.jboss.as.test.clustering.cluster.registry.bean.RegistryRetrieverBean;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
public class RegistryTestCase extends ClusterAbstractTestCase {
    private static final String MODULE_NAME = "registry";

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(CONTAINER_1)
    public static Archive<?> createDeploymentForContainer1() {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(CONTAINER_2)
    public static Archive<?> createDeploymentForContainer2() {
        return createDeployment();
    }

    private static Archive<?> createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar")
                .addPackage(RegistryRetriever.class.getPackage())
                .addAsManifestResource(createPermissionsXmlAsset(new PropertyPermission(NODE_NAME_PROPERTY, "read")), "permissions.xml")
                ;
    }

    @Test
    public void test() throws Exception {
        try (EJBDirectory context = new RemoteEJBDirectory(MODULE_NAME)) {
            RegistryRetriever bean = context.lookupStateless(RegistryRetrieverBean.class, RegistryRetriever.class);

            Collection<String> names = bean.getNodes();
            assertEquals(2, names.size());
            assertTrue(names.toString(), names.contains(NODE_1));
            assertTrue(names.toString(), names.contains(NODE_2));

            undeploy(DEPLOYMENT_1);

            names = bean.getNodes();
            assertEquals(1, names.size());
            assertTrue(names.contains(NODE_2));

            deploy(DEPLOYMENT_1);

            names = bean.getNodes();
            assertEquals(2, names.size());
            assertTrue(names.contains(NODE_1));
            assertTrue(names.contains(NODE_2));

            stop(CONTAINER_2);

            names = bean.getNodes();
            assertEquals(1, names.size());
            assertTrue(names.contains(NODE_1));

            start(CONTAINER_2);

            names = bean.getNodes();
            assertEquals(2, names.size());
            assertTrue(names.contains(NODE_1));
            assertTrue(names.contains(NODE_2));
        }
    }
}
