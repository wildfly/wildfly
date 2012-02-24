/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.cmp.jdbc2.schema;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;
import javax.transaction.Transaction;
import org.jboss.as.cmp.CmpMessages;
import org.jboss.as.cmp.jdbc.JDBCUtil;
import org.jboss.as.cmp.jdbc.SQLUtil;
import org.jboss.as.cmp.jdbc.metadata.JDBCRelationMetaData;
import org.jboss.as.cmp.jdbc2.bridge.JDBCCMPFieldBridge2;
import org.jboss.as.cmp.jdbc2.bridge.JDBCCMRFieldBridge2;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @version <tt>$Revision: 81030 $</tt>
 */
public class RelationTable
        implements Table {
    private static final byte CREATED = 1;
    private static final byte DELETED = 2;

    private final Schema schema;
    private final int tableId;
    private final DataSource ds;
    private final String tableName;
    private final JDBCCMRFieldBridge2 leftField;
    private final JDBCCMRFieldBridge2 rightField;
    private final Logger log;

    private String insertSql;
    private String deleteSql;

    public RelationTable(JDBCCMRFieldBridge2 leftField, JDBCCMRFieldBridge2 rightField, Schema schema, int tableId) {
        this.schema = schema;
        this.tableId = tableId;
        this.leftField = leftField;
        this.rightField = rightField;

        JDBCRelationMetaData metadata = leftField.getMetaData().getRelationMetaData();
        ds = leftField.getManager().getDataSource(metadata.getDataSourceName());
        tableName = SQLUtil.fixTableName(metadata.getDefaultTableName(), ds);

        log = Logger.getLogger(getClass().getName() + "." + tableName);

        // generate sql

        insertSql = "insert into " + tableName + " (";

        JDBCCMPFieldBridge2[] keyFields = (JDBCCMPFieldBridge2[]) this.leftField.getTableKeyFields();
        insertSql += keyFields[0].getColumnName();
        for (int i = 1; i < keyFields.length; ++i) {
            insertSql += ", " + keyFields[i].getColumnName();
        }

        keyFields = (JDBCCMPFieldBridge2[]) this.rightField.getTableKeyFields();
        insertSql += ", " + keyFields[0].getColumnName();
        for (int i = 1; i < keyFields.length; ++i) {
            insertSql += ", " + keyFields[i].getColumnName();
        }

        insertSql += ") values (?";
        for (int i = 1; i < this.leftField.getTableKeyFields().length + this.rightField.getTableKeyFields().length; ++i) {
            insertSql += ", ?";
        }

        insertSql += ")";

        log.debug("insert sql: " + insertSql);

        deleteSql = "delete from " + tableName + " where ";
        keyFields = (JDBCCMPFieldBridge2[]) this.leftField.getTableKeyFields();
        deleteSql += keyFields[0].getColumnName() + "=?";
        for (int i = 1; i < keyFields.length; ++i) {
            deleteSql += " and " + keyFields[i].getColumnName() + "=?";
        }

        keyFields = (JDBCCMPFieldBridge2[]) this.rightField.getTableKeyFields();
        deleteSql += " and " + keyFields[0].getColumnName() + "=?";
        for (int i = 1; i < keyFields.length; ++i) {
            deleteSql += " and " + keyFields[i].getColumnName() + "=?";
        }

        log.debug("delete sql: " + deleteSql);
    }

    // Public

    public void addRelation(JDBCCMRFieldBridge2 field1, Object key1, JDBCCMRFieldBridge2 field2, Object key2) {
        View view = getView();
        if (field1 == leftField) {
            view.addKeys(key1, key2);
        } else {
            view.addKeys(key2, key1);
        }
    }

    public void removeRelation(JDBCCMRFieldBridge2 field1, Object key1, JDBCCMRFieldBridge2 field2, Object key2) {
        View view = getView();
        if (field1 == leftField) {
            view.removeKeys(key1, key2);
        } else {
            view.removeKeys(key2, key1);
        }
    }

    // Table implementation

    public int getTableId() {
        return tableId;
    }

    public String getTableName() {
        return tableName;
    }

    public Table.View createView(Transaction tx) {
        return new View();
    }

    // Private

    private void delete(View view) throws SQLException {
        if (view.deleted == null) {
            if (log.isTraceEnabled()) {
                log.trace("no rows to delete");
            }
            return;
        }

        Connection con = null;
        PreparedStatement ps = null;
        try {
            if (log.isDebugEnabled()) {
                log.debug("executing : " + deleteSql);
            }

            con = ds.getConnection();
            ps = con.prepareStatement(deleteSql);

            int batchCount = 0;
            while (view.deleted != null) {
                RelationKeys keys = view.deleted;

                int paramInd = 1;
                JDBCCMPFieldBridge2[] keyFields = (JDBCCMPFieldBridge2[]) leftField.getTableKeyFields();
                for (int pkInd = 0; pkInd < keyFields.length; ++pkInd) {
                    JDBCCMPFieldBridge2 pkField = keyFields[pkInd];
                    Object fieldValue = pkField.getPrimaryKeyValue(keys.leftKey);
                    paramInd = pkField.setArgumentParameters(ps, paramInd, fieldValue);
                }

                keyFields = (JDBCCMPFieldBridge2[]) rightField.getTableKeyFields();
                for (int pkInd = 0; pkInd < keyFields.length; ++pkInd) {
                    JDBCCMPFieldBridge2 pkField = keyFields[pkInd];
                    Object fieldValue = pkField.getPrimaryKeyValue(keys.rightKey);
                    paramInd = pkField.setArgumentParameters(ps, paramInd, fieldValue);
                }

                ps.addBatch();
                ++batchCount;

                keys.dereference();
            }

            ps.executeBatch();

            if (view.deleted != null) {
                throw CmpMessages.MESSAGES.stillRowsToDelete();
            }

            if (log.isTraceEnabled()) {
                log.trace("deleted rows: " + batchCount);
            }
        } catch (SQLException e) {
            throw CmpMessages.MESSAGES.failedToDeleteView(e);
        } finally {
            JDBCUtil.safeClose(ps);
            JDBCUtil.safeClose(con);
        }
    }

    private void insert(View view) throws SQLException {
        if (view.created == null) {
            if (log.isTraceEnabled()) {
                log.trace("no rows to insert");
            }
            return;
        }

        Connection con = null;
        PreparedStatement ps = null;
        try {
            if (log.isDebugEnabled()) {
                log.debug("executing : " + insertSql);
            }

            con = ds.getConnection();
            ps = con.prepareStatement(insertSql);

            int batchCount = 0;
            while (view.created != null) {
                RelationKeys keys = view.created;

                JDBCCMPFieldBridge2[] keyFields = (JDBCCMPFieldBridge2[]) leftField.getTableKeyFields();
                int paramInd = 1;
                for (int fInd = 0; fInd < keyFields.length; ++fInd) {
                    JDBCCMPFieldBridge2 field = keyFields[fInd];
                    Object fieldValue = field.getPrimaryKeyValue(keys.leftKey);
                    paramInd = field.setArgumentParameters(ps, paramInd, fieldValue);
                }

                keyFields = (JDBCCMPFieldBridge2[]) rightField.getTableKeyFields();
                for (int fInd = 0; fInd < keyFields.length; ++fInd) {
                    JDBCCMPFieldBridge2 field = keyFields[fInd];
                    Object fieldValue = field.getPrimaryKeyValue(keys.rightKey);
                    paramInd = field.setArgumentParameters(ps, paramInd, fieldValue);
                }

                ps.addBatch();
                ++batchCount;

                keys.dereference();
            }

            ps.executeBatch();

            if (log.isTraceEnabled()) {
                log.trace("inserted rows: " + batchCount);
            }
        } catch (SQLException e) {
            throw CmpMessages.MESSAGES.failedToInsertNewRows(e);
        } finally {
            JDBCUtil.safeClose(ps);
            JDBCUtil.safeClose(con);
        }
    }

    private View getView() {
        return (View) schema.getView(this);
    }

    // Inner

    private class View implements Table.View {
        private RelationKeys created;
        private RelationKeys deleted;

        // Public

        public void addKeys(Object leftKey, Object rightKey) {
            // if it was deleted then dereference
            RelationKeys keys = deleted;
            while (keys != null) {
                if (keys.equals(leftKey, rightKey)) {
                    keys.dereference();
                    return;
                }
                keys = keys.next;
            }

            // add to created
            keys = new RelationKeys(this, leftKey, rightKey);

            if (created != null) {
                keys.next = created;
                created.prev = keys;
            }
            created = keys;
            keys.state = CREATED;
        }

        public void removeKeys(Object leftKey, Object rightKey) {
            // if it was created then dereference
            RelationKeys keys = created;
            while (keys != null) {
                if (keys.equals(leftKey, rightKey)) {
                    keys.dereference();
                    return;
                }
                keys = keys.next;
            }

            // add to deleted
            keys = new RelationKeys(this, leftKey, rightKey);

            if (deleted != null) {
                keys.next = deleted;
                deleted.prev = keys;
            }
            deleted = keys;
            keys.state = DELETED;
        }

        // Table.View implementation

        public void flushDeleted(Schema.Views views) throws SQLException {
            delete(this);
        }

        public void flushCreated(Schema.Views views) throws SQLException {
            insert(this);
        }

        public void flushUpdated() throws SQLException {
        }

        public void beforeCompletion() {
        }

        public void committed() {
        }

        public void rolledback() {
        }
    }

    private class RelationKeys {
        private final View view;
        private final Object leftKey;
        private final Object rightKey;

        private byte state;
        private RelationKeys next;
        private RelationKeys prev;

        public RelationKeys(View view, Object leftKey, Object rightKey) {
            this.view = view;
            this.leftKey = leftKey;
            this.rightKey = rightKey;
        }

        // Public

        public boolean equals(Object leftKey, Object rightKey) {
            return this.leftKey.equals(leftKey) && this.rightKey.equals(rightKey);
        }

        public void dereference() {
            if (state == CREATED && this == view.created) {
                view.created = next;
            } else if (state == DELETED && this == view.deleted) {
                view.deleted = next;
            }

            if (next != null) {
                next.prev = prev;
            }

            if (prev != null) {
                prev.next = next;
            }

            next = null;
            prev = null;
        }
    }
}
