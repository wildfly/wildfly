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
public class CommandDispatcherTestCase extends ClusterAbstractTestCase {
    private static final long VIEW_CHANGE_WAIT = TimeoutUtil.adjust(2000);
    private static final Logger log = Logger.getLogger(CommandDispatcherTestCase.class);
    private static final String MODULE_NAME = "command-dispatcher";
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
        ejbJar.addPackage(ClusterTopologyRetriever.class.getPackage());
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
            ClusterTopologyRetriever bean = directory.lookupStateless(ClusterTopologyRetrieverBean.class, ClusterTopologyRetriever.class);
            ClusterTopology topology = bean.getClusterTopology();
            assertEquals(2, topology.getNodes().size());
            assertTrue(topology.getNodes().toString(), topology.getNodes().contains(nodeName1));
            assertTrue(topology.getNodes().toString(), topology.getNodes().contains(nodeName2));
            assertFalse(topology.getRemoteNodes().toString() + " should not contain " + topology.getLocalNode(), topology.getRemoteNodes().contains(topology.getLocalNode()));

            undeploy(DEPLOYMENT_2);

            topology = bean.getClusterTopology();
            assertEquals(1, topology.getNodes().size());
            assertTrue(topology.getNodes().contains(nodeName1));
            assertEquals(nodeName1, topology.getLocalNode());
            assertTrue(topology.getRemoteNodes().toString(), topology.getRemoteNodes().isEmpty());

            deploy(DEPLOYMENT_2);

            Thread.sleep(VIEW_CHANGE_WAIT);

            topology = bean.getClusterTopology();
            assertEquals(2, topology.getNodes().size());
            assertTrue(topology.getNodes().contains(nodeName1));
            assertTrue(topology.getNodes().contains(nodeName2));
            assertFalse(topology.getRemoteNodes().toString() + " should not contain " + topology.getLocalNode(), topology.getRemoteNodes().contains(topology.getLocalNode()));

            stop(CONTAINER_1);

            topology = bean.getClusterTopology();
            assertEquals(1, topology.getNodes().size());
            assertTrue(topology.getNodes().contains(nodeName2));
            assertEquals(nodeName2, topology.getLocalNode());
            assertTrue(topology.getRemoteNodes().toString(), topology.getRemoteNodes().isEmpty());

            start(CONTAINER_1);

            Thread.sleep(VIEW_CHANGE_WAIT);

            topology = bean.getClusterTopology();
            assertEquals(topology.getNodes().toString(), 2, topology.getNodes().size());
            assertTrue(topology.getNodes().toString() + " should contain " + nodeName1, topology.getNodes().contains(nodeName1));
            assertTrue(topology.getNodes().toString() + " should contain " + nodeName2, topology.getNodes().contains(nodeName2));
            assertFalse(topology.getRemoteNodes().toString() + " should not contain " + topology.getLocalNode(), topology.getRemoteNodes().contains(topology.getLocalNode()));
        } finally {
            // reset the selector
            if (selector != null) {
                EJBClientContext.setSelector(selector);
            }
        }
    }
}
