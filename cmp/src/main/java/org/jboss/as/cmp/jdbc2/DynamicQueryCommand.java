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
package org.jboss.as.cmp.jdbc2;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.ejb.FinderException;
import org.jboss.as.cmp.CmpMessages;
import org.jboss.as.cmp.jdbc.EJBQLToSQL92Compiler;
import org.jboss.as.cmp.jdbc.JDBCQueryCommand;
import org.jboss.as.cmp.jdbc.QLCompiler;
import org.jboss.as.cmp.jdbc.QueryParameter;
import org.jboss.as.cmp.jdbc.metadata.JDBCDynamicQLQueryMetaData;
import org.jboss.as.cmp.jdbc2.bridge.JDBCCMPFieldBridge2;
import org.jboss.as.cmp.jdbc2.bridge.JDBCEntityBridge2;
import org.jboss.as.cmp.jdbc2.schema.Schema;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @version <tt>$Revision: 81030 $</tt>
 */
public class DynamicQueryCommand implements QueryCommand {
    private Logger log;
    private JDBCEntityBridge2 entity;
    private JDBCDynamicQLQueryMetaData metadata;
    private AbstractQueryCommand.CollectionFactory collectionFactory;

    public DynamicQueryCommand(JDBCEntityBridge2 entity, JDBCDynamicQLQueryMetaData metadata) {
        log =
                Logger.getLogger(getClass().getName() + "." + entity.getEntityName() + "#" + metadata.getMethod().getName());
        this.entity = entity;
        this.metadata = metadata;

        Class returnType = metadata.getMethod().getReturnType();
        if (Collection.class.isAssignableFrom(returnType)) {
            if (Set.class.isAssignableFrom(returnType)) {
                collectionFactory = AbstractQueryCommand.SET_FACTORY;
            } else {
                collectionFactory = AbstractQueryCommand.COLLECTION_FACTORY;
            }
        }
    }

    public JDBCStoreManager2 getStoreManager() {
        return (JDBCStoreManager2) entity.getManager();
    }

    public Collection fetchCollection(Schema schema, Object[] args, JDBCQueryCommand.EntityProxyFactory factory)
            throws FinderException {
        if (log.isTraceEnabled()) {
            log.trace("executing dynamic-ql: " + args[0]);
        }

        JDBCStoreManager2 manager = (JDBCStoreManager2) entity.getManager();
        QLCompiler compiler = new EJBQLToSQL92Compiler(manager.getCatalog());
        try {
            compiler.compileJBossQL((String) args[0],
                    metadata.getMethod().getReturnType(),
                    getParamTypes(args),
                    metadata
            );
        } catch (Throwable t) {
            throw CmpMessages.MESSAGES.errorCompilingJbossQlStatement(args[0], t);
        }

        String sql = compiler.getSQL();

        int offsetParam = compiler.getOffsetParam();
        int offsetValue = compiler.getOffsetValue();
        int limitParam = compiler.getLimitParam();
        int limitValue = compiler.getLimitValue();

        AbstractQueryCommand.ResultReader resultReader;
        if (!compiler.isSelectEntity()) {
            if (compiler.isSelectField()) {
                resultReader = new AbstractQueryCommand.FieldReader((JDBCCMPFieldBridge2) compiler.getSelectField());
            } else {
                resultReader = new AbstractQueryCommand.FunctionReader(compiler.getSelectFunction());
            }
        } else {
            resultReader = new AbstractQueryCommand.EntityReader((JDBCEntityBridge2) compiler.getSelectEntity(), compiler.isSelectDistinct());
        }

        return AbstractQueryCommand.fetchCollection(
                entity, sql, toArray(compiler.getInputParameters()),
                AbstractQueryCommand.toInt(args, offsetParam, offsetValue), AbstractQueryCommand.toInt(args, limitParam, limitValue),
                new AbstractQueryCommand.EagerCollectionStrategy(collectionFactory, resultReader, log),
                schema, (Object[]) args[1], factory, log);
    }

    public Object fetchOne(Schema schema, Object[] args, JDBCQueryCommand.EntityProxyFactory factory) throws FinderException {
        if (log.isTraceEnabled()) {
            log.trace("executing dynamic-ql: " + args[0]);
        }

        JDBCStoreManager2 manager = (JDBCStoreManager2) entity.getManager();
        QLCompiler compiler = new EJBQLToSQL92Compiler(manager.getCatalog());
        try {
            compiler.compileJBossQL((String) args[0],
                    metadata.getMethod().getReturnType(),
                    getParamTypes(args),
                    metadata
            );
        } catch (Throwable t) {
            throw CmpMessages.MESSAGES.errorCompilingJbossQlStatement(args[0], t);
        }

        String sql = compiler.getSQL();

        AbstractQueryCommand.ResultReader resultReader;
        if (!compiler.isSelectEntity()) {
            if (compiler.isSelectField()) {
                resultReader = new AbstractQueryCommand.FieldReader((JDBCCMPFieldBridge2) compiler.getSelectField());
            } else {
                resultReader = new AbstractQueryCommand.FunctionReader(compiler.getSelectFunction());
            }
        } else {
            resultReader = new AbstractQueryCommand.EntityReader((JDBCEntityBridge2) compiler.getSelectEntity(), compiler.isSelectDistinct());
        }

        return AbstractQueryCommand.fetchOne(entity, sql, toArray(compiler.getInputParameters()),
                resultReader, (Object[]) args[1], factory, log);
    }

    private static Class[] getParamTypes(Object[] args)
            throws FinderException {
        Class[] parameterTypes;
        // get the parameters
        Object[] parameters = (Object[]) args[1];
        if (parameters == null) {
            parameterTypes = new Class[0];
        } else {
            // get the parameter types
            parameterTypes = new Class[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i] == null) {
                    throw CmpMessages.MESSAGES.parameterIsNull(i);
                }
                parameterTypes[i] = parameters[i].getClass();
            }
        }
        return parameterTypes;
    }

    static QueryParameter[] toArray(List p) {
        QueryParameter[] params = null;
        if (p.size() > 0) {
            params = (QueryParameter[]) p.toArray(new QueryParameter[p.size()]);
        }
        return params;
    }
}
