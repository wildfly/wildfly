package org.jboss.as.test.clustering.cluster.ejb3.stateful.remote.failover;

import javax.annotation.PreDestroy;
import javax.ejb.Remote;
import javax.ejb.Stateful;

import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.ejb3.annotation.Clustered;
import org.jboss.logging.Logger;

/**
 * @author: Jaikiran Pai
 */
@Stateful
@Clustered
@Remote(NodeNameRetriever.class)
public class SlowUndeployingClusteredSFSB implements NodeNameRetriever {

    private static final Logger logger = Logger.getLogger(SlowUndeployingClusteredSFSB.class);

    @Override
    public String getNodeName() {
        return System.getProperty("jboss.node.name");
    }

    @PreDestroy
    private void preDestroy() throws Exception {
        final int sleepTime = TimeoutUtil.adjust(3000);
        logger.info("Going to sleep for " + sleepTime + " to intentionally slow down the undeployment and test failover cases");
        // slow down the undeployment process
        Thread.sleep(sleepTime);
    }
}
