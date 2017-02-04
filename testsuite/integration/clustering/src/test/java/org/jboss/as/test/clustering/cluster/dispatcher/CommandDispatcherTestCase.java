package org.jboss.as.test.clustering.cluster.dispatcher;

import static org.junit.Assert.*;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.clustering.EJBClientContextSelector;
import org.jboss.as.test.clustering.cluster.ClusterAbstractTestCase;
import org.jboss.as.test.clustering.cluster.dispatcher.bean.ClusterTopology;
import org.jboss.as.test.clustering.cluster.dispatcher.bean.ClusterTopologyRetriever;
import org.jboss.as.test.clustering.cluster.dispatcher.bean.ClusterTopologyRetrieverBean;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.as.test.shared.util.DisableInvocationTestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
public class CommandDispatcherTestCase extends ClusterAbstractTestCase {
    private static final long VIEW_CHANGE_WAIT = TimeoutUtil.adjust(2000);
    private static final String MODULE_NAME = "command-dispatcher";
    private static final String CLIENT_PROPERTIES = "cluster/ejb3/stateless/jboss-ejb-client.properties";

    @BeforeClass
    public static void beforeClass() {
        DisableInvocationTestUtil.disable();
    }

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
        ejbJar.addPackage(ClusterTopologyRetriever.class.getPackage());
        return ejbJar;
    }

    @Test
    public void test() throws Exception {

        // TODO Elytron: Once support for legacy EJB properties has been added back, actually set the EJB properties
        // that should be used for this test using CLIENT_PROPERTIES and ensure the EJB client context is reset
        // to its original state at the end of the test
        EJBClientContextSelector.setup(CLIENT_PROPERTIES);

        try (EJBDirectory directory = new RemoteEJBDirectory(MODULE_NAME)) {
            ClusterTopologyRetriever bean = directory.lookupStateless(ClusterTopologyRetrieverBean.class, ClusterTopologyRetriever.class);
            ClusterTopology topology = bean.getClusterTopology();
            assertEquals(2, topology.getNodes().size());
            assertTrue(topology.getNodes().toString(), topology.getNodes().contains(NODE_1));
            assertTrue(topology.getNodes().toString(), topology.getNodes().contains(NODE_2));
            assertFalse(topology.getRemoteNodes().toString() + " should not contain " + topology.getLocalNode(), topology.getRemoteNodes().contains(topology.getLocalNode()));

            undeploy(DEPLOYMENT_2);

            topology = bean.getClusterTopology();
            assertEquals(1, topology.getNodes().size());
            assertTrue(topology.getNodes().contains(NODE_1));
            assertEquals(NODE_1, topology.getLocalNode());
            assertTrue(topology.getRemoteNodes().toString(), topology.getRemoteNodes().isEmpty());

            deploy(DEPLOYMENT_2);

            Thread.sleep(VIEW_CHANGE_WAIT);

            topology = bean.getClusterTopology();
            assertEquals(2, topology.getNodes().size());
            assertTrue(topology.getNodes().contains(NODE_1));
            assertTrue(topology.getNodes().contains(NODE_2));
            assertFalse(topology.getRemoteNodes().toString() + " should not contain " + topology.getLocalNode(), topology.getRemoteNodes().contains(topology.getLocalNode()));

            stop(CONTAINER_1);

            topology = bean.getClusterTopology();
            assertEquals(1, topology.getNodes().size());
            assertTrue(topology.getNodes().contains(NODE_2));
            assertEquals(NODE_2, topology.getLocalNode());
            assertTrue(topology.getRemoteNodes().toString(), topology.getRemoteNodes().isEmpty());

            start(CONTAINER_1);

            Thread.sleep(VIEW_CHANGE_WAIT);

            topology = bean.getClusterTopology();
            assertEquals(topology.getNodes().toString(), 2, topology.getNodes().size());
            assertTrue(topology.getNodes().toString() + " should contain " + NODE_1, topology.getNodes().contains(NODE_1));
            assertTrue(topology.getNodes().toString() + " should contain " + NODE_2, topology.getNodes().contains(NODE_2));
            assertFalse(topology.getRemoteNodes().toString() + " should not contain " + topology.getLocalNode(), topology.getRemoteNodes().contains(topology.getLocalNode()));
        }
    }
}
