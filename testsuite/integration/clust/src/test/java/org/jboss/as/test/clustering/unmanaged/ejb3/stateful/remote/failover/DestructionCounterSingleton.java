package org.jboss.as.test.clustering.unmanaged.ejb3.stateful.remote.failover;

import javax.ejb.Remote;
import javax.ejb.Singleton;

/**
 * Singleton that tracks bean destructions
 *
 * @author Stuart Douglas
 */
@Singleton
@Remote(DestructionCounterRemote.class)
public class DestructionCounterSingleton implements DestructionCounterRemote {
    private int cdi;
    private int sfsb;

    @Override
    public void incrementCDIDestructionCount() {
        cdi++;
    }

    @Override
    public void incrementSFSBDestructionCount() {
        sfsb++;
    }

    @Override
    public int getCDIDestructionCount() {
        return cdi;
    }

    @Override
    public int getSFSBDestructionCount() {
        return sfsb;
    }
}
