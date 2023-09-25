/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.service;

import com.arjuna.ats.internal.jta.recovery.arjunacore.JTANodeNameXAResourceOrphanFilter;
import com.arjuna.ats.internal.jta.recovery.arjunacore.JTATransactionLogXAResourceOrphanFilter;
import com.arjuna.ats.internal.jta.recovery.arjunacore.JTAActionStatusServiceXAResourceOrphanFilter;
import com.arjuna.ats.internal.jta.recovery.arjunacore.SubordinateJTAXAResourceOrphanFilter;
import com.arjuna.ats.internal.jta.recovery.arjunacore.SubordinationManagerXAResourceOrphanFilter;
import com.arjuna.ats.jta.common.JTAEnvironmentBean;
import com.arjuna.ats.jta.common.jtaPropertyManager;

import org.jboss.as.txn.integration.LocalUserTransactionOperationsProvider;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.tm.LastResource;

import java.util.Arrays;
import java.util.Collections;

/**
 * Sets up the {@link JTAEnvironmentBean}
 *
 * @author Jaikiran Pai
 */
public class JTAEnvironmentBeanService implements Service<JTAEnvironmentBean> {

    private final String nodeIdentifier;
    private boolean useActionStatusServiceRecoveryFilter;

    public JTAEnvironmentBeanService(final String nodeIdentifier) {
        this.nodeIdentifier = nodeIdentifier;
        this.useActionStatusServiceRecoveryFilter = Boolean.valueOf(System.getProperty("org.jboss.narayana.wildfly.useActionStatusServiceRecoveryFilter.deprecated", "true"));
    }

    @Override
    public void start(StartContext context) throws StartException {
        final JTAEnvironmentBean jtaEnvironmentBean = jtaPropertyManager.getJTAEnvironmentBean();
        jtaEnvironmentBean.setLastResourceOptimisationInterfaceClassName(LastResource.class.getName());
        // recovery nodes
        jtaEnvironmentBean.setXaRecoveryNodes(Collections.singletonList(nodeIdentifier));
        // setup the XA orphan filters
        if (useActionStatusServiceRecoveryFilter) {
            jtaEnvironmentBean.setXaResourceOrphanFilterClassNames(Arrays.asList(JTATransactionLogXAResourceOrphanFilter.class.getName(), JTANodeNameXAResourceOrphanFilter.class.getName(), SubordinateJTAXAResourceOrphanFilter.class.getName(), SubordinationManagerXAResourceOrphanFilter.class.getName(), JTAActionStatusServiceXAResourceOrphanFilter.class.getName()));
        } else {
            jtaEnvironmentBean.setXaResourceOrphanFilterClassNames(Arrays.asList(JTATransactionLogXAResourceOrphanFilter.class.getName(), JTANodeNameXAResourceOrphanFilter.class.getName(), SubordinateJTAXAResourceOrphanFilter.class.getName(), SubordinationManagerXAResourceOrphanFilter.class.getName()));
        }
        jtaEnvironmentBean.setXAResourceRecordWrappingPlugin(new com.arjuna.ats.internal.jbossatx.jta.XAResourceRecordWrappingPluginImpl());
        jtaEnvironmentBean.setTransactionManagerJNDIContext("java:jboss/TransactionManager");
        jtaEnvironmentBean.setTransactionSynchronizationRegistryJNDIContext("java:jboss/TransactionSynchronizationRegistry");
        jtaEnvironmentBean.setUserTransactionOperationsProviderClassName(LocalUserTransactionOperationsProvider.class.getName());
    }

    @Override
    public void stop(StopContext context) {
        final JTAEnvironmentBean jtaEnvironmentBean = jtaPropertyManager.getJTAEnvironmentBean();
        // reset the XA orphan filters
        jtaEnvironmentBean.setXaResourceOrphanFilterClassNames(null);
        // reset the recovery nodes
        jtaEnvironmentBean.setXaRecoveryNodes(null);
        // reset the record wrapper plugin
        jtaEnvironmentBean.setXAResourceRecordWrappingPlugin(null);
        jtaEnvironmentBean.setLastResourceOptimisationInterfaceClassName(null);

    }

    @Override
    public JTAEnvironmentBean getValue() throws IllegalStateException, IllegalArgumentException {
        return jtaPropertyManager.getJTAEnvironmentBean();
    }
}
