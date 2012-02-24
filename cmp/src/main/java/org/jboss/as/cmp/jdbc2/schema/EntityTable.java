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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.DuplicateKeyException;
import javax.ejb.EJBException;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.transaction.Transaction;
import org.jboss.as.cmp.CmpConfig;
import org.jboss.as.cmp.CmpMessages;
import org.jboss.as.cmp.jdbc.JDBCEntityPersistenceStore;
import org.jboss.as.cmp.jdbc.JDBCTypeFactory;
import org.jboss.as.cmp.jdbc.JDBCUtil;
import org.jboss.as.cmp.jdbc.SQLUtil;
import org.jboss.as.cmp.jdbc.bridge.JDBCAbstractCMRFieldBridge;
import org.jboss.as.cmp.jdbc.metadata.JDBCEntityMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCFunctionMappingMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCTypeMappingMetaData;
import org.jboss.as.cmp.jdbc2.bridge.JDBCCMPFieldBridge2;
import org.jboss.as.cmp.jdbc2.bridge.JDBCEntityBridge2;
import org.jboss.logging.Logger;
import org.w3c.dom.Element;


/**
 * todo refactor optimistic locking
 *
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @author <a href="mailto:galder.zamarreno@jboss.com">Galder Zamarreno</a>
 * @version <tt>$Revision: 86009 $</tt>
 */
public class EntityTable implements Table {
    private static final byte UNREFERENCED = 0;
    private static final byte CLEAN = 1;
    private static final byte DIRTY = 2;
    private static final byte CREATED = 4;
    private static final byte DELETED = 8;
    private static final byte DIRTY_RELATIONS = 16;

    private static final Object NOT_LOADED = new Object();

    private JDBCEntityBridge2 entity;
    private String tableName;
    private int fieldsTotal;
    private int relationsTotal;
    private DataSource dataSource;
    private Schema schema;
    private int tableId;
    private boolean dontFlushCreated;

    private String deleteSql;
    private String updateSql;
    private String insertSql;
    private String selectSql;
    private String duplicatePkSql;

    private final CommitStrategy insertStrategy;
    private final CommitStrategy deleteStrategy;
    private final CommitStrategy updateStrategy;

    private Logger log;

    private Cache cache;
    private ObjectName cacheName;

    private int[] references;
    private int[] referencedBy;

    private ForeignKeyConstraint[] fkConstraints;

    public EntityTable(JDBCEntityMetaData metadata, JDBCEntityBridge2 entity, Schema schema, int tableId) {
        try {
            InitialContext ic = new InitialContext();
            dataSource = (DataSource) ic.lookup(metadata.getDataSourceName());
        } catch (NamingException e) {
            throw CmpMessages.MESSAGES.failedToLookupDatasource(metadata.getDataSourceName(), e);
        }

        this.entity = entity;
        tableName = SQLUtil.fixTableName(metadata.getDefaultTableName(), dataSource);
        log = Logger.getLogger(getClass().getName() + "." + tableName);

        this.schema = schema;
        this.tableId = tableId;

        final CmpConfig containerConf = entity.getManager().getCmpConfig();
        dontFlushCreated = containerConf.isInsertAfterEjbPostCreate();


        // TODO: jeb - make cache configurable
        int minCapacity;
        int maxCapacity;
        minCapacity = 1000;
        maxCapacity = 10000;
        int partitionsTotal;
        final boolean invalidable;
        final Element batchCommitStrategy;

        partitionsTotal = 10;
        batchCommitStrategy = null;
        invalidable = false;

        cache = Cache.NONE;

        if (batchCommitStrategy == null) {
            insertStrategy = NON_BATCH_UPDATE;
            deleteStrategy = NON_BATCH_UPDATE;
            updateStrategy = NON_BATCH_UPDATE;
        } else {
            log.debug("batch-commit-strategy enabled");
            insertStrategy = BATCH_UPDATE;
            deleteStrategy = BATCH_UPDATE;
            updateStrategy = BATCH_UPDATE;
        }

        start();
    }

    public void start() {
        final JDBCAbstractCMRFieldBridge[] cmrFields = entity.getCMRFields();
        relationsTotal = (cmrFields != null ? cmrFields.length : 0);

        JDBCCMPFieldBridge2[] pkFields = (JDBCCMPFieldBridge2[]) entity.getPrimaryKeyFields();
        JDBCCMPFieldBridge2[] tableFields = (JDBCCMPFieldBridge2[]) entity.getTableFields();

        // DELETE SQL
        deleteSql = "delete from " + tableName + " where ";
        deleteSql += pkFields[0].getColumnName() + "=?";
        for (int i = 1; i < pkFields.length; ++i) {
            deleteSql += " and " + pkFields[i].getColumnName() + "=?";
        }
        log.debug("delete sql: " + deleteSql);

        // INSERT SQL
        insertSql = "insert into " + tableName + "(";
        insertSql += tableFields[0].getColumnName();
        for (int i = 1; i < tableFields.length; ++i) {
            insertSql += ", " + tableFields[i].getColumnName();
        }
        insertSql += ") values (?";
        for (int i = 1; i < tableFields.length; ++i) {
            insertSql += ", ?";
        }
        insertSql += ")";
        log.debug("insert sql: " + insertSql);

        // UPDATE SQL
        updateSql = "update " + tableName + " set ";
        int setFields = 0;
        for (int i = 0; i < tableFields.length; ++i) {
            JDBCCMPFieldBridge2 field = tableFields[i];
            if (!field.isPrimaryKeyMember()) {
                if (setFields++ > 0) {
                    updateSql += ", ";
                }
                updateSql += field.getColumnName() + "=?";
            }
        }
        updateSql += " where ";
        updateSql += pkFields[0].getColumnName() + "=?";
        for (int i = 1; i < pkFields.length; ++i) {
            updateSql += " and " + pkFields[i].getColumnName() + "=?";
        }

        if (entity.getVersionField() != null) {
            updateSql += " and " + entity.getVersionField().getColumnName() + "=?";
        }
        log.debug("update sql: " + updateSql);

        // SELECT SQL
        String selectColumns = tableFields[0].getColumnName();
        for (int i = 1; i < tableFields.length; ++i) {
            JDBCCMPFieldBridge2 field = tableFields[i];
            selectColumns += ", " + field.getColumnName();
        }

        String whereColumns = pkFields[0].getColumnName() + "=?";
        for (int i = 1; i < pkFields.length; ++i) {
            whereColumns += " and " + pkFields[i].getColumnName() + "=?";
        }

        if (entity.getMetaData().hasRowLocking()) {
            JDBCEntityPersistenceStore manager = entity.getManager();
            JDBCTypeFactory typeFactory = manager.getJDBCTypeFactory();
            JDBCTypeMappingMetaData typeMapping = typeFactory.getTypeMapping();
            JDBCFunctionMappingMetaData rowLockingTemplate = typeMapping.getRowLockingTemplate();
            if (rowLockingTemplate == null) {
                throw CmpMessages.MESSAGES.noRowLockingTemplateForMapping(typeMapping.getName());
            }

            selectSql = rowLockingTemplate.getFunctionSql(new Object[]{selectColumns, tableName, whereColumns, null},
                    new StringBuffer()).toString();
        } else {
            selectSql = "select ";
            selectSql += selectColumns;
            selectSql += " from " + tableName + " where ";
            selectSql += whereColumns;
        }
        log.debug("select sql: " + selectSql);

        // DUPLICATE KEY
        if (dontFlushCreated) {
            duplicatePkSql = "select ";
            duplicatePkSql += pkFields[0].getColumnName();
            for (int i = 1; i < pkFields.length; ++i) {
                duplicatePkSql += ", " + pkFields[i].getColumnName();
            }
            duplicatePkSql += " from " + tableName + " where ";
            duplicatePkSql += pkFields[0].getColumnName() + "=?";
            for (int i = 1; i < pkFields.length; ++i) {
                duplicatePkSql += " and " + pkFields[i].getColumnName() + "=?";
            }
            log.debug("duplicate pk sql: " + duplicatePkSql);
        }
    }

    public StringBuffer appendColumnNames(JDBCCMPFieldBridge2[] fields, String alias, StringBuffer buf) {
        for (int i = 0; i < fields.length; ++i) {
            if (i > 0) {
                buf.append(", ");
            }

            if (alias != null) {
                buf.append(alias).append(".");
            }

            buf.append(fields[i].getColumnName());
        }

        return buf;
    }

    public void addField() {
        ++fieldsTotal;
    }

    public int addVersionField() {
        return fieldsTotal++;
    }

    public ForeignKeyConstraint addFkConstraint(JDBCCMPFieldBridge2[] fkFields, EntityTable referenced) {
        addReference(referenced);
        referenced.addReferencedBy(this);

        if (fkConstraints == null) {
            fkConstraints = new ForeignKeyConstraint[1];
        } else {
            ForeignKeyConstraint[] tmp = fkConstraints;
            fkConstraints = new ForeignKeyConstraint[tmp.length + 1];
            System.arraycopy(tmp, 0, fkConstraints, 0, tmp.length);
        }
        final int fkindex = fkConstraints.length - 1;
        final ForeignKeyConstraint fkc = new ForeignKeyConstraint(fkindex, fkFields, referenced.tableId == tableId);
        fkConstraints[fkindex] = fkc;
        return fkc;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public Object loadRow(ResultSet rs, boolean searchableOnly) {
        View view = getView();
        Object pk = view.loadPk(rs);
        if (pk != null) {
            view.loadRow(rs, pk, searchableOnly);
        } else if (log.isTraceEnabled()) {
            log.trace("loaded pk is null.");
        }
        return pk;
    }

    public Row getRow(Object id) {
        return getView().getRow(id);
    }

    public boolean hasRow(Object id) {
        return getView().hasRow(id);
    }

    public Row loadRow(Object id) throws SQLException {
        View view = getView();

        Row row = view.getRowByPk(id, false);
        if (row != null) {
            if (log.isTraceEnabled()) {
                log.trace("row is already loaded: pk=" + id);
            }
            return row;
        }

        JDBCCMPFieldBridge2[] pkFields = (JDBCCMPFieldBridge2[]) entity.getPrimaryKeyFields();

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            if (log.isDebugEnabled()) {
                log.debug("executing sql: " + selectSql);
            }

            con = dataSource.getConnection();
            ps = con.prepareStatement(selectSql);

            int paramInd = 1;
            for (int i = 0; i < pkFields.length; ++i) {
                JDBCCMPFieldBridge2 pkField = pkFields[i];
                Object pkValue = pkField.getPrimaryKeyValue(id);
                paramInd = pkField.setArgumentParameters(ps, paramInd, pkValue);
            }

            rs = ps.executeQuery();

            if (!rs.next()) {
                throw CmpMessages.MESSAGES.rowNotFound(id);
            }

            return view.loadRow(rs, id, false);
        } catch (SQLException e) {
            throw CmpMessages.MESSAGES.failedToLoadRow(tableName, id);
        } finally {
            JDBCUtil.safeClose(rs);
            JDBCUtil.safeClose(ps);
            JDBCUtil.safeClose(con);
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
        return new View(tx);
    }

    // Private

    private void addReference(EntityTable table) {
        boolean wasRegistered = false;
        if (references != null) {
            for (int i = 0; i < references.length; ++i) {
                if (references[i] == table.getTableId()) {
                    wasRegistered = true;
                    break;
                }
            }

            if (!wasRegistered) {
                int[] tmp = references;
                references = new int[references.length + 1];
                System.arraycopy(tmp, 0, references, 0, tmp.length);
                references[tmp.length] = table.getTableId();
            }
        } else {
            references = new int[1];
            references[0] = table.getTableId();
        }

        if (!wasRegistered) {
            if (log.isTraceEnabled()) {
                log.trace("references " + table.getTableName());
            }
        }
    }

    private void addReferencedBy(EntityTable table) {
        boolean wasRegistered = false;
        if (referencedBy != null) {
            for (int i = 0; i < referencedBy.length; ++i) {
                if (referencedBy[i] == table.getTableId()) {
                    wasRegistered = true;
                    break;
                }
            }

            if (!wasRegistered) {
                int[] tmp = referencedBy;
                referencedBy = new int[referencedBy.length + 1];
                System.arraycopy(tmp, 0, referencedBy, 0, tmp.length);
                referencedBy[tmp.length] = table.getTableId();
            }
        } else {
            referencedBy = new int[1];
            referencedBy[0] = table.getTableId();
        }

        if (!wasRegistered) {
            if (log.isTraceEnabled()) {
                log.trace("referenced by " + table.getTableName());
            }
        }
    }

    private void delete(View view) throws SQLException {
        JDBCCMPFieldBridge2[] pkFields = (JDBCCMPFieldBridge2[]) entity.getPrimaryKeyFields();

        Connection con = null;
        PreparedStatement ps = null;
        try {
            if (log.isDebugEnabled()) {
                log.debug("executing : " + deleteSql);
            }

            con = dataSource.getConnection();
            ps = con.prepareStatement(deleteSql);

            int batchCount = 0;
            while (view.deleted != null) {
                Row row = view.deleted;

                int paramInd = 1;
                for (int pkInd = 0; pkInd < pkFields.length; ++pkInd) {
                    JDBCCMPFieldBridge2 pkField = pkFields[pkInd];
                    Object fieldValue = row.fields[pkField.getRowIndex()];
                    paramInd = pkField.setArgumentParameters(ps, paramInd, fieldValue);
                }

                deleteStrategy.executeUpdate(ps);

                ++batchCount;
                row.flushStatus();
            }

            deleteStrategy.executeBatch(ps);

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

    private void update(View view) throws SQLException {
        JDBCCMPFieldBridge2[] tableFields = (JDBCCMPFieldBridge2[]) entity.getTableFields();
        JDBCCMPFieldBridge2[] pkFields = (JDBCCMPFieldBridge2[]) entity.getPrimaryKeyFields();

        Connection con = null;
        PreparedStatement ps = null;
        try {
            if (log.isDebugEnabled()) {
                log.debug("executing : " + updateSql);
            }

            con = dataSource.getConnection();
            ps = con.prepareStatement(updateSql);

            int batchCount = 0;
            while (view.dirty != null) {
                Row row = view.dirty;

                int paramInd = 1;
                for (int fInd = 0; fInd < tableFields.length; ++fInd) {
                    JDBCCMPFieldBridge2 field = tableFields[fInd];
                    if (!field.isPrimaryKeyMember()) {
                        Object fieldValue = row.fields[field.getRowIndex()];
                        paramInd = field.setArgumentParameters(ps, paramInd, fieldValue);
                    }
                }

                for (int fInd = 0; fInd < pkFields.length; ++fInd) {
                    JDBCCMPFieldBridge2 pkField = pkFields[fInd];
                    Object fieldValue = row.fields[pkField.getRowIndex()];
                    paramInd = pkField.setArgumentParameters(ps, paramInd, fieldValue);
                }

                JDBCCMPFieldBridge2 versionField = entity.getVersionField();
                if (versionField != null) {
                    int versionIndex = versionField.getVersionIndex();
                    Object curVersion = row.fields[versionIndex];
                    paramInd = versionField.setArgumentParameters(ps, paramInd, curVersion);

                    Object newVersion = row.fields[versionField.getRowIndex()];
                    row.fields[versionIndex] = newVersion;
                }

                updateStrategy.executeUpdate(ps);

                ++batchCount;
                row.flushStatus();
            }

            updateStrategy.executeBatch(ps);

            if (log.isTraceEnabled()) {
                log.trace("updated rows: " + batchCount);
            }
        } catch (SQLException e) {
            throw CmpMessages.MESSAGES.failedToUpdateTable(tableName, e);
        } finally {
            JDBCUtil.safeClose(ps);
            JDBCUtil.safeClose(con);
        }
    }

    private void insert(View view) throws SQLException {
        JDBCCMPFieldBridge2[] tableFields = (JDBCCMPFieldBridge2[]) entity.getTableFields();
        Connection con = null;
        PreparedStatement ps = null;
        try {
            if (log.isDebugEnabled()) {
                log.debug("executing : " + insertSql);
            }

            con = dataSource.getConnection();
            ps = con.prepareStatement(insertSql);

            int batchCount = 0;
            while (view.created != null) {
                Row row = view.created;

                int paramInd = 1;
                for (int fInd = 0; fInd < tableFields.length; ++fInd) {
                    JDBCCMPFieldBridge2 field = tableFields[fInd];
                    Object fieldValue = row.fields[field.getRowIndex()];
                    paramInd = field.setArgumentParameters(ps, paramInd, fieldValue);
                }

                insertStrategy.executeUpdate(ps);

                ++batchCount;
                row.flushStatus();
            }

            insertStrategy.executeBatch(ps);

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

    private EntityTable.View getView() {
        return (EntityTable.View) schema.getView(this);
    }

    public class View implements Table.View {
        private final Transaction tx;

        private Map rowByPk = new HashMap();
        private Row created;
        private Row deleted;
        private Row dirty;
        private Row dirtyRelations;
        private Row clean;

        private Row cacheUpdates;

        private List rowsWithNullFks;

        private boolean inFlush;

        public View(Transaction tx) {
            this.tx = tx;
        }

        public Row getRow(Object pk) {
            Row row;
            if (pk == null) {
                row = new Row(this);
            } else {
                row = getRowByPk(pk, false);
                if (row == null) {
                    row = createCleanRow(pk);
                }
            }
            return row;
        }

        public Row getRowByPk(Object pk, boolean required) {
            /*
            Row cursor = clean;
            while(cursor != null)
            {
               if(pk.equals(cursor.pk))
               {
                  return cursor;
               }
               cursor = cursor.next;
            }

            cursor = dirty;
            while(cursor != null)
            {
               if(pk.equals(cursor.pk))
               {
                  return cursor;
               }
               cursor = cursor.next;
            }

            cursor = created;
            while(cursor != null)
            {
               if(pk.equals(cursor.pk))
               {
                  return cursor;
               }
               cursor = cursor.next;
            }
            */

            Row row = (Row) rowByPk.get(pk);

            if (row == null) {
                Object[] fields;
                Object[] relations = null;
                try {
                    cache.lock(pk);

                    fields = cache.getFields(pk);
                    if (fields != null && relationsTotal > 0) {
                        relations = cache.getRelations(pk);
                        if (relations == null) {
                            relations = new Object[relationsTotal];
                        }
                    }
                } finally {
                    cache.unlock(pk);
                }

                if (fields != null) {
                    row = createCleanRow(pk, fields, relations);
                }
            }

            if (row == null && required) {
                throw CmpMessages.MESSAGES.rowNotFound(pk);
            }

            return row;
        }

        public void addClean(Row row) {
            if (clean != null) {
                row.next = clean;
                clean.prev = row;
            }

            clean = row;
            row.state = CLEAN;

            rowByPk.put(row.pk, row);
        }

        public void addCreated(Row row) throws DuplicateKeyException {
            if (created != null) {
                row.next = created;
                created.prev = row;
            }

            created = row;
            row.state = CREATED;

            rowByPk.put(row.pk, row);

            JDBCCMPFieldBridge2 versionField = entity.getVersionField();
            if (versionField != null) {
                row.fields[versionField.getVersionIndex()] = row.fields[versionField.getRowIndex()];
            }
        }

        public Row loadRow(ResultSet rs, Object pk, boolean searchableOnly) {
            Row row = getRowByPk(pk, false);
            if (row != null) {
                if (log.isTraceEnabled()) {
                    log.trace("row is already loaded: pk=" + pk);
                }
                return row;
            } else if (log.isTraceEnabled()) {
                log.trace("reading result set: pk=" + pk);
            }

            row = createCleanRow(pk);
            JDBCCMPFieldBridge2[] tableFields = (JDBCCMPFieldBridge2[]) entity.getTableFields();
            // this rsOffset is kind of a hack
            // but since tableIndex and rowIndex of a field are the same
            // this should work ok
            int rsOffset = 1;
            for (int i = 0; i < tableFields.length; ++i) {
                JDBCCMPFieldBridge2 field = tableFields[i];
                if (searchableOnly && !field.getJDBCType().isSearchable()) {
                    row.fields[field.getRowIndex()] = NOT_LOADED;
                    --rsOffset;
                    continue;
                }

                Object columnValue = field.loadArgumentResults(rs, field.getRowIndex() + rsOffset);
                row.fields[field.getRowIndex()] = columnValue;

                if (field.getVersionIndex() != -1) {
                    row.fields[field.getVersionIndex()] = columnValue;
                }
            }

            Object[] relations = (relationsTotal > 0 ? new Object[relationsTotal] : null);

            try {
                cache.lock(row.pk);
                cache.put(tx, row.pk, row.fields, relations);
            } finally {
                cache.unlock(row.pk);
            }

            return row;
        }

        public Object loadPk(ResultSet rs) {
            Object pk = null;
            JDBCCMPFieldBridge2[] pkFields = (JDBCCMPFieldBridge2[]) entity.getPrimaryKeyFields();
            //int rsInd = 1;
            for (int i = 0; i < pkFields.length; ++i) {
                JDBCCMPFieldBridge2 field = pkFields[i];
                //Object columnValue = field.loadArgumentResults(rs, rsInd++);
                Object columnValue = field.loadArgumentResults(rs, field.getRowIndex() + 1);
                pk = field.setPrimaryKeyValue(pk, columnValue);
            }
            return pk;
        }

        public boolean hasRow(Object id) {
            boolean has = rowByPk.containsKey(id);
            if (!has) {
                try {
                    cache.lock(id);
                    has = cache.contains(tx, id);
                } finally {
                    cache.unlock(id);
                }
            }
            return has;
        }

        public void addRowWithNullFk(Row row) {
            if (rowsWithNullFks == null) {
                rowsWithNullFks = new ArrayList();
            }
            rowsWithNullFks.add(row);
        }

        private Row createCleanRow(Object pk) {
            Row row = new Row(this);
            row.pk = pk;
            addClean(row);
            return row;
        }

        private Row createCleanRow(Object pk, Object[] fields, Object[] relations) {
            Row row = new Row(this, fields, relations);
            row.pk = pk;
            addClean(row);
            return row;
        }

        // Table.View implementation

        public void flushDeleted(Schema.Views views) throws SQLException {
            if (rowsWithNullFks != null) {
                nullifyForeignKeys();
                rowsWithNullFks = null;
            }

            if (deleted == null) {
                if (log.isTraceEnabled()) {
                    log.trace("no rows to delete");
                }
                return;
            }

            if (referencedBy != null) {
                if (inFlush) {
                    if (log.isTraceEnabled()) {
                        log.trace("inFlush, ignoring flushDeleted");
                    }
                    return;
                }

                inFlush = true;

                try {
                    for (int i = 0; i < referencedBy.length; ++i) {
                        final Table.View view = views.entityViews[referencedBy[i]];
                        if (view != null) {
                            view.flushDeleted(views);
                        }
                    }
                } finally {
                    inFlush = false;
                }
            }

            delete(this);
        }

        public void flushCreated(Schema.Views views) throws SQLException {
            if (created == null || dontFlushCreated) {
                if (log.isTraceEnabled()) {
                    log.trace("no rows to insert");
                }
                return;
            }

            if (references != null) {
                if (inFlush) {
                    if (log.isTraceEnabled()) {
                        log.trace("inFlush, ignoring flushCreated");
                    }
                    return;
                } else if (log.isTraceEnabled()) {
                    log.trace("flushing created references");
                }

                inFlush = true;
                try {
                    for (int i = 0; i < references.length; ++i) {
                        final Table.View view = views.entityViews[references[i]];
                        if (view != null) {
                            view.flushCreated(views);
                        }
                    }
                } finally {
                    inFlush = false;
                }
            }

            insert(this);
        }

        public void flushUpdated() throws SQLException {
            if (dirtyRelations != null) {
                while (dirtyRelations != null) {
                    Row row = dirtyRelations;
                    row.flushStatus();
                }
            }

            if (dirty == null) {
                if (log.isTraceEnabled()) {
                    log.trace("no rows to update");
                }
                return;
            }

            update(this);
        }

        public void beforeCompletion() {
        }

        public void committed() {
            if (cacheUpdates != null) {
                Row cursor = cacheUpdates;

                while (cursor != null) {
                    //if(cursor.lockedForUpdate)
                    //{
                    cache.lock(cursor.pk);
                    try {
                        switch (cursor.state) {
                            case CLEAN:
                                cache.put(tx, cursor.pk, cursor.fields, cursor.relations);
                                break;
                            case DELETED:
                                try {
                                    cache.remove(tx, cursor.pk);
                                } catch (Cache.RemoveException e) {
                                    log.trace(e.getMessage());
                                }
                                break;
                            default:
                                throw CmpMessages.MESSAGES.unexpectedRowState(entity.getQualifiedTableName(), cursor.pk, cursor.state);
                        }
                    } finally {
                        cache.unlock(cursor.pk);
                    }
                    //cursor.lockedForUpdate = false;
                    //}
                    cursor = cursor.nextCacheUpdate;
                }
            }
        }

        public void rolledback() {
        }

        private void nullifyForeignKeys()
                throws SQLException {
            if (log.isTraceEnabled()) {
                log.trace("nullifying foreign keys");
            }

            Connection con = null;
            PreparedStatement[] ps = new PreparedStatement[fkConstraints.length];

            try {
                final JDBCCMPFieldBridge2[] pkFields = (JDBCCMPFieldBridge2[]) entity.getPrimaryKeyFields();
                con = dataSource.getConnection();

                for (int i = 0; i < rowsWithNullFks.size(); ++i) {
                    final Row row = (Row) rowsWithNullFks.get(i);
                    final ForeignKeyConstraint[] cons = row.fkUpdates;
                    for (int c = 0; c < fkConstraints.length; ++c) {
                        if (cons[c] == null || row.state == DELETED && !cons[c].selfReference)
                            continue;

                        PreparedStatement s = ps[c];
                        if (s == null) {
                            if (log.isDebugEnabled()) {
                                log.debug("nullifying fk: " + cons[c].nullFkSql);
                            }
                            s = con.prepareStatement(cons[c].nullFkSql);
                            ps[c] = s;
                        }

                        int paramInd = 1;
                        for (int fInd = 0; fInd < pkFields.length; ++fInd) {
                            JDBCCMPFieldBridge2 pkField = pkFields[fInd];
                            Object fieldValue = row.fields[pkField.getRowIndex()];
                            paramInd = pkField.setArgumentParameters(s, paramInd, fieldValue);
                        }

                        final int affected = s.executeUpdate();
                        if (affected != 1) {
                            throw CmpMessages.MESSAGES.tooManyRowsAffected(affected);
                        }
                    }
                }
            } finally {
                for (int i = 0; i < ps.length; ++i) {
                    JDBCUtil.safeClose(ps[i]);
                }
                JDBCUtil.safeClose(con);
            }
        }
    }

    public class Row {
        private EntityTable.View view;
        private Object pk;
        private final Object[] fields;
        private final Object[] relations;

        private byte state;

        private Row prev;
        private Row next;

        private boolean cacheUpdateScheduled;
        private Row nextCacheUpdate;
        //private boolean lockedForUpdate;

        private ForeignKeyConstraint[] fkUpdates;

        public Row(EntityTable.View view) {
            this.view = view;
            fields = new Object[fieldsTotal];
            relations = (relationsTotal == 0 ? null : new Object[relationsTotal]);
            state = UNREFERENCED;
        }

        public Row(EntityTable.View view, Object[] fields, Object[] relations) {
            this.view = view;
            this.fields = fields;
            this.relations = relations;
            state = UNREFERENCED;
        }

        public Object getPk() {
            return pk;
        }

        public void loadCachedRelations(int index, Cache.CacheLoader loader) {
            if (relations != null) {
                final Object cached = relations[index];
                relations[index] = loader.loadFromCache(cached);
            }
        }

        public void cacheRelations(int index, Cache.CacheLoader loader) {
            relations[index] = loader.getCachedValue();
            scheduleCacheUpdate();
        }

        public void insert(Object pk) throws DuplicateKeyException {
            this.pk = pk;
            view.addCreated(this);
        }

        public Object getFieldValue(int i) {
            if (state == DELETED) {
                throw CmpMessages.MESSAGES.instanceAlreadyRemoved(pk);
            }

            Object value = fields[i];
            if (value == NOT_LOADED) {
                value = loadField(i);
            }

            return value;
        }

        public void setFieldValue(int i, Object value) {
            fields[i] = value;
        }

        public boolean isDirty() {
            return state != CLEAN && state != DIRTY_RELATIONS;
        }

        public void setDirty() {
            if (state == CLEAN || state == DIRTY_RELATIONS) {
                updateState(DIRTY);
            }
        }

        public void setDirtyRelations() {
            if (state == CLEAN) {
                updateState(DIRTY_RELATIONS);
            }
        }

        public void delete() {
            if (state == CLEAN || state == DIRTY || state == DIRTY_RELATIONS) {
                updateState(DELETED);
            } else if (state == CREATED) {
                dereference();
                state = DELETED;
                view.rowByPk.remove(pk);
            } else if (state == DELETED) {
                throw CmpMessages.MESSAGES.rowAlreadyRemoved(pk);
            }
        }

        public void nullForeignKey(ForeignKeyConstraint constraint) {
            if (fkUpdates == null) {
                fkUpdates = new ForeignKeyConstraint[fkConstraints.length];
                view.addRowWithNullFk(this);
            }

            fkUpdates[constraint.index] = constraint;
        }

        public void nonNullForeignKey(ForeignKeyConstraint constraint) {
            if (fkUpdates != null) {
                fkUpdates[constraint.index] = null;
            }
        }

        private void flushStatus() {
            if (state == CREATED || state == DIRTY) {
                updateState(CLEAN);
                fkUpdates = null;
            } else if (state == DELETED) {
                dereference();
            } else if (state == DIRTY_RELATIONS) {
                updateState(CLEAN);
                fkUpdates = null;
            }

            scheduleCacheUpdate();
        }

        private void scheduleCacheUpdate() {
            if (!cacheUpdateScheduled) {
                if (view.cacheUpdates == null) {
                    view.cacheUpdates = this;
                } else {
                    nextCacheUpdate = view.cacheUpdates;
                    view.cacheUpdates = this;
                }
                cacheUpdateScheduled = true;
            }
        }

        private void updateState(byte state) {
            dereference();

            if (state == CLEAN) {
                if (view.clean != null) {
                    next = view.clean;
                    view.clean.prev = this;
                }
                view.clean = this;
            } else if (state == DIRTY) {
                if (view.dirty != null) {
                    next = view.dirty;
                    view.dirty.prev = this;
                }
                view.dirty = this;
            } else if (state == CREATED) {
                if (view.created != null) {
                    next = view.created;
                    view.created.prev = this;
                }
                view.created = this;
            } else if (state == DELETED) {
                if (view.deleted != null) {
                    next = view.deleted;
                    view.deleted.prev = this;
                }
                view.deleted = this;
            } else if (state == DIRTY_RELATIONS) {
                if (view.dirtyRelations != null) {
                    next = view.dirtyRelations;
                    view.dirtyRelations.prev = this;
                }
                view.dirtyRelations = this;
            } else {
                throw CmpMessages.MESSAGES.canNotUpdateState(state);
            }

            this.state = state;
        }

        private void dereference() {
            if (state == CLEAN && view.clean == this) {
                view.clean = next;
            } else if (state == DIRTY && view.dirty == this) {
                view.dirty = next;
            } else if (state == CREATED && view.created == this) {
                view.created = next;
            } else if (state == DELETED && view.deleted == this) {
                view.deleted = next;
            } else if (state == DIRTY_RELATIONS && view.dirtyRelations == this) {
                view.dirtyRelations = next;
            }

            if (next != null) {
                next.prev = prev;
            }

            if (prev != null) {
                prev.next = next;
            }

            prev = null;
            next = null;
        }

        public void flush() throws SQLException, DuplicateKeyException {
            // todo needs refactoring

            if (state != CREATED) {
                if (log.isTraceEnabled()) {
                    log.trace("The row is already inserted: pk=" + pk);
                }
                return;
            }

            Connection con = null;
            PreparedStatement duplicatePkPs = null;
            PreparedStatement insertPs = null;
            ResultSet rs = null;
            try {
                int paramInd;
                con = dataSource.getConnection();

                // insert
                if (log.isDebugEnabled()) {
                    log.debug("executing : " + insertSql);
                }

                insertPs = con.prepareStatement(insertSql);

                paramInd = 1;
                JDBCCMPFieldBridge2[] tableFields = (JDBCCMPFieldBridge2[]) entity.getTableFields();
                for (int fInd = 0; fInd < tableFields.length; ++fInd) {
                    JDBCCMPFieldBridge2 field = tableFields[fInd];
                    Object fieldValue = fields[field.getRowIndex()];
                    paramInd = field.setArgumentParameters(insertPs, paramInd, fieldValue);
                }

                insertPs.executeUpdate();

                flushStatus();
            } catch (SQLException e) {
                throw CmpMessages.MESSAGES.failedToInsertNewRows(e);
            } finally {
                JDBCUtil.safeClose(rs);
                JDBCUtil.safeClose(duplicatePkPs);
                JDBCUtil.safeClose(insertPs);
                JDBCUtil.safeClose(con);
            }
        }

        private Object loadField(int i) {
            JDBCCMPFieldBridge2 field = (JDBCCMPFieldBridge2) entity.getFields().get(i);

            StringBuffer query = new StringBuffer();
            query.append("select ")
                    .append(field.getColumnName())
                    .append(" from ")
                    .append(tableName)
                    .append(" where ");

            JDBCCMPFieldBridge2[] pkFields = (JDBCCMPFieldBridge2[]) entity.getPrimaryKeyFields();
            for (int pkI = 0; pkI < pkFields.length; ++pkI) {
                if (pkI > 0) {
                    query.append(" and ");
                }
                query.append(pkFields[pkI].getColumnName()).append("=?");
            }

            if (log.isDebugEnabled()) {
                log.debug("executing: " + query.toString());
            }

            Object value = null;
            Connection con = null;
            PreparedStatement ps = null;
            ResultSet rs = null;

            try {
                con = dataSource.getConnection();
                ps = con.prepareStatement(query.toString());

                for (int pkI = 0; pkI < pkFields.length; ++pkI) {
                    JDBCCMPFieldBridge2 pkField = pkFields[pkI];
                    Object fieldValue = fields[pkField.getRowIndex()];
                    pkField.setArgumentParameters(ps, pkI + 1, fieldValue);
                }

                rs = ps.executeQuery();

                if (!rs.next()) {
                    throw CmpMessages.MESSAGES.rowNotFound(pk);
                }

                value = field.loadArgumentResults(rs, 1);
            } catch (SQLException e) {
                throw CmpMessages.MESSAGES.failedToLoadField(entity.getEntityName(), field.getFieldName(), e);
            } finally {
                JDBCUtil.safeClose(rs);
                JDBCUtil.safeClose(ps);
                JDBCUtil.safeClose(con);
            }

            fields[field.getRowIndex()] = value;
            return value;
        }
    }

    public static interface CommitStrategy {
        void executeUpdate(PreparedStatement ps) throws SQLException;

        void executeBatch(PreparedStatement ps) throws SQLException;
    }

    private static final CommitStrategy BATCH_UPDATE = new CommitStrategy() {
        public void executeUpdate(PreparedStatement ps) throws SQLException {
            ps.addBatch();
        }

        public void executeBatch(PreparedStatement ps) throws SQLException {
            int[] updates = ps.executeBatch();
            for (int i = 0; i < updates.length; ++i) {
                int status = updates[i];
                if (status != 1 && status != -2 /* java.sql.Statement.SUCCESS_NO_INFO since jdk1.4*/) {
                    if(status == -3) {
                        throw CmpMessages.MESSAGES.batchCommandFailedExecute();
                    } else {
                        throw CmpMessages.MESSAGES.batchUpdatedTooManyRows(status);
                    }

                }
            }
        }
    };

    private static final CommitStrategy NON_BATCH_UPDATE = new CommitStrategy() {
        public void executeUpdate(PreparedStatement ps) throws SQLException {
            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw CmpMessages.MESSAGES.expectedOneRow(rows);
            }
        }

        public void executeBatch(PreparedStatement ps) {
        }
    };

    public class ForeignKeyConstraint {
        public final int index;
        private final String nullFkSql;
        private final boolean selfReference;

        public ForeignKeyConstraint(int index, JDBCCMPFieldBridge2[] fkFields, boolean selfReference) {
            this.index = index;
            this.selfReference = selfReference;

            StringBuffer buf = new StringBuffer();
            buf.append("update ").append(tableName).append(" set ")
                    .append(fkFields[0].getColumnName()).append("=null");
            for (int i = 1; i < fkFields.length; ++i) {
                buf.append(", ").append(fkFields[i].getColumnName()).append("=null");
            }

            buf.append(" where ");
            JDBCCMPFieldBridge2[] pkFields = (JDBCCMPFieldBridge2[]) entity.getPrimaryKeyFields();
            buf.append(pkFields[0].getColumnName()).append("=?");
            for (int i = 1; i < pkFields.length; ++i) {
                buf.append(" and ").append(pkFields[i].getColumnName()).append("=?");
            }

            nullFkSql = buf.toString();
            if (log.isDebugEnabled()) {
                log.debug("update foreign key sql: " + nullFkSql);
            }
        }
    }

    public void stop() throws Exception {
//        if (cacheInvalidator != null) {
//            cacheInvalidator.unregister();
//        }
//
//        if (cacheName != null) {
//            serviceController.stop(cacheName);
//            serviceController.destroy(cacheName);
//            serviceController.remove(cacheName);
//        }
//        serviceController = null;
    }
}
