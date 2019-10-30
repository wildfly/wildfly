package org.jboss.as.test.clustering.cluster.registry;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.*;

import java.util.Collection;
import java.util.PropertyPermission;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
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
public class RegistryTestCase extends AbstractClusteringTestCase {
    private static final String MODULE_NAME = RegistryTestCase.class.getSimpleName();

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

            stop(NODE_2);

            names = bean.getNodes();
            assertEquals(1, names.size());
            assertTrue(names.contains(NODE_1));

            start(NODE_2);

            names = bean.getNodes();
            assertEquals(2, names.size());
            assertTrue(names.contains(NODE_1));
            assertTrue(names.contains(NODE_2));
        }
    }
}
