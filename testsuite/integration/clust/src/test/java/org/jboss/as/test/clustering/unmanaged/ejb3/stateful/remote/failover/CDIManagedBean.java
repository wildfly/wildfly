package org.jboss.as.test.clustering.unmanaged.ejb3.stateful.remote.failover;

import javax.annotation.PreDestroy;
import javax.ejb.EJB;

/**
 *
 * CDI bean that is injected at construction time, and not referenced
 *
 * @author Stuart Douglas
 */
public class CDIManagedBean {

    @EJB
    private DestructionCounterRemote counter;

    @PreDestroy
    public void destroy() {
        counter.incrementCDIDestructionCount();
    }

}
