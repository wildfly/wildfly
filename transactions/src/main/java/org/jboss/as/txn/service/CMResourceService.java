/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.txn.service;

import java.util.List;
import java.util.Map;

import com.arjuna.ats.jta.common.JTAEnvironmentBean;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Sets up the CMR related properties in {@link com.arjuna.ats.jta.common.JTAEnvironmentBean}
 *
 * @author Stefano Maestri
 */
public class CMResourceService implements Service<Void> {

    private final String jndiName;
    private final String tableName;
    private final Boolean immediateCleanup;
    private final Integer batchSize;

    private final InjectedValue<JTAEnvironmentBean> jtaEnvironmentBean = new InjectedValue<>();


    public CMResourceService(String jndiName, String tableName, Boolean immediateCleanup, Integer batchSize) {
        this.jndiName = jndiName;
        this.tableName = tableName;
        this.immediateCleanup = immediateCleanup;
        this.batchSize = batchSize;
    }


    @Override
    public void start(StartContext context) throws StartException {
        final JTAEnvironmentBean jtaBean = jtaEnvironmentBean.getValue();
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
    }

    @Override
    public void stop(StopContext context) {
        final JTAEnvironmentBean jtaBean = jtaEnvironmentBean.getValue();
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

    @Override
    public Void getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }

    public Injector<JTAEnvironmentBean> getJTAEnvironmentBeanInjector() {
            return this.jtaEnvironmentBean;
        }
}
