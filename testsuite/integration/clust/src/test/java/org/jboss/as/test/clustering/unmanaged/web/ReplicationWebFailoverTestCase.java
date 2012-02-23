package org.jboss.as.test.clustering.unmanaged.web;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.as.test.clustering.cluster.web.ClusteredWebTestCase;
import org.jboss.as.test.clustering.single.web.SimpleServlet;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import static org.jboss.as.test.clustering.ClusteringTestConstants.*;

public class ReplicationWebFailoverTestCase extends ClusteredWebFailoverTestCase {

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(CONTAINER_1)
    public static Archive<?> deployment0() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "distributable.war");
        war.addClass(SimpleServlet.class);
        // Take web.xml from the managed test.
        war.setWebXML(ClusteredWebTestCase.class.getPackage(), "web.xml");
        System.out.println(war.toString(true));
        return war;
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(CONTAINER_2)
    public static Archive<?> deployment1() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "distributable.war");
        war.addClass(SimpleServlet.class);
        war.setWebXML(ClusteredWebTestCase.class.getPackage(), "web.xml");
        System.out.println(war.toString(true));
        return war;
    }

}
