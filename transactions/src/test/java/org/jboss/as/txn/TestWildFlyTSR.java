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

import com.arjuna.ats.internal.jta.transaction.arjunacore.jca.XATerminatorImple;
import org.jboss.as.txn.service.internal.tsr.TransactionSynchronizationRegistryWrapper;
import org.jboss.tm.XAResourceRecovery;
import org.jboss.tm.XAResourceRecoveryRegistry;

import org.junit.Test;

import com.arjuna.ats.jta.common.jtaPropertyManager;
import org.wildfly.transaction.client.ContextTransactionManager;
import org.wildfly.transaction.client.LocalTransactionContext;
import org.wildfly.transaction.client.provider.jboss.JBossLocalTransactionProvider;

public class TestWildFlyTSR {
    boolean innerSyncCalled = false;

    @Test
    public void test() throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException {
        jtaPropertyManager.getJTAEnvironmentBean().setTransactionManagerClassName("com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple");
        final TransactionSynchronizationRegistry tsr =
            new TransactionSynchronizationRegistryWrapper();
        final JBossLocalTransactionProvider.Builder builder = JBossLocalTransactionProvider.builder();
        builder.setTransactionManager(com.arjuna.ats.jta.TransactionManager.transactionManager());
        builder.setExtendedJBossXATerminator(new XATerminatorImple());
        builder.setXAResourceRecoveryRegistry(new XAResourceRecoveryRegistry() {
            @Override
            public void addXAResourceRecovery(XAResourceRecovery xaResourceRecovery) {}

            @Override public void removeXAResourceRecovery(XAResourceRecovery xaResourceRecovery) {}
        });
        LocalTransactionContext.getContextManager().setGlobalDefault(new LocalTransactionContext(
            builder.build()
        ));
        TransactionManager transactionManager = ContextTransactionManager.getInstance();
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
