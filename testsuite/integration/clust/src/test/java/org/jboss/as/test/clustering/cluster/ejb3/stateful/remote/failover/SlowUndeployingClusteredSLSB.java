package org.jboss.as.test.clustering.cluster.ejb3.stateful.remote.failover;

import javax.annotation.PreDestroy;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.ejb3.annotation.Clustered;
import org.jboss.logging.Logger;

/**
 * @author: Jaikiran Pai
 */
@Stateless
@Clustered
@Remote(NodeNameRetriever.class)
public class SlowUndeployingClusteredSLSB implements NodeNameRetriever {

    private final Logger logger = Logger.getLogger(SlowUndeployingClusteredSLSB.class);

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
