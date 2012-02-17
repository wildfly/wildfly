/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.cmp.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import javax.ejb.EJBException;
import javax.ejb.NoSuchEntityException;
import org.jboss.as.cmp.jdbc.bridge.JDBCCMPFieldBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCEntityBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCFieldBridge;
import org.jboss.as.cmp.jdbc.metadata.JDBCFunctionMappingMetaData;
import org.jboss.as.cmp.context.CmpEntityBeanContext;
import org.jboss.logging.Logger;

/**
 * JDBCLoadEntityCommand loads the data for an instance from the table.
 * This command implements specified eager loading. For CMP 2.x, the
 * entity can be configured to only load some of the fields, which is
 * helpful for entities with lots of data.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @author <a href="mailto:on@ibis.odessa.ua">Oleg Nitz</a>
 * @author <a href="mailto:rickard.oberg@telkel.com">Rickard Oberg</a>
 * @author <a href="mailto:marc.fleury@telkel.com">Marc Fleury</a>
 * @author <a href="mailto:shevlandj@kpi.com.au">Joe Shevland</a>
 * @author <a href="mailto:justin@j-m-f.demon.co.uk">Justin Forder</a>
 * @author <a href="mailto:dirk@jboss.de">Dirk Zimmermann</a>
 * @author <a href="mailto:danch@nvisia.com">danch (Dan Christopherson)</a>
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @version $Revision: 81030 $
 */
public final class JDBCLoadEntityCommand {
    private final JDBCStoreManager manager;
    private final JDBCEntityBridge entity;
    private final Logger log;
    private final JDBCFunctionMappingMetaData rowLockingTemplate;

    public JDBCLoadEntityCommand(JDBCStoreManager manager) {
        this.manager = manager;
        entity = (JDBCEntityBridge) manager.getEntityBridge();
        boolean rowLocking = entity.getMetaData().hasRowLocking();
        rowLockingTemplate = rowLocking ? entity.getMetaData().getTypeMapping().getRowLockingTemplate() : null;

        // Create the Log
        log = Logger.getLogger(
                this.getClass().getName() +
                        "." +
                        manager.getMetaData().getName());
    }

    /**
     * Loads entity.
     * If failIfNotFound is true and entity wasn't found then NoSuchEntityException is thrown.
     * Otherwise, if entity wasn't found, returns false.
     * If entity was loaded successfully return true.
     *
     * @param ctx            - entity context;
     * @param failIfNotFound - whether to fail if entity wasn't found;
     * @return true if entity was loaded, false - otherwise.
     */
    public boolean execute(CmpEntityBeanContext ctx, boolean failIfNotFound) {
        return execute(null, ctx, failIfNotFound);
    }

    /**
     * Loads entity or required field. If entity not found throws NoSuchEntityException.
     *
     * @param requiredField - required field or null;
     * @param ctx           - the corresponding context;
     */
    public void execute(JDBCCMPFieldBridge requiredField, CmpEntityBeanContext ctx) {
        execute(requiredField, ctx, true);
    }


    /**
     * Loads entity or required field.
     * If failIfNotFound is set to true, then NoSuchEntityException is thrown if the
     * entity wasn't found.
     * If failIfNotFound is false then if the entity wasn't found returns false,
     * if the entity was loaded successfully, returns true.
     *
     * @param requiredField  - required field;
     * @param ctx            - entity context;
     * @param failIfNotFound - whether to fail if entity wasn't loaded.
     * @return true if entity was loaded, false - otherwise.
     */
    private boolean execute(JDBCCMPFieldBridge requiredField,
                            CmpEntityBeanContext ctx,
                            boolean failIfNotFound) {
        // load the instance primary key fields into the context
        Object id = ctx.getPrimaryKey();
        entity.injectPrimaryKeyIntoInstance(ctx, id);

        // get the read ahead cache
        ReadAheadCache readAheadCache = manager.getReadAheadCache();

        // load any preloaded fields into the context
        if (readAheadCache.load(ctx)) {
            if (requiredField == null || (requiredField != null && requiredField.isLoaded(ctx))) {
                return true;
            }
        }

        // get the finder results associated with this context, if it exists
        ReadAheadCache.EntityReadAheadInfo info = readAheadCache.getEntityReadAheadInfo(id);

        // determine the fields to load
        JDBCEntityBridge.FieldIterator loadIter = entity.getLoadIterator(requiredField, info.getReadAhead(), ctx);
        if (!loadIter.hasNext())
            return true;

        // get the keys to load
        List loadKeys = info.getLoadKeys();

        // generate the sql
        String sql = (rowLockingTemplate != null ? getRawLockingSQL(loadIter, loadKeys.size()) : getSQL(loadIter, loadKeys.size()));

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            // create the statement
            if (log.isDebugEnabled()) {
                log.debug("Executing SQL: " + sql);
            }

            // get the connection
            con = entity.getDataSource().getConnection();
            ps = con.prepareStatement(sql);

            // Set the fetch size of the statement
            if (entity.getFetchSize() > 0) {
                ps.setFetchSize(entity.getFetchSize());
            }

            // set the parameters
            int paramIndex = 1;
            for (int i = 0; i < loadKeys.size(); i++) {
                paramIndex = entity.setPrimaryKeyParameters(ps, paramIndex, loadKeys.get(i));
            }

            // execute statement
            rs = ps.executeQuery();
            // load results
            boolean mainEntityLoaded = false;
            Object[] ref = new Object[1];
            while (rs.next()) {
                // reset the column index for this row
                int index = 1;

                // ref must be reset to null before load
                ref[0] = null;

                // if we are loading more then one entity, load the pk from the row
                Object pk = null;
                if (loadKeys.size() > 1) {
                    // load the pk
                    index = entity.loadPrimaryKeyResults(rs, index, ref);
                    pk = ref[0];
                }
                // is this the main entity or a preload entity
                if (loadKeys.size() == 1 || pk.equals(id)) {
                    // main entity; load the values into the context
                    loadIter.reset();
                    while (loadIter.hasNext()) {
                        JDBCCMPFieldBridge field = loadIter.next();
                        index = field.loadInstanceResults(rs, index, ctx);
                        field.setClean(ctx);
                    }
                    mainEntityLoaded = true;
                } else {
                    // preload entity; load the values into the read ahead cache
                    loadIter.reset();
                    while (loadIter.hasNext()) {
                        JDBCCMPFieldBridge field = loadIter.next();
                        // ref must be reset to null before load
                        ref[0] = null;

                        // load the result of the field
                        index = field.loadArgumentResults(rs, index, ref);

                        // cache the field value
                        readAheadCache.addPreloadData(pk, field, ref[0]);
                    }
                }
            }

            // clear LOAD_REQUIRED flag
            loadIter.removeAll();
            // did we load the main results
            if (!mainEntityLoaded) {
                if (failIfNotFound)
                    throw new NoSuchEntityException("Entity not found: primaryKey=" + ctx.getPrimaryKey());
                else
                    return false;
            } else
                return true;
        } catch (EJBException e) {
            throw e;
        } catch (Exception e) {
            throw new EJBException("Load failed", e);
        } finally {
            JDBCUtil.safeClose(rs);
            JDBCUtil.safeClose(ps);
            JDBCUtil.safeClose(con);
        }
    }

    private String getSQL(JDBCEntityBridge.FieldIterator loadIter, int keyCount) {
        StringBuffer sql = new StringBuffer(250);
        sql.append(SQLUtil.SELECT);

        // if we are loading more then one entity we need to add the primary
        // key to the load fields to match up the results with the correct entity.
        JDBCFieldBridge[] primaryKeyFields = entity.getPrimaryKeyFields();
        if (keyCount > 1) {
            SQLUtil.getColumnNamesClause(primaryKeyFields, sql);
            sql.append(SQLUtil.COMMA);
        }
        SQLUtil.getColumnNamesClause(loadIter, sql);
        sql.append(SQLUtil.FROM)
                .append(entity.getQualifiedTableName())
                .append(SQLUtil.WHERE);

        //
        // where clause
        String pkWhere = SQLUtil.getWhereClause(primaryKeyFields, new StringBuffer(50)).toString();
        sql.append('(').append(pkWhere).append(')');
        for (int i = 1; i < keyCount; i++) {
            sql.append(SQLUtil.OR).append('(').append(pkWhere).append(')');
        }

        return sql.toString();
    }

    private String getRawLockingSQL(JDBCEntityBridge.FieldIterator loadIter, int keyCount) {
        //
        // column names clause
        StringBuffer columnNamesClause = new StringBuffer(250);
        // if we are loading more then one entity we need to add the primary
        // key to the load fields to match up the results with the correct
        // entity.
        if (keyCount > 1) {
            SQLUtil.getColumnNamesClause(entity.getPrimaryKeyFields(), columnNamesClause);
            columnNamesClause.append(SQLUtil.COMMA);
        }

        SQLUtil.getColumnNamesClause(loadIter, columnNamesClause);

        //
        // table name clause
        String tableName = entity.getQualifiedTableName();

        //
        // where clause
        String whereClause = SQLUtil.
                getWhereClause(entity.getPrimaryKeyFields(), new StringBuffer(50)).toString();
        if (keyCount > 0) {
            StringBuffer sb = new StringBuffer((whereClause.length() + 6) * keyCount + 4);
            for (int i = 0; i < keyCount; i++) {
                if (i > 0)
                    sb.append(SQLUtil.OR);
                sb.append('(').append(whereClause).append(')');
            }
            whereClause = sb.toString();
        }

        String[] args = new String[]{
                columnNamesClause.toString(),
                tableName,
                whereClause,
                null // order by
        };
        return rowLockingTemplate.getFunctionSql(args, new StringBuffer(300)).toString();
    }
}
