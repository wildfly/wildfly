package org.jboss.as.test.clustering.cluster.session.persistence;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.clustering.ClusterTestUtil;
import org.jboss.as.test.clustering.cluster.web.ClusteredWebFailoverAbstractCase;
import org.jboss.as.test.clustering.single.web.Mutable;
import org.jboss.as.test.clustering.single.web.SimpleServlet;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
@org.junit.Ignore("WFLY-2409")
public class SessionClusterDbPersistenceTestCase extends ClusteredWebFailoverAbstractCase {
    private static final String DEPLOYMENT_NAME = "session-db-cluster.war";

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(CONTAINER_1)
    public static Archive<?> deployment0() {
        return getDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(CONTAINER_2)
    public static Archive<?> deployment1() {
        return getDeployment();
    }

    private static Archive<?> getDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME);
        war.addClasses(SimpleServlet.class, Mutable.class);
        ClusterTestUtil.addTopologyListenerDependencies(war);
        war.setWebXML(SessionClusterDbPersistenceTestCase.class.getPackage(), "web.xml");
        war.addAsWebInfResource("WEB-INF/jboss-web.xml","jboss-web.xml");
        log.info(war.toString(true));
        return war;
    }

    @Override
    protected String getDeploymentName() {
        return DEPLOYMENT_NAME;
    }
}
