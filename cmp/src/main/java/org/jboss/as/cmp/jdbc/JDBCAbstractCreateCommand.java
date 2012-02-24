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

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.ejb.CreateException;
import org.jboss.as.cmp.CmpMessages;
import static org.jboss.as.cmp.CmpMessages.MESSAGES;
import org.jboss.as.cmp.context.CmpEntityBeanContext;
import org.jboss.as.cmp.jdbc.bridge.JDBCCMPFieldBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCCMRFieldBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCEntityBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCFieldBridge;
import org.jboss.as.cmp.jdbc.metadata.JDBCEntityCommandMetaData;
import org.jboss.logging.Logger;
import org.jboss.security.AuthenticationManager;

/**
 * Base class for create commands that drives the operation sequence.
 *
 * @author <a href="mailto:jeremy@boynes.com">Jeremy Boynes</a>
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 */
public abstract class JDBCAbstractCreateCommand implements JDBCCreateCommand {
    protected Logger log;
    protected boolean debug;
    protected boolean trace;
    protected JDBCEntityBridge entity;
    protected AuthenticationManager securityManager;
    protected boolean createAllowed;
    protected SQLExceptionProcessor exceptionProcessor;
    protected String insertSQL;
    protected JDBCFieldBridge[] insertFields;
    protected boolean insertAfterEjbPostCreate;

    // Generated fields
    private JDBCCMPFieldBridge createdPrincipal;
    private JDBCCMPFieldBridge createdTime;
    private JDBCCMPFieldBridge updatedPrincipal;
    private JDBCCMPFieldBridge updatedTime;

    public void init(JDBCStoreManager manager) {
        log = Logger.getLogger(getClass().getName() + '.' + manager.getMetaData().getName());
        debug = log.isDebugEnabled();
        trace = log.isTraceEnabled();

        entity = (JDBCEntityBridge) manager.getEntityBridge();
        insertAfterEjbPostCreate = manager.getCmpConfig().isInsertAfterEjbPostCreate();

        // set create allowed
        createAllowed = true;
        JDBCFieldBridge[] pkFields = entity.getPrimaryKeyFields();
        for (int i = 0; i < pkFields.length; i++) {
            if (pkFields[i].isReadOnly()) {
                createAllowed = false;
                log.debug("Create will not be allowed because pk field "
                        + pkFields[i].getFieldName() + "is read only.");
                break;
            }
        }

        initGeneratedFields();

        JDBCEntityCommandMetaData entityCommand = manager.getMetaData().getEntityCommand();
        if (entityCommand == null) {
            throw MESSAGES.entityCommandIsNull();
        }
        initEntityCommand(entityCommand);

        initInsertFields();
        initInsertSQL();
    }

    protected void initEntityCommand(JDBCEntityCommandMetaData entityCommand) {
        exceptionProcessor = null;
    }

    public Object execute(Method m, Object[] args, CmpEntityBeanContext ctx) throws CreateException {
        // TODO: implement this logic nicer
        if (insertAfterEjbPostCreate) {
            if (!JDBCEntityBridge.isEjbCreateDone(ctx)) {
                checkCreateAllowed();
                generateFields(ctx);
                JDBCEntityBridge.setEjbCreateDone(ctx);
            } else {
                beforeInsert(ctx);
                performInsert(ctx);
                afterInsert(ctx);
                JDBCEntityBridge.setCreated(ctx);
            }
        } else {
            checkCreateAllowed();
            generateFields(ctx);
            beforeInsert(ctx);
            performInsert(ctx);
            afterInsert(ctx);
            JDBCEntityBridge.setCreated(ctx);
        }
        return getPrimaryKey(ctx);
    }

    protected void checkCreateAllowed() throws CreateException {
        if (!createAllowed) {
            throw MESSAGES.creationNotAllowedPKReadOnly();
        }
    }

    protected JDBCCMPFieldBridge getGeneratedPKField() {
        // extract the pk field to be generated
        JDBCCMPFieldBridge pkField = null;
        JDBCFieldBridge[] pkFields = entity.getPrimaryKeyFields();
        for (int i = 0; i < pkFields.length; ++i) {
            if (pkField != null)
                throw MESSAGES.generationOnlySupportedWithSinglePK();
            pkField = (JDBCCMPFieldBridge) pkFields[i];
        }
        return pkField;
    }

    protected void initGeneratedFields() {
        createdPrincipal = entity.getCreatedPrincipalField();
        if (securityManager == null && createdPrincipal != null) {
            throw MESSAGES.noSecurityDomainForCreatedBy();
        }
        updatedPrincipal = entity.getUpdatedPrincipalField();
        if (securityManager == null && updatedPrincipal != null) {
            throw MESSAGES.noSecurityDomainForCreatedBy();
        }
        createdTime = entity.getCreatedTimeField();
        updatedTime = entity.getUpdatedTimeField();
    }

    protected void generateFields(CmpEntityBeanContext ctx) throws CreateException {
        // Audit principal fields
        if (securityManager != null) {
            String principalName = ctx.getCallerPrincipal().getName();
            if (createdPrincipal != null && createdPrincipal.getInstanceValue(ctx) == null) {
                createdPrincipal.setInstanceValue(ctx, principalName);
            }
            /*
            if(updatedPrincipal != null && updatedPrincipal.getInstanceValue(ctx) == null)
            {
               updatedPrincipal.setInstanceValue(ctx, principalName);
            }
            */
        }

        // Audit time fields
        Date date = null;
        if (createdTime != null && createdTime.getInstanceValue(ctx) == null) {
            date = new Date();
            createdTime.setInstanceValue(ctx, date);
        }
        /*
        if(updatedTime != null && updatedTime.getInstanceValue(ctx) == null)
        {
           if(date == null)
              date = new Date();
           updatedTime.setInstanceValue(ctx, date);
        }
        */
    }

    protected void initInsertFields() {
        JDBCFieldBridge[] fields = entity.getTableFields();
        List insertFieldsList = new ArrayList(fields.length);
        for (int i = 0; i < fields.length; i++) {
            JDBCFieldBridge field = fields[i];
            if (isInsertField(field))
                insertFieldsList.add(field);
        }

        insertFields = (JDBCFieldBridge[]) insertFieldsList.toArray(new JDBCFieldBridge[insertFieldsList.size()]);
    }

    protected boolean isInsertField(JDBCFieldBridge field) {
        boolean result =
                !(field instanceof JDBCCMRFieldBridge)
                        && field.getJDBCType() != null
                        && !field.isReadOnly();
        if (field instanceof JDBCCMPFieldBridge)
            result = result && !((JDBCCMPFieldBridge) field).isRelationTableField();
        return result;
    }

    protected void initInsertSQL() {
        StringBuffer sql = new StringBuffer(250);
        sql.append(SQLUtil.INSERT_INTO)
                .append(entity.getQualifiedTableName())
                .append(" (");

        SQLUtil.getColumnNamesClause(insertFields, sql);

        sql.append(')')
                .append(SQLUtil.VALUES).append('(');
        SQLUtil.getValuesClause(insertFields, sql)
                .append(')');
        insertSQL = sql.toString();

        if (debug)
            log.debug("Insert Entity SQL: " + insertSQL);
    }

    protected void beforeInsert(CmpEntityBeanContext ctx) throws CreateException {
    }

    protected void performInsert(CmpEntityBeanContext ctx) throws CreateException {
        Connection c = null;
        PreparedStatement ps = null;
        boolean throwRuntimeExceptions = entity.getMetaData().getThrowRuntimeExceptions();

        // if metadata is true, the getConnection is done inside
        // its own try catch block to throw a runtime exception (EJBException)
        if (throwRuntimeExceptions) {
            try {
                c = entity.getDataSource().getConnection();
            } catch (SQLException sqle) {
                javax.ejb.EJBException ejbe = new javax.ejb.EJBException("Could not get a connection; " + sqle);
                ejbe.initCause(sqle);
                throw ejbe;
            }
        }

        try {
            if (log.isDebugEnabled()) {
                log.debug("Executing SQL: " + insertSQL);
            }


            // if metadata is false, the getConnection is done inside this try catch block
            if (!throwRuntimeExceptions) {
                c = entity.getDataSource().getConnection();
            }
            ps = prepareStatement(c, insertSQL, ctx);

            // set the parameters
            int index = 1;
            for (int fieldInd = 0; fieldInd < insertFields.length; ++fieldInd) {
                index = insertFields[fieldInd].setInstanceParameters(ps, index, ctx);
            }

            // execute statement
            int rowsAffected = executeInsert(index, ps, ctx);
            if (rowsAffected != 1) {
                throw CmpMessages.MESSAGES.expectedOneRow(rowsAffected, ctx.getPrimaryKey());
            }
        } catch (SQLException e) {
            if (exceptionProcessor != null && exceptionProcessor.isDuplicateKey(e)) {
                throw CmpMessages.MESSAGES.uniqueKeyViolationInvalidFk(ctx.getPrimaryKey());
            } else {
                throw CmpMessages.MESSAGES.couldNotCreateEntity(e);
            }
        } finally {
            JDBCUtil.safeClose(ps);
            JDBCUtil.safeClose(c);
        }

        // Mark the inserted fields as clean.
        for (int fieldInd = 0; fieldInd < insertFields.length; ++fieldInd) {
            insertFields[fieldInd].setClean(ctx);
        }
    }

    protected PreparedStatement prepareStatement(Connection c, String sql, CmpEntityBeanContext ctx) throws SQLException {
        return c.prepareStatement(sql);
    }

    protected int executeInsert(int paramIndex, PreparedStatement ps, CmpEntityBeanContext ctx) throws SQLException {
        return ps.executeUpdate();
    }

    protected void afterInsert(CmpEntityBeanContext ctx) throws CreateException {
    }

    protected Object getPrimaryKey(CmpEntityBeanContext ctx) {
        return entity.extractPrimaryKeyFromInstance(ctx);
    }
}
