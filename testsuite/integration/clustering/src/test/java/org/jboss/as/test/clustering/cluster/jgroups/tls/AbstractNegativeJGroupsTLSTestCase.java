/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.jgroups.tls;

import java.net.URL;
import java.util.Set;

import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.ClusterTestUtil;
import org.jboss.as.test.clustering.TopologyChangeListenerServlet;
import org.jboss.as.test.clustering.TopologyChangeListenerUtil;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.single.web.SimpleServlet;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Base class for tests which configure the JGroups channel such that the nodes are expected to not form a cluster.
 *
 * That the topology of each node is a singleton is verified over HTTP web request by awaiting the topology of the
 * distributed web session cache via {@link TopologyChangeListenerServlet}, rather than using the EJB client, whose
 * discovery does not work reliably with split clusters.
 *
 * @author Radoslav Husar
 * @see <a href="https://redhat.atlassian.net/browse/WFLY-22524">WFLY-22524</a>
 */
@ExtendWith(ArquillianExtension.class)
public abstract class AbstractNegativeJGroupsTLSTestCase extends AbstractClusteringTestCase {

    private final String deploymentName;

    protected AbstractNegativeJGroupsTLSTestCase(String moduleName) {
        this.deploymentName = moduleName + ".war";
    }

    protected static Archive<?> createDeployment(String moduleName) {
        WebArchive war = ShrinkWrap.create(WebArchive.class, moduleName + ".war");
        // Distributable web application provides a clustered cache whose topology can be awaited
        war.setWebXML(SimpleServlet.class.getPackage(), "web.xml");
        ClusterTestUtil.addTopologyListenerDependencies(war);
        return war;
    }

    @Test
    void nodesDoNotCluster(
            @ArquillianResource(TopologyChangeListenerServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource(TopologyChangeListenerServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2
    ) throws Exception {
        // These servers should never cluster, i.e. the topology of each node consists of itself only
        TopologyChangeListenerUtil.establishTopology(baseURL1, "web", this.deploymentName, Set.of(NODE_1));
        TopologyChangeListenerUtil.establishTopology(baseURL2, "web", this.deploymentName, Set.of(NODE_2));
    }
}
