package org.jboss.as.test.clustering.cluster.provider;

import static org.junit.Assert.*;

import java.util.Collection;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.provider.bean.ServiceProviderRetriever;
import org.jboss.as.test.clustering.cluster.provider.bean.ServiceProviderRetrieverBean;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class ServiceProviderRegistrationTestCase extends AbstractClusteringTestCase {
    private static final String MODULE_NAME = ServiceProviderRegistrationTestCase.class.getSimpleName();

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
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        ejbJar.addPackage(ServiceProviderRetriever.class.getPackage());
        return ejbJar;
    }

    @Test
    public void test() throws Exception {
        try (EJBDirectory directory = new RemoteEJBDirectory(MODULE_NAME)) {
            ServiceProviderRetriever bean = directory.lookupStateless(ServiceProviderRetrieverBean.class, ServiceProviderRetriever.class);

            Collection<String> names = bean.getProviders();
            assertEquals(2, names.size());
            assertTrue(names.toString(), names.contains(NODE_1));
            assertTrue(names.toString(), names.contains(NODE_2));

            undeploy(DEPLOYMENT_1);

            names = bean.getProviders();
            assertEquals(1, names.size());
            assertTrue(names.contains(NODE_2));

            deploy(DEPLOYMENT_1);

            names = bean.getProviders();
            assertEquals(2, names.size());
            assertTrue(names.contains(NODE_1));
            assertTrue(names.contains(NODE_2));

            stop(NODE_2);

            names = bean.getProviders();
            assertEquals(1, names.size());
            assertTrue(names.contains(NODE_1));

            start(NODE_2);

            names = bean.getProviders();
            assertEquals(2, names.size());
            assertTrue(names.contains(NODE_1));
            assertTrue(names.contains(NODE_2));
        }
    }
}
