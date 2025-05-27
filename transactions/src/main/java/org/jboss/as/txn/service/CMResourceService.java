/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.service;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.arjuna.ats.jta.common.JTAEnvironmentBean;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Sets up the CMR related properties in {@link JTAEnvironmentBean}
 *
 * @author Stefano Maestri
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class CMResourceService implements Service {
    private final Consumer<Class<Void>> serviceConsumer;
    private final Supplier<JTAEnvironmentBean> jtaEnvironmentBeanSupplier;
    private final String jndiName;
    private final String tableName;
    private final Boolean immediateCleanup;
    private final Integer batchSize;

    public CMResourceService(final Consumer<Class<Void>> serviceConsumer, final Supplier<JTAEnvironmentBean> jtaEnvironmentBeanSupplier, final String jndiName, final String tableName, final Boolean immediateCleanup, final Integer batchSize) {
        this.serviceConsumer = serviceConsumer;
        this.jtaEnvironmentBeanSupplier = jtaEnvironmentBeanSupplier;
        this.jndiName = jndiName;
        this.tableName = tableName;
        this.immediateCleanup = immediateCleanup;
        this.batchSize = batchSize;
    }


    @Override
    public void start(final StartContext context) throws StartException {
        final JTAEnvironmentBean jtaBean = jtaEnvironmentBeanSupplier.get();
        synchronized (jtaBean) {
            List<String> connectableResourceJNDINames = jtaBean.getCommitMarkableResourceJNDINames();
            Map<String, String> connectableResourceTableNameMap = jtaBean.getCommitMarkableResourceTableNameMap();
            Map<String, Boolean> performImmediateCleanupOfConnectableResourceBranchesMap = jtaBean.getPerformImmediateCleanupOfCommitMarkableResourceBranchesMap();
            Map<String, Integer> connectableResourceRecordDeleteBatchSizeMap = jtaBean.getCommitMarkableResourceRecordDeleteBatchSizeMap();

            connectableResourceJNDINames.add(jndiName);
            connectableResourceTableNameMap.put(jndiName, tableName);
            performImmediateCleanupOfConnectableResourceBranchesMap.put(jndiName, immediateCleanup);
            connectableResourceRecordDeleteBatchSizeMap.put(jndiName, batchSize);

            jtaBean.setCommitMarkableResourceJNDINames(connectableResourceJNDINames);
            jtaBean.setCommitMarkableResourceTableNameMap(connectableResourceTableNameMap);
            jtaBean.setPerformImmediateCleanupOfCommitMarkableResourceBranchesMap(performImmediateCleanupOfConnectableResourceBranchesMap);
            jtaBean.setCommitMarkableResourceRecordDeleteBatchSizeMap(connectableResourceRecordDeleteBatchSizeMap);
        }
        serviceConsumer.accept(Void.class);
    }

    @Override
    public void stop(final StopContext context) {
        serviceConsumer.accept(null);
        final JTAEnvironmentBean jtaBean = jtaEnvironmentBeanSupplier.get();
        synchronized (jtaBean) {
            List<String> connectableResourceJNDINames = jtaBean.getCommitMarkableResourceJNDINames();
            Map<String, String> connectableResourceTableNameMap = jtaBean.getCommitMarkableResourceTableNameMap();
            Map<String, Boolean> performImmediateCleanupOfConnectableResourceBranchesMap = jtaBean.getPerformImmediateCleanupOfCommitMarkableResourceBranchesMap();
            Map<String, Integer> connectableResourceRecordDeleteBatchSizeMap = jtaBean.getCommitMarkableResourceRecordDeleteBatchSizeMap();

            connectableResourceJNDINames.remove(jndiName);
            connectableResourceTableNameMap.remove(jndiName);
            performImmediateCleanupOfConnectableResourceBranchesMap.remove(jndiName);
            connectableResourceRecordDeleteBatchSizeMap.remove(jndiName);

            jtaBean.setCommitMarkableResourceJNDINames(connectableResourceJNDINames);
            jtaBean.setCommitMarkableResourceTableNameMap(connectableResourceTableNameMap);
            jtaBean.setPerformImmediateCleanupOfCommitMarkableResourceBranchesMap(performImmediateCleanupOfConnectableResourceBranchesMap);
            jtaBean.setCommitMarkableResourceRecordDeleteBatchSizeMap(connectableResourceRecordDeleteBatchSizeMap);
        }
    }
}
