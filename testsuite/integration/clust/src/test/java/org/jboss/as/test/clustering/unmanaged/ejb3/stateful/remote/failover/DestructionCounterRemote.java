package org.jboss.as.test.clustering.unmanaged.ejb3.stateful.remote.failover;

/**
 * @author Stuart Douglas
 */
public interface DestructionCounterRemote {

    void incrementCDIDestructionCount();

    void incrementSFSBDestructionCount();

    int getCDIDestructionCount();

    int getSFSBDestructionCount();

}
