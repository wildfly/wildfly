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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ejb.FinderException;
import javax.ejb.ObjectNotFoundException;
import org.jboss.as.cmp.CmpMessages;
import static org.jboss.as.cmp.CmpMessages.MESSAGES;
import org.jboss.as.cmp.ejbql.SelectFunction;
import org.jboss.as.cmp.jdbc.JDBCQueryCommand;
import org.jboss.as.cmp.jdbc.JDBCUtil;
import org.jboss.as.cmp.jdbc.QueryParameter;
import org.jboss.as.cmp.jdbc2.bridge.JDBCCMPFieldBridge2;
import org.jboss.as.cmp.jdbc2.bridge.JDBCEntityBridge2;
import org.jboss.as.cmp.jdbc2.schema.Schema;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @version <tt>$Revision: 81030 $</tt>
 */
public abstract class AbstractQueryCommand implements QueryCommand {
    static final CollectionFactory COLLECTION_FACTORY = new CollectionFactory() {
        public Collection newCollection() {
            return new ArrayList();
        }
    };

    static final CollectionFactory SET_FACTORY = new CollectionFactory() {
        public Collection newCollection() {
            return new HashSet();
        }
    };

    protected String sql;
    protected Logger log;
    protected JDBCEntityBridge2 entity;
    protected QueryParameter[] params = null;
    private CollectionFactory collectionFactory;
    private CollectionStrategy collectionStrategy;
    private ResultReader resultReader;
    private int offsetParam;
    private int offsetValue;
    private int limitParam;
    private int limitValue;

    // Protected

    protected void setResultType(Class clazz) {
        if (Set.class.isAssignableFrom(clazz)) {
            collectionFactory = SET_FACTORY;
        } else if (Collection.class.isAssignableFrom(clazz)) {
            collectionFactory = COLLECTION_FACTORY;
        }
        initCollectionStrategy();
    }

    protected void setFieldReader(JDBCCMPFieldBridge2 field) {
        this.resultReader = new FieldReader(field);
        initCollectionStrategy();
    }

    protected void setFunctionReader(SelectFunction func) {
        this.resultReader = new FunctionReader(func);
        initCollectionStrategy();
    }

    protected void setEntityReader(JDBCEntityBridge2 entity, boolean searchableOnly) {
        this.entity = entity;
        this.resultReader = new EntityReader(entity, searchableOnly);
        initCollectionStrategy();
    }

    private void initCollectionStrategy() {
        if (collectionFactory != null && resultReader != null) {
            collectionStrategy = new EagerCollectionStrategy(collectionFactory, resultReader, log);
        }
    }

    // QueryCommand implementation

    public JDBCStoreManager2 getStoreManager() {
        return (JDBCStoreManager2) entity.getManager();
    }

    public Collection fetchCollection(Schema schema, Object[] args, final JDBCQueryCommand.EntityProxyFactory factory)
            throws FinderException {
        int offset = toInt(args, offsetParam, offsetValue);
        int limit = toInt(args, limitParam, limitValue);
        return fetchCollection(entity, sql, params, offset, limit, collectionStrategy, schema, args, factory, log);
    }

    public Object fetchOne(Schema schema, Object[] args, final JDBCQueryCommand.EntityProxyFactory factory) throws FinderException {
        schema.flush();
        return executeFetchOne(args, factory);
    }

    public void setOffsetValue(int offsetValue) {
        this.offsetValue = offsetValue;
    }

    public void setLimitValue(int limitValue) {
        this.limitValue = limitValue;
    }

    public void setOffsetParam(int offsetParam) {
        this.offsetParam = offsetParam;
    }

    public void setLimitParam(int limitParam) {
        this.limitParam = limitParam;
    }

    // Protected

    protected static int toInt(Object[] params, int paramNumber, int defaultValue) {
        if (paramNumber == 0) {
            return defaultValue;
        }
        Integer arg = (Integer) params[paramNumber - 1];
        return arg.intValue();
    }

    protected Object executeFetchOne(Object[] args, final JDBCQueryCommand.EntityProxyFactory factory) throws FinderException {
        return fetchOne(entity, sql, params, resultReader, args, factory, log);
    }

    static Collection fetchCollection(JDBCEntityBridge2 entity,
                                      String sql,
                                      QueryParameter[] params,
                                      int offset,
                                      int limit,
                                      CollectionStrategy collectionStrategy,
                                      Schema schema,
                                      Object[] args,
                                      final JDBCQueryCommand.EntityProxyFactory factory,
                                      Logger log)
            throws FinderException {
        schema.flush();

        int count = offset;
        Collection result;

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        boolean throwRuntimeExceptions = entity.getMetaData().getThrowRuntimeExceptions();

        // if metadata is true, the getConnection is done inside
        // its own try catch block to throw a runtime exception (EJBException)
        if (throwRuntimeExceptions) {
            try {
                con = entity.getDataSource().getConnection();
            } catch (SQLException sqle) {
                javax.ejb.EJBException ejbe = new javax.ejb.EJBException("Could not get a connection; " + sqle);
                ejbe.initCause(sqle);
                throw ejbe;
            }
        }
        try {
            if (log.isDebugEnabled()) {
                log.debug("executing: " + sql);
            }

            // if metadata is false, the getConnection is done inside this try catch block
            if (!throwRuntimeExceptions) {
                con = entity.getDataSource().getConnection();
            }
            ps = con.prepareStatement(sql);

            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    params[i].set(log, ps, i + 1, args);
                }
            }

            rs = ps.executeQuery();

            // skip 'offset' results
            while (count > 0 && rs.next()) {
                count--;
            }

            count = limit;
        } catch (Exception e) {
            JDBCUtil.safeClose(rs);
            JDBCUtil.safeClose(ps);
            JDBCUtil.safeClose(con);

            throw CmpMessages.MESSAGES.finderFailed(e);
        }

        result = collectionStrategy.readResultSet(con, ps, rs, limit, count, factory);

        return result;
    }

    static Object fetchOne(JDBCEntityBridge2 entity,
                           String sql,
                           QueryParameter[] params,
                           ResultReader resultReader,
                           Object[] args,
                           final JDBCQueryCommand.EntityProxyFactory factory,
                           Logger log)
            throws FinderException {
        Object pk;
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        boolean throwRuntimeExceptions = entity.getMetaData().getThrowRuntimeExceptions();

        // if metadata is true, the getConnection is done inside
        // its own try catch block to throw a runtime exception (EJBException)
        if (throwRuntimeExceptions) {
            try {
                con = entity.getDataSource().getConnection();
            } catch (SQLException sqle) {
                javax.ejb.EJBException ejbe = new javax.ejb.EJBException("Could not get a connection; " + sqle);
                //ejbe.initCause(sqle); only for JBoss 4 and +
                throw ejbe;
            }
        }
        try {
            if (log.isDebugEnabled()) {
                log.debug("executing: " + sql);
            }

            // if metadata is false, the getConnection is done inside this try catch block
            if (!throwRuntimeExceptions) {
                con = entity.getDataSource().getConnection();
            }
            ps = con.prepareStatement(sql);

            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    params[i].set(log, ps, i + 1, args);
                }
            }

            rs = ps.executeQuery();
            if (rs.next()) {
                pk = resultReader.readRow(rs, factory);
                if (rs.next()) {
                    List list = new ArrayList();
                    list.add(pk);
                    list.add(resultReader.readRow(rs, factory));
                    while (rs.next()) {
                        list.add(resultReader.readRow(rs, factory));
                    }
                    throw MESSAGES.moreThanOneInstanceForSingleValueFinder(list);
                }
            } else {
                throw new ObjectNotFoundException();
            }
        } catch (FinderException e) {
            throw e;
        } catch (Exception e) {
            FinderException fe = new FinderException(e.getMessage());
            fe.initCause(e);
            throw fe;
        } finally {
            JDBCUtil.safeClose(rs);
            JDBCUtil.safeClose(ps);
            JDBCUtil.safeClose(con);
        }

        return pk;
    }

    protected void setParameters(List p) {
        if (p.size() > 0) {
            params = new QueryParameter[p.size()];
            for (int i = 0; i < p.size(); i++) {
                Object pi = p.get(i);
                if (!(pi instanceof QueryParameter)) {
                    throw CmpMessages.MESSAGES.elementNotQueryParam(i, p.get(i).getClass().getName());
                }
                params[i] = (QueryParameter) pi;
            }
        }
    }

    // Inner

    static interface CollectionFactory {
        Collection newCollection();
    }

    static interface ResultReader {
        Object readRow(ResultSet rs, JDBCQueryCommand.EntityProxyFactory factory) throws SQLException;
    }

    static class EntityReader implements ResultReader {
        private final JDBCEntityBridge2 entity;
        private final boolean searchableOnly;

        public EntityReader(JDBCEntityBridge2 entity, boolean searchableOnly) {
            this.entity = entity;
            this.searchableOnly = searchableOnly;
        }

        public Object readRow(ResultSet rs, JDBCQueryCommand.EntityProxyFactory factory) {
            final Object pk = entity.getTable().loadRow(rs, searchableOnly);
            return pk == null ? null : factory.getEntityObject(pk);
        }
    }

    ;

    static class FieldReader implements ResultReader {
        private final JDBCCMPFieldBridge2 field;

        public FieldReader(JDBCCMPFieldBridge2 field) {
            this.field = field;
        }

        public Object readRow(ResultSet rs, JDBCQueryCommand.EntityProxyFactory factory) throws SQLException {
            return field.loadArgumentResults(rs, 1);
        }
    }

    static class FunctionReader implements ResultReader {
        private final SelectFunction function;

        public FunctionReader(SelectFunction function) {
            this.function = function;
        }

        public Object readRow(ResultSet rs, JDBCQueryCommand.EntityProxyFactory factory) throws SQLException {
            return function.readResult(rs);
        }
    }

    interface CollectionStrategy {
        Collection readResultSet(Connection con, PreparedStatement ps, ResultSet rs, int limit, int count, JDBCQueryCommand.EntityProxyFactory factory)
                throws FinderException;
    }

    static class EagerCollectionStrategy
            implements CollectionStrategy {
        private final CollectionFactory collectionFactory;
        private final ResultReader resultReader;
        private final Logger log;

        public EagerCollectionStrategy(CollectionFactory collectionFactory,
                                       ResultReader resultReader, Logger log) {
            this.collectionFactory = collectionFactory;
            this.resultReader = resultReader;
            this.log = log;
        }

        public Collection readResultSet(Connection con,
                                        PreparedStatement ps,
                                        ResultSet rs,
                                        int limit, int count, JDBCQueryCommand.EntityProxyFactory factory)
                throws FinderException {
            Collection result;
            try {
                if ((limit == 0 || count-- > 0) && rs.next()) {
                    result = collectionFactory.newCollection();
                    Object instance = resultReader.readRow(rs, factory);
                    result.add(instance);
                    while ((limit == 0 || count-- > 0) && rs.next()) {
                        instance = resultReader.readRow(rs, factory);
                        result.add(instance);
                    }
                } else {
                    result = Collections.EMPTY_SET;
                }
            } catch (Exception e) {
                throw MESSAGES.finderFailed(e);
            } finally {
                JDBCUtil.safeClose(rs);
                JDBCUtil.safeClose(ps);
                JDBCUtil.safeClose(con);
            }
            return result;
        }
    }
}
