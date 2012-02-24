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
import javax.ejb.EJBException;
import org.jboss.as.cmp.CmpMessages;
import org.jboss.as.cmp.context.CmpEntityBeanContext;
import org.jboss.as.cmp.jdbc.bridge.JDBCCMPFieldBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCEntityBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCFieldBridge;
import org.jboss.logging.Logger;

/**
 * JDBCStoreEntityCommand updates the row with the new state.
 * In the event that no field is dirty the command just returns.
 * Note: read-only fields are never considered dirty.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @author <a href="mailto:rickard.oberg@telkel.com">Rickard Oberg</a>
 * @author <a href="mailto:marc.fleury@telkel.com">Marc Fleury</a>
 * @author <a href="mailto:shevlandj@kpi.com.au">Joe Shevland</a>
 * @author <a href="mailto:justin@j-m-f.demon.co.uk">Justin Forder</a>
 * @author <a href="mailto:sebastien.alborini@m4x.org">Sebastien Alborini</a>
 * @author <a href="mailto:alex@jboss.org">Alex Loubyansky</a>
 * @version $Revision: 81030 $
 */
public final class JDBCStoreEntityCommand {
    private final JDBCEntityBridge entity;
    private final JDBCFieldBridge[] primaryKeyFields;
    private final Logger log;

    public JDBCStoreEntityCommand(JDBCStoreManager manager) {
        entity = (JDBCEntityBridge) manager.getEntityBridge();
        primaryKeyFields = entity.getPrimaryKeyFields();

        // Create the Log
        log = Logger.getLogger(
                this.getClass().getName() +
                        "." +
                        manager.getMetaData().getName());
    }

    public void execute(CmpEntityBeanContext ctx) {
        // scheduled for batch cascade-delete instance should not be updated
        // because foreign key fields could be updated to null and cascade-delete will fail.
        JDBCEntityBridge.FieldIterator dirtyIterator = entity.getDirtyIterator(ctx);
        if (!dirtyIterator.hasNext() || entity.isBeingRemoved(ctx) || entity.isScheduledForBatchCascadeDelete(ctx)) {
            if (log.isTraceEnabled()) {
                log.trace("Store command NOT executed. Entity is not dirty "
                        + ", is being removed or scheduled for *batch* cascade delete: pk=" + ctx.getPrimaryKey());
            }
            return;
        }

        // generate sql
        StringBuffer sql = new StringBuffer(200);
        sql.append(SQLUtil.UPDATE)
                .append(entity.getQualifiedTableName())
                .append(SQLUtil.SET);
        SQLUtil.getSetClause(dirtyIterator, sql)
                .append(SQLUtil.WHERE);
        SQLUtil.getWhereClause(primaryKeyFields, sql);

        boolean hasLockedFields = entity.hasLockedFields(ctx);
        JDBCEntityBridge.FieldIterator lockedIterator = null;
        if (hasLockedFields) {
            lockedIterator = entity.getLockedIterator(ctx);
            while (lockedIterator.hasNext()) {
                sql.append(SQLUtil.AND);
                JDBCCMPFieldBridge field = lockedIterator.next();
                if (field.getLockedValue(ctx) == null) {
                    SQLUtil.getIsNullClause(false, field, "", sql);
                    lockedIterator.remove();
                } else {
                    SQLUtil.getWhereClause(field, sql);
                }
            }
        }

        Connection con = null;
        PreparedStatement ps = null;
        int rowsAffected = 0;
        try {
            // create the statement
            if (log.isDebugEnabled()) {
                log.debug("Executing SQL: " + sql);
            }

            // get the connection
            con = entity.getDataSource().getConnection();
            ps = con.prepareStatement(sql.toString());

            // SET: set the dirty fields parameters
            int index = 1;
            dirtyIterator.reset();
            while (dirtyIterator.hasNext()) {
                index = dirtyIterator.next().setInstanceParameters(ps, index, ctx);
            }

            // WHERE: set primary key fields
            index = entity.setPrimaryKeyParameters(ps, index, ctx.getPrimaryKey());

            // WHERE: set optimistically locked field values
            if (hasLockedFields) {
                lockedIterator.reset();
                while (lockedIterator.hasNext()) {
                    JDBCCMPFieldBridge field = lockedIterator.next();
                    Object value = field.getLockedValue(ctx);
                    index = field.setArgumentParameters(ps, index, value);
                }
            }

            // execute statement
            rowsAffected = ps.executeUpdate();
        } catch (EJBException e) {
            throw e;
        } catch (Exception e) {
            throw CmpMessages.MESSAGES.storeFailed(e);
        } finally {
            JDBCUtil.safeClose(ps);
            JDBCUtil.safeClose(con);
        }

        // check results
        if (rowsAffected != 1) {
            throw CmpMessages.MESSAGES.updateFailedTooManyRowsAffected(rowsAffected, ctx.getPrimaryKey());
        }

        // Mark the updated fields as clean.
        dirtyIterator.reset();
        while (dirtyIterator.hasNext()) {
            dirtyIterator.next().setClean(ctx);
        }
    }
}
