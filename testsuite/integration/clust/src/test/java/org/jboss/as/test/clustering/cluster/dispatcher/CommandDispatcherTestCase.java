package org.jboss.as.test.clustering.cluster.dispatcher;

import static org.junit.Assert.*;

import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.as.test.clustering.EJBClientContextSelector;
import org.jboss.as.test.clustering.EJBDirectory;
import org.jboss.as.test.clustering.RemoteEJBDirectory;
import org.jboss.as.test.clustering.ViewChangeListener;
import org.jboss.as.test.clustering.ViewChangeListenerBean;
import org.jboss.as.test.clustering.cluster.ClusterAbstractTestCase;
import org.jboss.as.test.clustering.cluster.dispatcher.bean.ClusterTopology;
import org.jboss.as.test.clustering.cluster.dispatcher.bean.ClusterTopologyRetriever;
import org.jboss.as.test.clustering.cluster.dispatcher.bean.ClusterTopologyRetrieverBean;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
@Ignore("WFLY-1690")
public class CommandDispatcherTestCase extends ClusterAbstractTestCase {
    private static final Logger log = Logger.getLogger(CommandDispatcherTestCase.class);
    private static final String MODULE_NAME = "command-dispatcher";
    private static final String CLIENT_PROPERTIES = "cluster/ejb3/stateless/jboss-ejb-client.properties";
    private static EJBDirectory context;

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
        ejbJar.addClasses(ViewChangeListener.class, ViewChangeListenerBean.class);
        ejbJar.setManifest(new StringAsset("Manifest-Version: 1.0\nDependencies: org.wildfly.clustering.api, org.jboss.msc, org.jboss.as.clustering.common, org.infinispan\n"));
        log.info(ejbJar.toString(true));
        return ejbJar;
    }

    @BeforeClass
    public static void beforeClass() throws NamingException {
        context = new RemoteEJBDirectory(MODULE_NAME);
    }

    @AfterClass
    public static void destroy() throws NamingException {
        context.close();
    }

    @Override
    protected void setUp() {
        super.setUp();

        // Also deploy
        deploy(DEPLOYMENTS);
    }

    @Test
    @InSequence(1)
    public void test() throws Exception {

        String cluster = "ejb";
        String nodeNameFormat = "%s/%s";
        String nodeName1 = String.format(nodeNameFormat, NODE_1, cluster);
        String nodeName2 = String.format(nodeNameFormat, NODE_2, cluster);

        ContextSelector<EJBClientContext> selector = EJBClientContextSelector.setup(CLIENT_PROPERTIES);

        try {
            ViewChangeListener listener = context.lookupStateless(ViewChangeListenerBean.class, ViewChangeListener.class);
            listener.establishView(cluster, NODE_1, NODE_2);
            ClusterTopologyRetriever bean = context.lookupStateless(ClusterTopologyRetrieverBean.class, ClusterTopologyRetriever.class);
            ClusterTopology topology = bean.getClusterTopology();
            assertEquals(2, topology.getNodes().size());
            assertTrue(topology.getNodes().contains(nodeName1));
            assertTrue(topology.getNodes().contains(nodeName2));
            assertFalse(topology.getRemoteNodes().toString() + " should not contain " + topology.getLocalNode(), topology.getRemoteNodes().contains(topology.getLocalNode()));

            undeploy(DEPLOYMENT_2);

            listener.establishView(cluster, NODE_1);

            topology = bean.getClusterTopology();
            assertEquals(1, topology.getNodes().size());
            assertTrue(topology.getNodes().contains(nodeName1));
            assertEquals(nodeName1, topology.getLocalNode());
            assertTrue(topology.getRemoteNodes().toString(), topology.getRemoteNodes().isEmpty());

            deploy(DEPLOYMENT_2);

            listener.establishView(cluster, NODE_1, NODE_2);

            topology = bean.getClusterTopology();
            assertEquals(2, topology.getNodes().size());
            assertTrue(topology.getNodes().contains(nodeName1));
            assertTrue(topology.getNodes().contains(nodeName2));
            assertFalse(topology.getRemoteNodes().toString() + " should not contain " + topology.getLocalNode(), topology.getRemoteNodes().contains(topology.getLocalNode()));

            stop(CONTAINER_1);

            listener.establishView(cluster, NODE_2);

            topology = bean.getClusterTopology();
            assertEquals(1, topology.getNodes().size());
            assertTrue(topology.getNodes().contains(nodeName2));
            assertEquals(nodeName2, topology.getLocalNode());
            assertTrue(topology.getRemoteNodes().toString(), topology.getRemoteNodes().isEmpty());

            start(CONTAINER_1);

            listener.establishView(cluster, NODE_1, NODE_2);

            topology = bean.getClusterTopology();
            assertEquals(2, topology.getNodes().size());
            assertTrue(topology.getNodes().contains(nodeName1));
            assertTrue(topology.getNodes().contains(nodeName2));
            assertFalse(topology.getRemoteNodes().toString() + " should not contain " + topology.getLocalNode(), topology.getRemoteNodes().contains(topology.getLocalNode()));
        } finally {
            // reset the selector
            if (selector != null) {
                EJBClientContext.setSelector(selector);
            }
        }
    }
}
