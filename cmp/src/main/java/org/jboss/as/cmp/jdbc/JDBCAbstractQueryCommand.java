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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;
import javax.ejb.EJBException;
import javax.ejb.FinderException;
import javax.transaction.Synchronization;
import org.jboss.as.cmp.context.CmpEntityBeanContext;
import org.jboss.as.cmp.ejbql.SelectFunction;
import org.jboss.as.cmp.jdbc.bridge.JDBCCMPFieldBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCCMRFieldBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCEntityBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCFieldBridge;
import org.jboss.as.cmp.jdbc.metadata.JDBCLeftJoinMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCQueryMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCRelationMetaData;
import org.jboss.logging.Logger;

/**
 * Abstract superclass of finder commands that return collections.
 * Provides the handleResult() implementation that these all need.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @author <a href="mailto:rickard.oberg@telkel.com">Rickard Oberg</a>
 * @author <a href="mailto:marc.fleury@telkel.com">Marc Fleury</a>
 * @author <a href="mailto:shevlandj@kpi.com.au">Joe Shevland</a>
 * @author <a href="mailto:justin@j-m-f.demon.co.uk">Justin Forder</a>
 * @author <a href="mailto:alex@jboss.org">Alex Loubyansky</a>
 * @version $Revision: 81030 $
 */
public abstract class JDBCAbstractQueryCommand implements JDBCQueryCommand {
    private JDBCQueryMetaData queryMetaData;
    protected Logger log;

    private JDBCStoreManager selectManager;
    private JDBCEntityBridge selectEntity;
    private JDBCCMPFieldBridge selectField;
    private SelectFunction selectFunction;
    private boolean[] eagerLoadMask;
    private String eagerLoadGroup;
    private String sql;
    private int offsetParam;
    private int offsetValue;
    private int limitParam;
    private int limitValue;
    private List parameters = new ArrayList(0);
    private List onFindCMRList = Collections.EMPTY_LIST;
    private QueryCollectionFactory collectionFactory;

    public JDBCAbstractQueryCommand(JDBCStoreManager manager, JDBCQueryMetaData q) {
        this.log = Logger.getLogger(this.getClass().getName() +
                "." +
                manager.getMetaData().getName() +
                "#" +
                q.getMethod().getName());

        queryMetaData = q;
        collectionFactory = q.isLazyResultSetLoading() ?
                new LazyCollectionFactory() :
                (QueryCollectionFactory) new EagerCollectionFactory();

//      setDefaultOffset(q.getOffsetParam());
//      setDefaultLimit(q.getLimitParam());
        setSelectEntity((JDBCEntityBridge) manager.getEntityBridge());
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

    public void setOnFindCMRList(List onFindCMRList) {
        this.onFindCMRList = onFindCMRList;
    }

    public JDBCStoreManager getSelectManager() {
        return selectManager;
    }

    public Collection execute(Method finderMethod, Object[] args, CmpEntityBeanContext ctx, EntityProxyFactory factory) throws FinderException {
        int offset = toInt(args, offsetParam, offsetValue);
        int limit = toInt(args, limitParam, limitValue);
        return execute(sql,
                args,
                offset,
                limit,
                selectEntity,
                selectField,
                selectFunction,
                selectManager,
                eagerLoadMask,
                parameters,
                onFindCMRList,
                queryMetaData,
                factory,
                log);
    }

    protected static int toInt(Object[] params, int paramNumber, int defaultValue) {
        if (paramNumber == 0) {
            return defaultValue;
        }
        Integer arg = (Integer) params[paramNumber - 1];
        return arg.intValue();
    }

    protected Collection execute(String sql,
                                 Object[] args,
                                 int offset,
                                 int limit,
                                 JDBCEntityBridge selectEntity,
                                 JDBCCMPFieldBridge selectField,
                                 SelectFunction selectFunction,
                                 JDBCStoreManager selectManager,
                                 boolean[] eagerLoadMask,
                                 List parameters,
                                 List onFindCMRList,
                                 JDBCQueryMetaData queryMetaData,
                                 EntityProxyFactory factory,
                                 Logger log)
            throws FinderException {
        int count = offset;
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        final JDBCEntityBridge entityBridge = (JDBCEntityBridge) selectManager.getEntityBridge();
        boolean throwRuntimeExceptions = entityBridge.getMetaData().getThrowRuntimeExceptions();

        // if metadata is true, the getConnection is done inside
        // its own try catch block to throw a runtime exception (EJBException)
        if (throwRuntimeExceptions) {
            try {
                con = entityBridge.getDataSource().getConnection();
            } catch (SQLException sqle) {
                javax.ejb.EJBException ejbe = new javax.ejb.EJBException("Could not get a connection; " + sqle);
                ejbe.initCause(sqle);
                throw ejbe;
            }
        }


        try {
            // create the statement
            if (log.isDebugEnabled()) {
                log.debug("Executing SQL: " + sql);
                if (limit != 0 || offset != 0) {
                    log.debug("Query offset=" + offset + ", limit=" + limit);
                }
            }

            // if metadata is false, the getConnection is done inside this try catch block
            if (!throwRuntimeExceptions) {
                con = entityBridge.getDataSource().getConnection();
            }
            ps = con.prepareStatement(sql);

            // Set the fetch size of the statement
            if (entityBridge.getFetchSize() > 0) {
                ps.setFetchSize(entityBridge.getFetchSize());
            }

            // set the parameters
            for (int i = 0; i < parameters.size(); i++) {
                QueryParameter parameter = (QueryParameter) parameters.get(i);
                parameter.set(log, ps, i + 1, args);
            }

            // execute statement
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

            log.error("Find failed", e);
            FinderException fe = new FinderException("Find failed: " + e);
            fe.initCause(e);
            throw fe;
        }

        return collectionFactory.createCollection(con,
                ps,
                rs,
                limit,
                count,
                selectEntity,
                selectField,
                selectFunction,
                selectManager,
                onFindCMRList,
                factory,
                eagerLoadMask);
    }

    protected Logger getLog() {
        return log;
    }

    protected void setSQL(String sql) {
        this.sql = sql;
        if (log.isDebugEnabled()) {
            log.debug("SQL: " + sql);
        }
    }

    protected void setParameterList(List p) {
        for (int i = 0; i < p.size(); i++) {
            if (!(p.get(i) instanceof QueryParameter)) {
                throw new IllegalArgumentException("Element " +
                        i +
                        " of list " +
                        "is not an instance of QueryParameter, but " +
                        p.get(i).getClass().getName());
            }
        }
        parameters = new ArrayList(p);
    }

    protected JDBCEntityBridge getSelectEntity() {
        return selectEntity;
    }

    protected void setSelectEntity(JDBCEntityBridge selectEntity) {
        if (queryMetaData.getMethod().getName().startsWith("find") &&
                this.selectEntity != null && this.selectEntity != selectEntity) {
            throw new RuntimeException("Finder " + queryMetaData.getMethod().getName() +
                    " defined on " + this.selectEntity.getEntityName() +
                    " should return only instances of " + this.selectEntity.getEntityName() +
                    " but the query results in instances of " + selectEntity.getEntityName());
        }

        this.selectField = null;
        this.selectFunction = null;
        this.selectEntity = selectEntity;
        this.selectManager = (JDBCStoreManager) selectEntity.getManager();
    }

    protected JDBCCMPFieldBridge getSelectField() {
        return selectField;
    }

    protected void setSelectField(JDBCCMPFieldBridge selectField) {
        this.selectEntity = null;
        this.selectFunction = null;
        this.selectField = selectField;
        this.selectManager = (JDBCStoreManager) selectField.getManager();
    }

    protected void setSelectFunction(SelectFunction func, JDBCStoreManager manager) {
        this.selectEntity = null;
        this.selectField = null;
        this.selectFunction = func;
        this.selectManager = manager;
    }

    protected void setEagerLoadGroup(String eagerLoadGroup) {
        this.eagerLoadGroup = eagerLoadGroup;
        boolean[] originalMask = selectEntity.getLoadGroupMask(eagerLoadGroup);
        this.eagerLoadMask = new boolean[originalMask.length];
        System.arraycopy(originalMask, 0, eagerLoadMask, 0, eagerLoadMask.length);
    }

    protected String getEagerLoadGroup() {
        return eagerLoadGroup;
    }

    protected boolean[] getEagerLoadMask() {
        return this.eagerLoadMask;
    }

    /**
     * Replaces the parameters in the specific sql with question marks, and
     * initializes the parameter setting code. Parameters are encoded in curly
     * brackets use a zero based index.
     *
     * @param sql the sql statement that is parsed for parameters
     * @return the original sql statement with the parameters replaced with a
     *         question mark
     */
    protected String parseParameters(String sql) {
        StringBuffer sqlBuf = new StringBuffer();
        ArrayList params = new ArrayList();

        // Replace placeholders {0} with ?
        if (sql != null) {
            sql = sql.trim();

            StringTokenizer tokens = new StringTokenizer(sql, "{}", true);
            while (tokens.hasMoreTokens()) {
                String token = tokens.nextToken();
                if (token.equals("{")) {
                    token = tokens.nextToken();
                    if (Character.isDigit(token.charAt(0))) {
                        QueryParameter parameter = new QueryParameter(selectManager, queryMetaData.getMethod(), token);

                        // of if we are here we can assume that we have
                        // a parameter and not a function
                        sqlBuf.append("?");
                        params.add(parameter);

                        if (!tokens.nextToken().equals("}")) {
                            throw new RuntimeException("Invalid parameter - missing closing '}' : " + sql);
                        }
                    } else {
                        // ok we don't have a parameter, we have a function
                        // push the tokens on the buffer and continue
                        sqlBuf.append("{").append(token);
                    }
                } else {
                    // not parameter... just append it
                    sqlBuf.append(token);
                }
            }
        }

        parameters = params;

        return sqlBuf.toString();
    }

    // Static

    public static List<LeftJoinCMRNode> getLeftJoinCMRNodes(JDBCEntityBridge entity, String path, List<JDBCLeftJoinMetaData> leftJoins, Set<String> declaredPaths) {
        if (leftJoins.isEmpty()) {
            return Collections.emptyList();
        }

        final List<LeftJoinCMRNode> leftJoinCMRNodes = new ArrayList<LeftJoinCMRNode>();
        for (JDBCLeftJoinMetaData leftJoin : leftJoins) {
            JDBCCMRFieldBridge cmrField = entity.getCMRFieldByName(leftJoin.getCmrField());
            if (cmrField == null) {
                throw new RuntimeException("cmr-field in left-join was not found: cmr-field=" +
                        leftJoin.getCmrField() + ", entity=" + entity.getEntityName());
            }

            List<LeftJoinCMRNode> subNodes;
            JDBCEntityBridge relatedEntity = cmrField.getRelatedJDBCEntity();
            String childPath = path + '.' + cmrField.getFieldName();
            if (declaredPaths != null) {
                declaredPaths.add(childPath);
            }

            subNodes = getLeftJoinCMRNodes(relatedEntity, childPath, leftJoin.getLeftJoins(), declaredPaths);

            boolean[] mask = relatedEntity.getLoadGroupMask(leftJoin.getEagerLoadGroup());
            LeftJoinCMRNode node = new LeftJoinCMRNode(childPath, cmrField, mask, subNodes);
            leftJoinCMRNodes.add(node);
        }

        return leftJoinCMRNodes;
    }

    public static final void leftJoinCMRNodes(String alias, List<LeftJoinCMRNode> onFindCMRNodes, AliasManager aliasManager, StringBuffer sb) {
        for (LeftJoinCMRNode node : onFindCMRNodes) {
            JDBCCMRFieldBridge cmrField = node.cmrField;
            JDBCEntityBridge relatedEntity = cmrField.getRelatedJDBCEntity();
            String relatedAlias = aliasManager.getAlias(node.path);

            JDBCRelationMetaData relation = cmrField.getMetaData().getRelationMetaData();
            if (relation.isTableMappingStyle()) {
                String relTableAlias = aliasManager.getRelationTableAlias(node.path);
                sb.append(" LEFT OUTER JOIN ")
                        .append(cmrField.getQualifiedTableName())
                        .append(' ')
                        .append(relTableAlias)
                        .append(" ON ");
                SQLUtil.getRelationTableJoinClause(cmrField, alias, relTableAlias, sb);

                sb.append(" LEFT OUTER JOIN ")
                        .append(relatedEntity.getQualifiedTableName())
                        .append(' ')
                        .append(relatedAlias)
                        .append(" ON ");
                SQLUtil.getRelationTableJoinClause(cmrField.getRelatedCMRField(), relatedAlias, relTableAlias, sb);
            } else {
                // foreign key mapping style
                sb.append(" LEFT OUTER JOIN ")
                        .append(relatedEntity.getQualifiedTableName())
                        .append(' ')
                        .append(relatedAlias)
                        .append(" ON ");
                SQLUtil.getJoinClause(cmrField,
                        alias,
                        relatedAlias,
                        sb);
            }

            List<LeftJoinCMRNode> subNodes = node.onFindCMRNodes;
            if (!subNodes.isEmpty()) {
                leftJoinCMRNodes(relatedAlias, subNodes, aliasManager, sb);
            }
        }
    }

    public static final void appendLeftJoinCMRColumnNames(List<LeftJoinCMRNode> onFindCMRNodes,
                                                          AliasManager aliasManager,
                                                          StringBuffer sb) {
        for (LeftJoinCMRNode node : onFindCMRNodes) {
            JDBCCMRFieldBridge cmrField = node.cmrField;
            JDBCEntityBridge relatedEntity = cmrField.getRelatedJDBCEntity();
            String childAlias = aliasManager.getAlias(node.path);

            // primary key fields
            SQLUtil.appendColumnNamesClause(relatedEntity.getPrimaryKeyFields(),
                    childAlias,
                    sb);

            // eager load group
            if (node.eagerLoadMask != null) {
                SQLUtil.appendColumnNamesClause(relatedEntity.getTableFields(),
                        node.eagerLoadMask,
                        childAlias,
                        sb);
            }

            List<LeftJoinCMRNode> subNodes = node.onFindCMRNodes;
            if (!subNodes.isEmpty()) {
                appendLeftJoinCMRColumnNames(subNodes, aliasManager, sb);
            }
        }
    }

    private static int loadOnFindCMRFields(Object pk, List<LeftJoinCMRNode> onFindCMRNodes, ResultSet rs, int index, Logger log) {
        Object[] ref = new Object[1];
        for (LeftJoinCMRNode node : onFindCMRNodes) {
            JDBCCMRFieldBridge cmrField = node.cmrField;
            ReadAheadCache myCache = cmrField.getJDBCStoreManager().getReadAheadCache();
            JDBCEntityBridge relatedEntity = cmrField.getRelatedJDBCEntity();
            ReadAheadCache relatedCache = cmrField.getRelatedManager().getReadAheadCache();

            // load related id
            ref[0] = null;
            index = relatedEntity.loadPrimaryKeyResults(rs, index, ref);
            Object relatedId = ref[0];
            boolean cacheRelatedData = relatedId != null;

            if (pk != null) {
                if (cmrField.getMetaData().getRelatedRole().isMultiplicityOne()) {
                    // cacheRelatedData the value
                    myCache.addPreloadData(pk,
                            cmrField,
                            relatedId == null ? Collections.EMPTY_LIST : Collections.singletonList(relatedId));
                } else {
                    Collection<Object> cachedValue = myCache.getCachedCMRValue(pk, cmrField);
                    if (cachedValue == null) {
                        cachedValue = new ArrayList<Object>();
                        myCache.addPreloadData(pk, cmrField, cachedValue);
                    }

                    if (relatedId != null) {
                        if (cachedValue.contains(relatedId)) {
                            cacheRelatedData = false;
                        } else {
                            cachedValue.add(relatedId);
                        }
                    }
                }
            }

            // load eager load group
            if (node.eagerLoadMask != null) {
                JDBCFieldBridge[] tableFields = relatedEntity.getTableFields();
                for (int fieldInd = 0; fieldInd < tableFields.length; ++fieldInd) {
                    if (node.eagerLoadMask[fieldInd]) {
                        JDBCFieldBridge field = tableFields[fieldInd];
                        ref[0] = null;
                        index = field.loadArgumentResults(rs, index, ref);

                        if (cacheRelatedData) {
                            if (log.isTraceEnabled()) {
                                log.trace("Caching " +
                                        relatedEntity.getEntityName() +
                                        '[' +
                                        relatedId +
                                        "]." +
                                        field.getFieldName() + "=" + ref[0]);
                            }
                            relatedCache.addPreloadData(relatedId, field, ref[0]);
                        }
                    }
                }
            }

            List<LeftJoinCMRNode> subNodes = node.onFindCMRNodes;
            if (!subNodes.isEmpty()) {
                index = loadOnFindCMRFields(relatedId, subNodes, rs, index, log);
            }
        }

        return index;
    }

    public static final class LeftJoinCMRNode {
        public final String path;
        public final JDBCCMRFieldBridge cmrField;
        public final boolean[] eagerLoadMask;
        public final List<LeftJoinCMRNode> onFindCMRNodes;

        public LeftJoinCMRNode(String path, JDBCCMRFieldBridge cmrField, boolean[] eagerLoadMask, List<LeftJoinCMRNode> onFindCMRNodes) {
            this.path = path;
            this.cmrField = cmrField;
            this.eagerLoadMask = eagerLoadMask;
            this.onFindCMRNodes = onFindCMRNodes;
        }

        public boolean equals(Object o) {
            boolean result;
            if (o == this) {
                result = true;
            } else if (o instanceof LeftJoinCMRNode) {
                LeftJoinCMRNode other = (LeftJoinCMRNode) o;
                result = cmrField == other.cmrField;
            } else {
                result = false;
            }
            return result;
        }

        public int hashCode() {
            return cmrField == null ? Integer.MIN_VALUE : cmrField.hashCode();
        }

        public String toString() {
            return '[' + cmrField.getFieldName() + ": " + onFindCMRNodes + ']';
        }
    }


    interface QueryCollectionFactory {
        Collection createCollection(Connection con,
                                    PreparedStatement ps,
                                    ResultSet rs,
                                    int limit, int count,
                                    JDBCEntityBridge selectEntity,
                                    JDBCCMPFieldBridge selectField,
                                    SelectFunction selectFunction,
                                    JDBCStoreManager selectManager,
                                    List onFindCMRList,
                                    EntityProxyFactory factory,
                                    boolean[] eagerLoadMask)
                throws FinderException;
    }

    class EagerCollectionFactory
            implements QueryCollectionFactory {
        public Collection createCollection(Connection con,
                                           PreparedStatement ps,
                                           ResultSet rs,
                                           int limit, int count,
                                           JDBCEntityBridge selectEntity,
                                           JDBCCMPFieldBridge selectField,
                                           SelectFunction selectFunction,
                                           JDBCStoreManager selectManager,
                                           List onFindCMRList,
                                           EntityProxyFactory factory,
                                           boolean[] eagerLoadMask)
                throws FinderException {
            try {
                List results = new ArrayList();

                if (selectEntity != null) {
                    ReadAheadCache selectReadAheadCache = selectManager.getReadAheadCache();
                    List ids = new ArrayList();

                    boolean loadOnFindCmr = !onFindCMRList.isEmpty();
                    Object[] ref = new Object[1];
                    Object prevPk = null;

                    while ((limit == 0 || count-- > 0) && rs.next()) {
                        int index = 1;

                        // get the pk
                        index = selectEntity.loadPrimaryKeyResults(rs, index, ref);
                        Object pk = ref[0];

                        boolean addPk = (loadOnFindCmr ? !pk.equals(prevPk) : true);
                        if (addPk) {
                            ids.add(pk);
                            results.add(pk != null ? factory.getEntityObject(pk) : null);
                            prevPk = pk;
                        }

                        // read the preload fields
                        if (eagerLoadMask != null) {
                            JDBCFieldBridge[] tableFields = selectEntity.getTableFields();
                            for (int i = 0; i < eagerLoadMask.length; i++) {
                                if (eagerLoadMask[i]) {
                                    JDBCFieldBridge field = tableFields[i];
                                    ref[0] = null;

                                    // read the value and store it in the readahead cache
                                    index = field.loadArgumentResults(rs, index, ref);

                                    if (addPk) {
                                        selectReadAheadCache.addPreloadData(pk, field, ref[0]);
                                    }
                                }
                            }

                            if (!onFindCMRList.isEmpty()) {
                                index = loadOnFindCMRFields(pk, onFindCMRList, rs, index, log);
                            }
                        }
                    }

                    // add the results list to the cache
                    selectReadAheadCache.addFinderResults(ids, queryMetaData.getReadAhead());
                } else if (selectField != null) {
                    // load the field
                    Object[] valueRef = new Object[1];
                    while ((limit == 0 || count-- > 0) && rs.next()) {
                        valueRef[0] = null;
                        selectField.loadArgumentResults(rs, 1, valueRef);
                        results.add(valueRef[0]);
                    }
                } else {
                    while (rs.next()) {
                        results.add(selectFunction.readResult(rs));
                    }
                }

                if (log.isDebugEnabled() && limit != 0 && count == 0) {
                    log.debug("Query result was limited to " + limit + " row(s)");
                }

                return results;
            } catch (Exception e) {
                log.error("Find failed", e);
                throw new FinderException("Find failed: " + e);
            } finally {
                JDBCUtil.safeClose(rs);
                JDBCUtil.safeClose(ps);
                JDBCUtil.safeClose(con);
            }
        }

    }

    class LazyCollectionFactory implements QueryCollectionFactory {
        public Collection createCollection(Connection con,
                                           PreparedStatement ps,
                                           ResultSet rs,
                                           int limit, int count,
                                           JDBCEntityBridge selectEntity,
                                           JDBCCMPFieldBridge selectField,
                                           SelectFunction selectFunction,
                                           JDBCStoreManager selectManager,
                                           List onFindCMRList,
                                           EntityProxyFactory factory,
                                           boolean[] eagerLoadMask)
                throws FinderException {
            return new LazyCollection(con,
                    ps,
                    rs,
                    limit,
                    count,
                    selectEntity,
                    selectField,
                    selectFunction,
                    selectManager,
                    factory,
                    eagerLoadMask);
        }

        private class LazyCollection extends AbstractCollection {
            private final Connection con;
            private final PreparedStatement ps;
            private final ResultSet rs;
            private final int limit;
            private int count;
            private final JDBCEntityBridge selectEntity;
            private final JDBCCMPFieldBridge selectField;
            private final SelectFunction selectFunction;
            private final JDBCStoreManager selectManager;
            private final EntityProxyFactory factory;
            private final boolean[] eagerLoadMask;

            private Object prevPk;
            private Object curPk;
            private Object currentResult;

            Object[] ref = new Object[1];

            boolean loadOnFindCmr;

            private List results = null;
            private Iterator firstIterator;
            private int size;
            private boolean resourcesClosed;

            public LazyCollection(final Connection con,
                                  final PreparedStatement ps,
                                  final ResultSet rs,
                                  int limit,
                                  int count,
                                  JDBCEntityBridge selectEntity,
                                  JDBCCMPFieldBridge selectField,
                                  SelectFunction selectFunction,
                                  JDBCStoreManager selectManager,
                                  EntityProxyFactory factory,
                                  boolean[] eagerLoadMask) {
                this.con = con;
                this.ps = ps;
                this.rs = rs;
                this.limit = limit;
                this.count = count;
                this.selectEntity = selectEntity;
                this.selectField = selectField;
                this.selectFunction = selectFunction;
                this.selectManager = selectManager;
                this.eagerLoadMask = eagerLoadMask;
                this.factory = factory;
                loadOnFindCmr = !onFindCMRList.isEmpty();

                firstIterator = getFirstIterator();
                if (firstIterator.hasNext()) {
                    try {
                        size = rs.getInt(1);
                    } catch (SQLException e) {
                        throw new EJBException("Failed to read ResultSet.", e);
                    }

                    if (limit > 0 && size > limit) {
                        size = limit;
                    }
                }

                if (size < 1) {
                    firstIterator = null;
                    results = new ArrayList(0);
                    closeResources();
                } else {
                    results = new ArrayList(size);
                    try {
                        selectManager.getComponent().getTransactionManager().getTransaction().registerSynchronization(new Synchronization() {
                            public void beforeCompletion() {
                                closeResources();
                            }

                            public void afterCompletion(int status) {
                                closeResources();
                            }
                        });
                    } catch (Exception e) {
                        throw new EJBException("Failed to obtain current transaction", e);
                    }
                }
            }

            private void closeResources() {
                if (!resourcesClosed) {
                    JDBCUtil.safeClose(rs);
                    JDBCUtil.safeClose(ps);
                    JDBCUtil.safeClose(con);
                    resourcesClosed = true;
                }
            }

            public Iterator iterator() {
                return firstIterator != null ? firstIterator : results.iterator();
            }

            public int size() {
                return firstIterator != null ? size : results.size();
            }

            public boolean add(Object o) {
                if (firstIterator == null) {
                    return results.add(o);
                }
                throw new IllegalStateException("Can't modify collection while the first iterator is not exhausted.");
            }

            public boolean remove(Object o) {
                if (firstIterator == null) {
                    return results.remove(o);
                }
                throw new IllegalStateException("Can't modify collection while the first iterator is not exhausted.");
            }

            private boolean hasNextResult() {
                try {
                    boolean has = (limit == 0 || count-- > 0) && rs.next();
                    if (!has) {
                        if (log.isTraceEnabled()) {
                            log.trace("first iterator exhausted!");
                        }
                        firstIterator = null;
                        closeResources();
                    }
                    return has;
                } catch (Exception e) {
                    log.error("Failed to read ResultSet.", e);
                    throw new EJBException("Failed to read ResultSet: " + e.getMessage());
                }
            }

            private Object readNext() {
                try {
                    if (selectEntity != null) {
                        ReadAheadCache selectReadAheadCache = selectManager.getReadAheadCache();

                        // first one is size
                        int index = 2;

                        // get the pk
                        index = selectEntity.loadPrimaryKeyResults(rs, index, ref);
                        curPk = ref[0];

                        boolean addPk = (loadOnFindCmr ? !curPk.equals(prevPk) : true);
                        if (addPk) {
                            prevPk = curPk;
                            currentResult = curPk != null ? factory.getEntityObject(curPk) : null;
                        }

                        // read the preload fields
                        if (eagerLoadMask != null) {
                            JDBCFieldBridge[] tableFields = selectEntity.getTableFields();
                            for (int i = 0; i < eagerLoadMask.length; i++) {
                                if (eagerLoadMask[i]) {
                                    JDBCFieldBridge field = tableFields[i];
                                    ref[0] = null;

                                    // read the value and store it in the readahead cache
                                    index = field.loadArgumentResults(rs, index, ref);

                                    if (addPk) {
                                        selectReadAheadCache.addPreloadData(curPk, field, ref[0]);
                                    }
                                }
                            }

                            if (!onFindCMRList.isEmpty()) {
                                index = loadOnFindCMRFields(curPk, onFindCMRList, rs, index, log);
                            }
                        }
                    } else if (selectField != null) {
                        // load the field
                        selectField.loadArgumentResults(rs, 2, ref);
                        currentResult = ref[0];
                    } else {
                        currentResult = selectFunction.readResult(rs);
                    }

                    if (log.isTraceEnabled() && limit != 0 && count == 0) {
                        log.trace("Query result was limited to " + limit + " row(s)");
                    }

                    return currentResult;
                } catch (Exception e) {
                    log.error("Failed to read ResultSet", e);
                    throw new EJBException("Failed to read ResultSet: " + e.getMessage());
                }
            }

            private Iterator getFirstIterator() {
                return new Iterator() {
                    private boolean hasNext;
                    private Object cursor;

                    public boolean hasNext() {
                        return hasNext ? hasNext : (hasNext = hasNextResult());
                    }

                    public Object next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }
                        hasNext = false;

                        cursor = readNext();
                        results.add(cursor);

                        return cursor;
                    }

                    public void remove() {
                        --size;
                        results.remove(cursor);
                    }
                };
            }
        }
    }
}
