package org.jboss.as.test.clustering.cluster.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.clustering.EJBClientContextSelector;
import org.jboss.as.test.clustering.cluster.ClusterAbstractTestCase;
import org.jboss.as.test.clustering.cluster.provider.bean.ServiceProviderRetriever;
import org.jboss.as.test.clustering.cluster.provider.bean.ServiceProviderRetrieverBean;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
public class ServiceProviderRegistrationTestCase extends ClusterAbstractTestCase {
    private static final Logger log = Logger.getLogger(ServiceProviderRegistrationTestCase.class);
    private static final String MODULE_NAME = "service-provider-registration";
    private static final String CLIENT_PROPERTIES = "cluster/ejb3/stateless/jboss-ejb-client.properties";

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
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        ejbJar.addPackage(ServiceProviderRetriever.class.getPackage());
        log.info(ejbJar.toString(true));
        return ejbJar;
    }

    @Test
    public void test() throws Exception {

        String cluster = "server";
        String nodeNameFormat = "%s/%s";
        String nodeName1 = String.format(nodeNameFormat, NODE_1, cluster);
        String nodeName2 = String.format(nodeNameFormat, NODE_2, cluster);

        ContextSelector<EJBClientContext> selector = EJBClientContextSelector.setup(CLIENT_PROPERTIES);

        try (EJBDirectory directory = new RemoteEJBDirectory(MODULE_NAME)) {
            ServiceProviderRetriever bean = directory.lookupStateless(ServiceProviderRetrieverBean.class, ServiceProviderRetriever.class);
            Collection<String> names = bean.getProviders();
            assertEquals(2, names.size());
            assertTrue(names.toString(), names.contains(nodeName1));
            assertTrue(names.toString(), names.contains(nodeName2));
            
            undeploy(DEPLOYMENT_1);
            
            names = bean.getProviders();
            assertEquals(1, names.size());
            assertTrue(names.contains(nodeName2));
            
            deploy(DEPLOYMENT_1);
            
            names = bean.getProviders();
            assertEquals(2, names.size());
            assertTrue(names.contains(nodeName1));
            assertTrue(names.contains(nodeName2));
            
            stop(CONTAINER_2);
            
            names = bean.getProviders();
            assertEquals(1, names.size());
            assertTrue(names.contains(nodeName1));
            
            start(CONTAINER_2);
            
            names = bean.getProviders();
            assertEquals(2, names.size());
            assertTrue(names.contains(nodeName1));
            assertTrue(names.contains(nodeName2));
        } finally {
            // reset the selector
            if (selector != null) {
                EJBClientContext.setSelector(selector);
            }
        }
    }
}
