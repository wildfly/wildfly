/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 *
 */
package org.jboss.as.txn.service;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.internal.arjuna.objectstore.hornetq.HornetqJournalEnvironmentBean;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;

import org.jboss.as.controller.services.path.PathManager;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Configures the {@link ObjectStoreEnvironmentBean}s using an injected path.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class ArjunaObjectStoreEnvironmentService implements Service {
    private final Consumer<Class<Void>> serviceConsumer;
    private final Supplier<PathManager> pathManagerSupplier;
    private final boolean useJournalStore;
    private final boolean enableAsyncIO;
    private final String path;
    private final String pathRef;
    private final boolean useJdbcStore;
    private final String dataSourceJndiName;
    private final JdbcStoreConfig jdbcSoreConfig;
    private volatile PathManager.Callback.Handle callbackHandle;

    public ArjunaObjectStoreEnvironmentService(final Consumer<Class<Void>> serviceConsumer, final Supplier<PathManager> pathManagerSupplier, final boolean useJournalStore, final boolean enableAsyncIO, final String path, final String pathRef, final boolean useJdbcStore, final String dataSourceJndiName, final JdbcStoreConfig jdbcSoreConfig) {
        this.serviceConsumer = serviceConsumer;
        this.pathManagerSupplier = pathManagerSupplier;
        this.useJournalStore = useJournalStore;
        this.enableAsyncIO = enableAsyncIO;
        this.path = path;
        this.pathRef = pathRef;
        this.useJdbcStore = useJdbcStore;
        this.dataSourceJndiName = dataSourceJndiName;
        this.jdbcSoreConfig = jdbcSoreConfig;
    }

    @Override
    public void start(final StartContext context) throws StartException {
        callbackHandle = pathManagerSupplier.get().registerCallback(pathRef, PathManager.ReloadServerCallback.create(), PathManager.Event.UPDATED, PathManager.Event.REMOVED);
        String objectStoreDir = pathManagerSupplier.get().resolveRelativePathEntry(path, pathRef);

         final ObjectStoreEnvironmentBean defaultActionStoreObjectStoreEnvironmentBean =
           BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, null);


        if(useJournalStore) {
            HornetqJournalEnvironmentBean hornetqJournalEnvironmentBean = BeanPopulator.getDefaultInstance(
                    HornetqJournalEnvironmentBean.class
            );
            hornetqJournalEnvironmentBean.setAsyncIO(enableAsyncIO);
            hornetqJournalEnvironmentBean.setStoreDir(objectStoreDir+"/HornetqObjectStore");
            defaultActionStoreObjectStoreEnvironmentBean.setObjectStoreType(
                    "com.arjuna.ats.internal.arjuna.objectstore.hornetq.HornetqObjectStoreAdaptor"
            );
        } else {
            defaultActionStoreObjectStoreEnvironmentBean.setObjectStoreDir(objectStoreDir);
        }

        final ObjectStoreEnvironmentBean stateStoreObjectStoreEnvironmentBean =
            BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, "stateStore");
        stateStoreObjectStoreEnvironmentBean.setObjectStoreDir(objectStoreDir);
        final ObjectStoreEnvironmentBean communicationStoreObjectStoreEnvironmentBean =
            BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, "communicationStore");
        communicationStoreObjectStoreEnvironmentBean.setObjectStoreDir(objectStoreDir);

        if (useJdbcStore) {
            defaultActionStoreObjectStoreEnvironmentBean.setObjectStoreType("com.arjuna.ats.internal.arjuna.objectstore.jdbc.JDBCStore");
            stateStoreObjectStoreEnvironmentBean.setObjectStoreType("com.arjuna.ats.internal.arjuna.objectstore.jdbc.JDBCStore");
            communicationStoreObjectStoreEnvironmentBean.setObjectStoreType("com.arjuna.ats.internal.arjuna.objectstore.jdbc.JDBCStore");

            defaultActionStoreObjectStoreEnvironmentBean.setJdbcAccess("com.arjuna.ats.internal.arjuna.objectstore.jdbc.accessors.DataSourceJDBCAccess;datasourceName=" + dataSourceJndiName);
            stateStoreObjectStoreEnvironmentBean.setJdbcAccess("com.arjuna.ats.internal.arjuna.objectstore.jdbc.accessors.DataSourceJDBCAccess;datasourceName=" + dataSourceJndiName);
            communicationStoreObjectStoreEnvironmentBean.setJdbcAccess("com.arjuna.ats.internal.arjuna.objectstore.jdbc.accessors.DataSourceJDBCAccess;datasourceName=" + dataSourceJndiName);

            defaultActionStoreObjectStoreEnvironmentBean.setTablePrefix(jdbcSoreConfig.getActionTablePrefix());
            stateStoreObjectStoreEnvironmentBean.setTablePrefix(jdbcSoreConfig.getStateTablePrefix());
            communicationStoreObjectStoreEnvironmentBean.setTablePrefix(jdbcSoreConfig.getCommunicationTablePrefix());

            defaultActionStoreObjectStoreEnvironmentBean.setDropTable(jdbcSoreConfig.isActionDropTable());
            stateStoreObjectStoreEnvironmentBean.setDropTable(jdbcSoreConfig.isStateDropTable());
            communicationStoreObjectStoreEnvironmentBean.setDropTable(jdbcSoreConfig.isCommunicationDropTable());
        }
        serviceConsumer.accept(Void.class);
    }

    @Override
    public void stop(final StopContext context) {
        callbackHandle.remove();
    }

    public static final class JdbcStoreConfig {
        private final String actionTablePrefix;
        private final boolean actionDropTable;
        private final String stateTablePrefix;
        private final boolean stateDropTable;
        private final String communicationTablePrefix;
        private final boolean communicationDropTable;

        private JdbcStoreConfig(final String actionTablePrefix, final boolean actionDropTable, final String stateTablePrefix, final boolean stateDropTable, final String communicationTablePrefix, final boolean communicationDropTable) {
            this.actionTablePrefix = actionTablePrefix;
            this.actionDropTable = actionDropTable;
            this.stateTablePrefix = stateTablePrefix;
            this.stateDropTable = stateDropTable;
            this.communicationTablePrefix = communicationTablePrefix;
            this.communicationDropTable = communicationDropTable;
        }

        public String getActionTablePrefix() {
            return actionTablePrefix;
        }

        public boolean isActionDropTable() {
            return actionDropTable;
        }

        public String getStateTablePrefix() {
            return stateTablePrefix;
        }

        public boolean isStateDropTable() {
            return stateDropTable;
        }

        public String getCommunicationTablePrefix() {
            return communicationTablePrefix;
        }

        public boolean isCommunicationDropTable() {
            return communicationDropTable;
        }
    }

    public static final class JdbcStoreConfigBulder {
            private String actionTablePrefix;
            private boolean actionDropTable;
            private String stateTablePrefix;
            private boolean stateDropTable;
            private String communicationTablePrefix;
            private boolean communicationDropTable;

        public JdbcStoreConfigBulder setActionTablePrefix(String actionTablePrefix) {
            this.actionTablePrefix = actionTablePrefix;
            return this;
        }

        public JdbcStoreConfigBulder setActionDropTable(boolean actionDropTable) {
            this.actionDropTable = actionDropTable;
            return this;
        }

        public JdbcStoreConfigBulder setStateTablePrefix(String stateTablePrefix) {
            this.stateTablePrefix = stateTablePrefix;
            return this;
        }

        public JdbcStoreConfigBulder setStateDropTable(boolean stateDropTable) {
            this.stateDropTable = stateDropTable;
            return this;
        }

        public JdbcStoreConfigBulder setCommunicationTablePrefix(String communicationTablePrefix) {
            this.communicationTablePrefix = communicationTablePrefix;
            return this;
        }

        public JdbcStoreConfigBulder setCommunicationDropTable(boolean communicationDropTable) {
            this.communicationDropTable = communicationDropTable;
            return this;
        }

        public JdbcStoreConfig build() {
            return new JdbcStoreConfig(actionTablePrefix, actionDropTable, stateTablePrefix, stateDropTable, communicationTablePrefix, communicationDropTable);
        }
    }
}
