package org.jboss.as.txn;

import static org.junit.Assert.assertTrue;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.txn.service.internal.tsr.TransactionSynchronizationRegistryWrapper;
import org.junit.Test;

import com.arjuna.ats.jta.common.jtaPropertyManager;

public class TestWildFlyTSR {
    boolean innerSyncCalled = false;

    @Test
    public void test() throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException {
        jtaPropertyManager.getJTAEnvironmentBean().setTransactionManagerClassName("com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple");
        final TransactionSynchronizationRegistry tsr =
            new TransactionSynchronizationRegistryWrapper(new com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionSynchronizationRegistryImple());
        TransactionManager transactionManager = com.arjuna.ats.jta.TransactionManager.transactionManager();
        transactionManager.begin();
        tsr.registerInterposedSynchronization(new Synchronization() {
            @Override
            public void beforeCompletion() {
                tsr.registerInterposedSynchronization(new Synchronization() {

                    @Override
                    public void beforeCompletion() {
                        innerSyncCalled = true;
                    }

                    @Override
                    public void afterCompletion(int status) {
                    }
                });
            }

            @Override
            public void afterCompletion(int status) {
            }
        });

        transactionManager.commit();

        assertTrue(innerSyncCalled);
    }
}
