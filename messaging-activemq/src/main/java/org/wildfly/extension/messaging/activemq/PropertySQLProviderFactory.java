/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.messaging.activemq;

import static java.lang.String.format;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.HashSet;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.activemq.artemis.jdbc.store.sql.SQLProvider;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;

/**
 * Property-based implementation of a {@link SQLProvider}'s factory.
 *
 * Properties are stored in a journal-sql.properties in the org.wildfly.extension.messaging-activemq JBoss module.
 *
 * Dialects specific to a database can be customized by suffixing the property keys with the name of the database.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class PropertySQLProviderFactory implements SQLProvider.Factory {

    private static final String ORACLE = "oracle";

    String database;
    private volatile Properties sql;
    /** List of extracted known dialects*/
    private final HashSet<String> databaseDialects = new HashSet<>();

    public PropertySQLProviderFactory(String database) throws IOException {
        this.database = database;
        try (InputStream stream = PropertySQLProvider.class.getClassLoader().getResourceAsStream("journal-sql.properties")) {
            sql = new Properties();
            sql.load(stream);
            extractDialects();
        }
    }

    @Override
    public SQLProvider create(String tableName, SQLProvider.DatabaseStoreType storeType) {
        // WFLY-8307 - Oracle driver does not support lower case for table names
        String name = ORACLE.equals(database) ? tableName.toUpperCase() : tableName;
        return new PropertySQLProvider(name, storeType);
    }

    /**
     * Read the properties from the journal-sql and extract the database dialects.
     */
    private void extractDialects() {
        for (Object prop : sql.keySet()) {
            int dot = ((String)prop).indexOf('.');
            if (dot > 0) {
                databaseDialects.add(((String)prop).substring(dot+1));
            }
        }
    }

    public void investigateDialect(DataSource dataSource) {
        // specifying the database has precedence over detecting it from the data source metadata.
        if (database != null) {
            return;
        }
        // no database dialect from configuration, guessing from MetaData
        try  (Connection connection = dataSource.getConnection()){
            DatabaseMetaData metaData = connection.getMetaData();
            String dbProduct = metaData.getDatabaseProductName();
            database = identifyDialect(dbProduct);

            if (database == null) {
                MessagingLogger.ROOT_LOGGER.debug("Attempting to guess on driver name.");
                database = identifyDialect(metaData.getDriverName());
            }
            if (dataSource == null) {
                MessagingLogger.ROOT_LOGGER.jdbcDatabaseDialectDetectionFailed(databaseDialects.toString());
            } else {
                MessagingLogger.ROOT_LOGGER.debugf("Detect database dialect as '%s'.  If this is incorrect, please specify the correct dialect using the 'database' attribute in your configuration.  Supported database dialect strings are %s", database, databaseDialects);
            }
        } catch (Exception e) {
            MessagingLogger.ROOT_LOGGER.debug("Unable to read JDBC metadata.", e);
        }
    }

    private String identifyDialect(String name) {
        String unified = null;

        if (name != null) {
            if (name.toLowerCase().contains("postgres")) {
                unified = "postgresql";
            } else if (name.toLowerCase().contains("mysql")) {
                unified = "mysql";
            } else if (name.toLowerCase().contains("db2")) {
                unified = "db2";
            } else if (name.toLowerCase().contains("derby")) {
                unified = "derby";
            } else if (name.toLowerCase().contains("hsql") || name.toLowerCase().contains("hypersonic")) {
                unified = "hsql";
            } else if (name.toLowerCase().contains("h2")) {
                unified = "h2";
            } else if (name.toLowerCase().contains(ORACLE)) {
                unified = ORACLE;
            }else if (name.toLowerCase().contains("microsoft")) {
                unified = "mssql";
            }else if (name.toLowerCase().contains("jconnect")) {
                unified = "sybase";
            }
        }
        MessagingLogger.ROOT_LOGGER.debugf("Check dialect for '%s', result is '%s'", name, unified);
        return unified;
    }

    private class PropertySQLProvider implements SQLProvider {

        private final String tableName;
        private static final int STATE_ROW_ID = 0;
        private static final int LIVE_LOCK_ROW_ID = 1;
        private static final int BACKUP_LOCK_ROW_ID = 2;
        private static final int NODE_ID_ROW_ID = 3;


        public PropertySQLProvider(String tableName, DatabaseStoreType storeType) {
            this.tableName = tableName;
        }

        @Override
        public long getMaxBlobSize() {
            return Long.valueOf(sql("max-blob-size"));
        }

        @Override
        public String[] getCreateJournalTableSQL() {
            return new String[] {
                    format(sql("create-journal-table"), tableName),
                    format(sql("create-journal-index"), tableName),
            };
        }

        @Override
        public String getInsertJournalRecordsSQL() {
            return format(sql("insert-journal-record"), tableName);
        }

        @Override
        public String getSelectJournalRecordsSQL() {
            return format(sql("select-journal-record"), tableName);
        }

        @Override
        public String getDeleteJournalRecordsSQL() {
            return format(sql("delete-journal-record"), tableName);
        }

        @Override
        public String getDeleteJournalTxRecordsSQL() {
            return format(sql("delete-journal-tx-record"), tableName);
        }

        @Override
        public String getTableName() {
            return tableName;
        }

        @Override
        public String getCreateFileTableSQL() {
            return format(sql("create-file-table"), tableName);
        }

        @Override
        public String getInsertFileSQL() {
            return format(sql("insert-file"), tableName);
        }

        @Override
        public String getSelectFileNamesByExtensionSQL() {
            return format(sql("select-filenames-by-extension"), tableName);
        }

        @Override
        public String getSelectFileByFileName() {
            return format(sql("select-file-by-filename"), tableName);
        }

        @Override
        public String getAppendToLargeObjectSQL() {
            return format(sql("append-to-file"), tableName);
        }

        @Override
        public String getReadLargeObjectSQL() {
            return format(sql("read-large-object"), tableName);
        }

        @Override
        public String getDeleteFileSQL() {
            return format(sql("delete-file"), tableName);
        }

        @Override
        public String getUpdateFileNameByIdSQL() {
            return format(sql("update-filename-by-id"), tableName);
        }

        @Override
        public String getCopyFileRecordByIdSQL() {
            return format(sql("copy-file-record-by-id"), tableName);
        }

        @Override
        public String getDropFileTableSQL() {
            return format(sql("drop-table"), tableName);
        }

        @Override
        public String getCloneFileRecordByIdSQL() {
            return format(sql("clone-file-record"), tableName);
        }

        @Override
        public String getCountJournalRecordsSQL() {
            return format(sql("count-journal-record"), tableName);
        }

        @Override
        public boolean closeConnectionOnShutdown() {
            return Boolean.valueOf(sql("close-connection-on-shutdown"));
        }

        @Override
        public String createNodeManagerStoreTableSQL() {
            return format(sql("create-node-manager-store-table"), tableName);
        }

        @Override
        public String createStateSQL() {
            return format(sql("create-state"), tableName, STATE_ROW_ID);
        }

        @Override
        public String createNodeIdSQL() {
            return format(sql("create-state"), tableName, NODE_ID_ROW_ID);
        }

        @Override
        public String createLiveLockSQL() {
            return format(sql("create-state"), tableName, LIVE_LOCK_ROW_ID);
        }

        @Override
        public String createBackupLockSQL() {
            return format(sql("create-state"), tableName, BACKUP_LOCK_ROW_ID);
        }

        @Override
        public String tryAcquireLiveLockSQL() {
            return format(sql("try-acquire-lock"), tableName, LIVE_LOCK_ROW_ID);
        }

        @Override
        public String tryAcquireBackupLockSQL() {
            return format(sql("try-acquire-lock"), tableName, BACKUP_LOCK_ROW_ID);
        }

        @Override
        public String tryReleaseLiveLockSQL() {
            return format(sql("try-release-lock"), tableName, LIVE_LOCK_ROW_ID);
        }

        @Override
        public String tryReleaseBackupLockSQL() {
            return format(sql("try-release-lock"), tableName, BACKUP_LOCK_ROW_ID);
        }

        @Override
        public String isLiveLockedSQL() {
            return format(sql("is-locked"), tableName, LIVE_LOCK_ROW_ID);
        }

        @Override
        public String isBackupLockedSQL() {
            return format(sql("is-locked"), tableName, BACKUP_LOCK_ROW_ID);
        }

        @Override
        public String renewLiveLockSQL() {
            return format(sql("renew-lock"), tableName, LIVE_LOCK_ROW_ID);
        }

        @Override
        public String renewBackupLockSQL() {
            return format(sql("renew-lock"), tableName, BACKUP_LOCK_ROW_ID);
        }

        @Override
        public String currentTimestampSQL() {
            return format(sql("current-timestamp"), tableName);
        }

        @Override
        public String writeStateSQL() {
            return format(sql("write-state"), tableName, STATE_ROW_ID);
        }

        @Override
        public String readStateSQL() {
            return format(sql("read-state"), tableName, STATE_ROW_ID);
        }

        @Override
        public String writeNodeIdSQL() {
            return format(sql("write-nodeId"), tableName, NODE_ID_ROW_ID);
        }

        @Override
        public String readNodeIdSQL() {
            return format(sql("read-nodeId"), tableName, NODE_ID_ROW_ID);
        }

        @Override
        public String initializeNodeIdSQL() {
            return format(sql("initialize-nodeId"), tableName, NODE_ID_ROW_ID);
        }

        private String sql(final String key) {
            if (database != null) {
                String result = sql.getProperty(key + "." + database);
                if (result != null) {
                    return result;
                }
            }
            return sql.getProperty(key);
        }
    }
}
