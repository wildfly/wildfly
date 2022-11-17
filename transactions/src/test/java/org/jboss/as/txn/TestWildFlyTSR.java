package org.jboss.as.txn;

import static org.junit.Assert.assertTrue;

import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;

import com.arjuna.ats.arjuna.common.arjPropertyManager;
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
        arjPropertyManager.getObjectStoreEnvironmentBean().setObjectStoreDir(System.getProperty("ObjectStoreEnvironmentBean.objectStoreDir"));
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
