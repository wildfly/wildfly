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

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jboss.as.cmp.bridge.CMPFieldBridge;
import org.jboss.as.cmp.ejbql.ASTAbs;
import org.jboss.as.cmp.ejbql.ASTAbstractSchema;
import org.jboss.as.cmp.ejbql.ASTAvg;
import org.jboss.as.cmp.ejbql.ASTBooleanLiteral;
import org.jboss.as.cmp.ejbql.ASTCollectionMemberDeclaration;
import org.jboss.as.cmp.ejbql.ASTConcat;
import org.jboss.as.cmp.ejbql.ASTCount;
import org.jboss.as.cmp.ejbql.ASTEJBQL;
import org.jboss.as.cmp.ejbql.ASTEntityComparison;
import org.jboss.as.cmp.ejbql.ASTExactNumericLiteral;
import org.jboss.as.cmp.ejbql.ASTFrom;
import org.jboss.as.cmp.ejbql.ASTIdentifier;
import org.jboss.as.cmp.ejbql.ASTIsEmpty;
import org.jboss.as.cmp.ejbql.ASTLCase;
import org.jboss.as.cmp.ejbql.ASTLength;
import org.jboss.as.cmp.ejbql.ASTLimitOffset;
import org.jboss.as.cmp.ejbql.ASTLocate;
import org.jboss.as.cmp.ejbql.ASTMax;
import org.jboss.as.cmp.ejbql.ASTMemberOf;
import org.jboss.as.cmp.ejbql.ASTMin;
import org.jboss.as.cmp.ejbql.ASTMod;
import org.jboss.as.cmp.ejbql.ASTNullComparison;
import org.jboss.as.cmp.ejbql.ASTOrderBy;
import org.jboss.as.cmp.ejbql.ASTParameter;
import org.jboss.as.cmp.ejbql.ASTPath;
import org.jboss.as.cmp.ejbql.ASTRangeVariableDeclaration;
import org.jboss.as.cmp.ejbql.ASTSelect;
import org.jboss.as.cmp.ejbql.ASTSqrt;
import org.jboss.as.cmp.ejbql.ASTSubstring;
import org.jboss.as.cmp.ejbql.ASTSum;
import org.jboss.as.cmp.ejbql.ASTUCase;
import org.jboss.as.cmp.ejbql.ASTValueClassComparison;
import org.jboss.as.cmp.ejbql.ASTWhere;
import org.jboss.as.cmp.ejbql.ASTWhereConditionalTerm;
import org.jboss.as.cmp.ejbql.BasicVisitor;
import org.jboss.as.cmp.ejbql.Catalog;
import org.jboss.as.cmp.ejbql.EJBQLParser;
import org.jboss.as.cmp.ejbql.EJBQLTypes;
import org.jboss.as.cmp.ejbql.JBossQLParser;
import org.jboss.as.cmp.ejbql.Node;
import org.jboss.as.cmp.ejbql.SelectFunction;
import org.jboss.as.cmp.ejbql.SimpleNode;
import org.jboss.as.cmp.jdbc.bridge.JDBCAbstractEntityBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCCMPFieldBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCCMRFieldBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCEntityBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCFieldBridge;
import org.jboss.as.cmp.jdbc.metadata.JDBCFunctionMappingMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCQueryMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCReadAheadMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCTypeMappingMetaData;

/**
 * Compiles EJB-QL and JBossQL into SQL.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @author <a href="mailto:alex@jboss.org">Alex Loubyansky</a>
 * @version $Revision: 102957 $
 */
public final class JDBCEJBQLCompiler extends BasicVisitor implements QLCompiler {
    // input objects
    private final Catalog catalog;
    private Class returnType;
    private Class[] parameterTypes;
    private JDBCReadAheadMetaData readAhead;
    private boolean lazyResultSetLoading;

    // alias info
    private AliasManager aliasManager;

    // join info
    private Set declaredPaths = new HashSet();
    private Set ctermJoinPaths = new HashSet();
    private Set allJoinPaths = new HashSet();
    private Map ctermCollectionMemberJoinPaths = new HashMap();
    private Map allCollectionMemberJoinPaths = new HashMap();
    private Map ctermLeftJoinPaths = new HashMap();
    private Map allLeftJoinPaths = new HashMap();

    // mapping metadata
    private JDBCTypeMappingMetaData typeMapping;
    private JDBCTypeFactory typeFactory;
    private boolean subquerySupported;

    // output objects
    private boolean forceDistinct;
    private String sql;
    private int offsetParam;
    private int offsetValue;
    private int limitParam;
    private int limitValue;
    private JDBCStoreManager selectManager;
    private Object selectObject;
    private List inputParameters = new ArrayList();
    private JDBCType functionJDBCType;

    /**
     * deep read ahead for cmrs
     */
    private List leftJoinCMRList = new ArrayList();
    private StringBuffer onFindCMRJoin;

    private boolean countCompositePk;
    private String selectAlias;
    private boolean selectDistinct;

    public JDBCEJBQLCompiler(Catalog catalog) {
        this.catalog = catalog;
    }

    public void compileEJBQL(String ejbql,
                             Class returnType,
                             Class[] parameterTypes,
                             JDBCQueryMetaData metadata) throws Exception {
        // reset all state variables
        reset();

        // set input arguments
        this.returnType = returnType;
        this.parameterTypes = parameterTypes;
        this.readAhead = metadata.getReadAhead();
        this.lazyResultSetLoading = metadata.isLazyResultSetLoading();

        // get the parser
        EJBQLParser parser = new EJBQLParser(new StringReader(SQLUtil.EMPTY_STRING));

        try {
            // parse the ejbql into an abstract syntax tree
            ASTEJBQL ejbqlNode;
            ejbqlNode = parser.parse(catalog, parameterTypes, ejbql);

            // translate to sql
            sql = ejbqlNode.jjtAccept(this, new StringBuffer()).toString();
        } catch (Exception e) {
            // if there is a problem reset the state before exiting
            reset();
            throw e;
        } catch (Error e) {
            // lame javacc lexer throws Errors
            reset();
            throw e;
        }
    }

    public void compileJBossQL(String ejbql,
                               Class returnType,
                               Class[] parameterTypes,
                               JDBCQueryMetaData metadata)
            throws Exception {
        // reset all state variables
        reset();

        // set input arguments
        this.returnType = returnType;
        this.parameterTypes = parameterTypes;
        this.readAhead = metadata.getReadAhead();
        this.lazyResultSetLoading = metadata.isLazyResultSetLoading();

        // get the parser
        JBossQLParser parser = new JBossQLParser(new StringReader(SQLUtil.EMPTY_STRING));

        try {
            // parse the ejbql into an abstract syntax tree
            ASTEJBQL ejbqlNode;
            ejbqlNode = parser.parse(catalog, parameterTypes, ejbql);

            // translate to sql
            sql = ejbqlNode.jjtAccept(this, new StringBuffer()).toString();
        } catch (Exception e) {
            // if there is a problem reset the state before exiting
            reset();
            throw e;
        } catch (Error e) {
            // lame javacc lexer throws Errors
            reset();
            throw e;
        }
    }

    private void reset() {
        returnType = null;
        parameterTypes = null;
        readAhead = null;
        inputParameters.clear();
        declaredPaths.clear();
        clearPerTermJoinPaths();
        allJoinPaths.clear();
        allCollectionMemberJoinPaths.clear();
        allLeftJoinPaths.clear();
        selectObject = null;
        selectManager = null;
        typeFactory = null;
        typeMapping = null;
        aliasManager = null;
        subquerySupported = true;
        forceDistinct = false;
        limitParam = 0;
        limitValue = 0;
        offsetParam = 0;
        offsetValue = 0;
        leftJoinCMRList.clear();
        onFindCMRJoin = null;
        countCompositePk = false;
        selectAlias = null;
        selectDistinct = false;
        functionJDBCType = null;
    }

    public String getSQL() {
        return sql;
    }

    public int getOffsetValue() {
        return offsetValue;
    }

    public int getOffsetParam() {
        return offsetParam;
    }

    public int getLimitValue() {
        return limitValue;
    }

    public int getLimitParam() {
        return limitParam;
    }

    public boolean isSelectEntity() {
        return selectObject instanceof JDBCEntityBridge;
    }

    public JDBCAbstractEntityBridge getSelectEntity() {
        return (JDBCAbstractEntityBridge) selectObject;
    }

    public boolean isSelectField() {
        return selectObject instanceof JDBCCMPFieldBridge;
    }

    public JDBCFieldBridge getSelectField() {
        return (JDBCCMPFieldBridge) selectObject;
    }

    public SelectFunction getSelectFunction() {
        return (SelectFunction) selectObject;
    }

    public JDBCStoreManager getStoreManager() {
        return selectManager;
    }

    public List getInputParameters() {
        return inputParameters;
    }

    public List getLeftJoinCMRList() {
        return leftJoinCMRList;
    }

    public boolean isSelectDistinct() {
        return selectDistinct;
    }

    public Object visit(SimpleNode node, Object data) {
        throw new RuntimeException("Internal error: Found unknown node type in " +
                "EJB-QL abstract syntax tree: node=" + node);
    }

    private void setTypeFactory(JDBCTypeFactory typeFactory) {
        this.typeFactory = typeFactory;
        this.typeMapping = typeFactory.getTypeMapping();
        aliasManager = new AliasManager(typeMapping.getAliasHeaderPrefix(),
                typeMapping.getAliasHeaderSuffix(),
                typeMapping.getAliasMaxLength());
        subquerySupported = typeMapping.isSubquerySupported();
    }

    private Class getParameterType(int index) {
        int zeroBasedIndex = index - 1;
        Class[] params = parameterTypes;
        if (zeroBasedIndex < params.length) {
            return params[zeroBasedIndex];
        }
        return null;
    }

    // verify that parameter is the same type as the entity
    private void verifyParameterEntityType(int number,
                                           JDBCEntityBridge entity) {
        Class parameterType = getParameterType(number);
        Class remoteClass = entity.getMetaData().getRemoteClass();
        Class localClass = entity.getMetaData().getLocalClass();
        if ((
                localClass == null ||
                        !localClass.isAssignableFrom(parameterType)
        ) &&
                (
                        remoteClass == null ||
                                !remoteClass.isAssignableFrom(parameterType)
                )) {

            throw new IllegalStateException("Only like types can be " +
                    "compared: from entity=" +
                    entity.getEntityName() +
                    " to parameter type=" + parameterType);
        }
    }

    private void compareEntity(boolean not,
                               Node fromNode,
                               Node toNode,
                               StringBuffer buf) {
        buf.append('(');
        if (not) {
            buf.append(SQLUtil.NOT).append('(');
        }

        String fromAlias;
        JDBCEntityBridge fromEntity;
        ASTPath fromPath = (ASTPath) fromNode;
        addJoinPath(fromPath);
        fromAlias = aliasManager.getAlias(fromPath.getPath());
        fromEntity = (JDBCEntityBridge) fromPath.getEntity();

        if (toNode instanceof ASTParameter) {
            ASTParameter toParam = (ASTParameter) toNode;

            // can only compare like kind entities
            verifyParameterEntityType(toParam.number, fromEntity);

            inputParameters.addAll(QueryParameter.createParameters(toParam.number - 1, fromEntity));

            SQLUtil.getWhereClause(fromEntity.getPrimaryKeyFields(), fromAlias, buf);
        } else {
            String toAlias;
            JDBCEntityBridge toEntity;
            ASTPath toPath = (ASTPath) toNode;
            addJoinPath(toPath);
            toAlias = aliasManager.getAlias(toPath.getPath());
            toEntity = (JDBCEntityBridge) toPath.getEntity();

            // can only compare like kind entities
            if (!fromEntity.equals(toEntity)) {
                throw new IllegalStateException("Only like types can be " +
                        "compared: from entity=" +
                        fromEntity.getEntityName() +
                        " to entity=" + toEntity.getEntityName());
            }

            SQLUtil.getSelfCompareWhereClause(fromEntity.getPrimaryKeyFields(), fromAlias, toAlias, buf);
        }

        if (not) {
            buf.append(')');
        }
        buf.append(')');
    }

    private void existsClause(ASTPath path, StringBuffer buf, boolean not) {
        if (!path.isCMRField()) {
            throw new IllegalArgumentException("path must be a cmr field");
        }

        JDBCCMRFieldBridge cmrField = (JDBCCMRFieldBridge) path.getCMRField();
        String pathStr = path.getPath(path.size() - 2);
        String parentAlias = aliasManager.getAlias(pathStr);

        // if exists is not supported we use a left join and is null
        if (!subquerySupported) {
            // add the path to the list of paths to left join
            addLeftJoinPath(pathStr, path);
            forceDistinct = true;

            addJoinPath(path);

            if (cmrField.getRelationMetaData().isForeignKeyMappingStyle()) {
                JDBCEntityBridge childEntity = (JDBCEntityBridge) cmrField.getRelatedEntity();
                String childAlias = aliasManager.getAlias(path.getPath());
                SQLUtil.getIsNullClause(!not, childEntity.getPrimaryKeyFields(), childAlias, buf);
            } else {
                String relationTableAlias = aliasManager.getRelationTableAlias(path.getPath());
                SQLUtil.getIsNullClause(!not, cmrField.getTableKeyFields(), relationTableAlias, buf);
            }
            return;
        }

        if (not) {
            buf.append(SQLUtil.NOT);
        }
        buf.append(SQLUtil.EXISTS).append('(');

        if (cmrField.getRelationMetaData().isForeignKeyMappingStyle()) {
            JDBCEntityBridge childEntity = (JDBCEntityBridge) cmrField.getRelatedEntity();
            String childAlias = aliasManager.getAlias(path.getPath());

            buf.append(SQLUtil.SELECT);

            SQLUtil.getColumnNamesClause(childEntity.getPrimaryKeyFields(), childAlias, buf)
                    .append(SQLUtil.FROM)
                    .append(childEntity.getQualifiedTableName()).append(' ').append(childAlias)
                    .append(SQLUtil.WHERE);
            SQLUtil.getJoinClause(cmrField, parentAlias, childAlias, buf);
        } else {
            String relationTableAlias = aliasManager.getRelationTableAlias(path.getPath());
            buf.append(SQLUtil.SELECT);
            SQLUtil.getColumnNamesClause(cmrField.getTableKeyFields(), relationTableAlias, buf)
                    .append(SQLUtil.FROM)
                    .append(cmrField.getQualifiedTableName())
                    .append(' ')
                    .append(relationTableAlias)
                    .append(SQLUtil.WHERE);
            SQLUtil.getRelationTableJoinClause(cmrField, parentAlias, relationTableAlias, buf);
        }

        buf.append(')');
    }

    public Object visit(ASTEJBQL node, Object data) {
        Node selectNode = node.jjtGetChild(0);
        Node fromNode = node.jjtGetChild(1);
        Node whereNode = null;
        Node orderByNode = null;
        Node limitNode = null;

        for (int childNode = 2; childNode < node.jjtGetNumChildren(); childNode++) {
            Node temp = node.jjtGetChild(childNode);
            if (temp instanceof ASTWhere) {
                whereNode = temp;
            } else if (temp instanceof ASTOrderBy) {
                orderByNode = temp;
            } else if (temp instanceof ASTLimitOffset) {
                limitNode = temp;
            }
        }

        // translate select and add it to the buffer
        StringBuffer select = new StringBuffer();
        selectNode.jjtAccept(this, select);

        // conditional term paths from the where clause are treated separately from those
        // in select, from and order by.
        // TODO come up with a nicer treatment implementation.
        Set selectJoinPaths = new HashSet(ctermJoinPaths);
        Map selectCollectionMemberJoinPaths = new HashMap(ctermCollectionMemberJoinPaths);
        Map selectLeftJoinPaths = new HashMap(ctermLeftJoinPaths);

        // translate where and save results to append later
        StringBuffer where = new StringBuffer();
        if (whereNode != null) {
            whereNode.jjtAccept(this, where);
        }

        // reassign conditional term paths and add paths from order by and from to select paths
        ctermJoinPaths = selectJoinPaths;
        ctermCollectionMemberJoinPaths = selectCollectionMemberJoinPaths;
        ctermLeftJoinPaths = selectLeftJoinPaths;

        // translate order by and save results to append later
        StringBuffer orderBy = new StringBuffer();
        if (orderByNode != null) {
            orderByNode.jjtAccept(this, orderBy);

            // hack alert - this should use the visitor approach
            for (int i = 0; i < orderByNode.jjtGetNumChildren(); i++) {
                Node orderByPath = orderByNode.jjtGetChild(i);
                ASTPath path = (ASTPath) orderByPath.jjtGetChild(0);
                if (!isSelected(path)) {
                    select.append(SQLUtil.COMMA);
                    path.jjtAccept(this, select);
                }
            }
        }

        if (limitNode != null) {
            limitNode.jjtAccept(this, null);
        }

        StringBuffer from = new StringBuffer(50);
        fromNode.jjtAccept(this, from);

        StringBuffer fromThetaJoin = new StringBuffer();
        createThetaJoin(fromThetaJoin);

        if (where.length() != 0 && fromThetaJoin.length() != 0) {
            where.insert(0, '(')
                    .append(')')
                    .append(SQLUtil.AND)
                    .append(fromThetaJoin);
        } else if (fromThetaJoin.length() != 0) {
            where.append(fromThetaJoin.toString());
        }

        selectDistinct = isDistinct(selectNode);

        // select size
        if (lazyResultSetLoading) {
            StringBuffer buf = new StringBuffer(200);
            if (isSelectEntity()) {
                final JDBCFieldBridge[] pkFields = getSelectEntity().getPrimaryKeyFields();
                if (pkFields.length == 1) {
                    buf.append('(').append(SQLUtil.SELECT).append("count(");
                    if (selectDistinct) {
                        buf.append(SQLUtil.DISTINCT);
                    }
                    SQLUtil.getColumnNamesClause(pkFields, selectAlias, buf);
                    buf.append(')').append(SQLUtil.FROM);
                    buf.append(from);
                    if (where.length() > 0) {
                        buf.append(SQLUtil.WHERE).append(where);
                    }
                    buf.append("), ");
                    select.insert(0, buf);
                } else {
                    buf.append('(').append(SQLUtil.SELECT).append("count(*)").append(SQLUtil.FROM)
                            .append('(')
                            .append(SQLUtil.SELECT);

                    if (selectDistinct) {
                        buf.append(SQLUtil.DISTINCT);
                    }

                    SQLUtil.getColumnNamesClause(pkFields, selectAlias, buf);
                    buf.append(SQLUtil.FROM).append(from);

                    if (where.length() > 0) {
                        buf.append(SQLUtil.WHERE).append(where);
                    }
                    buf.append(") t_count), ");
                    select.insert(0, buf);
                }
            } else if (isSelectField()) {
                buf.append('(').append(SQLUtil.SELECT).append("count(");
                if (selectDistinct) {
                    buf.append(SQLUtil.DISTINCT);
                }
                buf.append(select).append(')').append(SQLUtil.FROM);
                buf.append(from);
                if (where.length() > 0) {
                    buf.append(SQLUtil.WHERE).append(where);
                }
                buf.append("), ");
                select.insert(0, buf);
            }
        }

        // distinct
        if (selectDistinct) {
            select.insert(0, SQLUtil.DISTINCT);
        }

        StringBuffer buf = (StringBuffer) data;
        if (selectManager.getMetaData().hasRowLocking()) {
            JDBCFunctionMappingMetaData rowLockingTemplate = typeMapping.getRowLockingTemplate();
            Object[] args = new Object[]{
                    select,
                    from,
                    where.length() == 0 ? null : where,
                    orderBy.length() == 0 ? null : orderBy
            };
            rowLockingTemplate.getFunctionSql(args, buf);
        } else {
            buf.append(SQLUtil.SELECT)
                    .append(select)
                    .append(SQLUtil.FROM)
                    .append(from);

            if (where.length() > 0) {
                buf.append(SQLUtil.WHERE)
                        .append(where);
            }

            if (orderBy.length() != 0) {
                buf.append(SQLUtil.ORDERBY)
                        .append(orderBy);
            }
        }

        // todo: ...
        if (countCompositePk) {
            buf.insert(0, "SELECT COUNT(*) FROM (").append(") t_count");
        }

        return buf;
    }

    public Object visit(ASTFrom node, Object data) {
        StringBuffer buf = (StringBuffer) data;

        node.jjtGetChild(0).jjtAccept(this, buf);
        for (int i = 1; i < node.jjtGetNumChildren(); i++) {
            buf.append(SQLUtil.COMMA);
            node.jjtGetChild(i).jjtAccept(this, buf);
        }

        // add all the additional path tables
        if (!allJoinPaths.isEmpty()) {
            for (Iterator iter = allJoinPaths.iterator(); iter.hasNext(); ) {
                ASTPath path = (ASTPath) iter.next();
                for (int i = 0; i < path.size(); i++) {
                    declareTables(path, i, buf);
                }
            }
        }

        // add all parent paths for collection member join paths
        if (!allCollectionMemberJoinPaths.isEmpty()) {
            for (Iterator iter = allCollectionMemberJoinPaths.values().iterator(); iter.hasNext(); ) {
                ASTPath path = (ASTPath) iter.next();
                // don't declare the last one as the first path was left joined
                for (int i = 0; i < path.size() - 1; i++) {
                    declareTables(path, i, buf);
                }
            }
        }

        // get all the left joined paths
        if (!allLeftJoinPaths.isEmpty()) {
            Set allLeftJoins = new HashSet();
            for (Iterator iter = allLeftJoinPaths.values().iterator(); iter.hasNext(); ) {
                allLeftJoins.addAll((Set) iter.next());
            }

            // add all parent paths for left joins
            for (Iterator iter = allLeftJoins.iterator(); iter.hasNext(); ) {
                ASTPath path = (ASTPath) iter.next();
                // don't declare the last one as the first path was left joined
                for (int i = 0; i < path.size() - 1; i++) {
                    declareTables(path, i, buf);
                }
            }
        }

        return buf;
    }

    private void declareTables(ASTPath path, int i, StringBuffer buf) {
        if (!path.isCMRField(i) || declaredPaths.contains(path.getPath(i))) {
            return;
        }

        JDBCCMRFieldBridge cmrField = (JDBCCMRFieldBridge) path.getCMRField(i);
        JDBCEntityBridge entity = (JDBCEntityBridge) path.getEntity(i);

        buf.append(SQLUtil.COMMA)
                .append(entity.getQualifiedTableName())
                .append(' ')
                .append(aliasManager.getAlias(path.getPath(i)));
        leftJoins(path.getPath(i), buf);

        if (cmrField.getRelationMetaData().isTableMappingStyle()) {
            String relationTableAlias = aliasManager.getRelationTableAlias(path.getPath(i));
            buf.append(SQLUtil.COMMA)
                    .append(cmrField.getQualifiedTableName())
                    .append(' ')
                    .append(relationTableAlias);
        }

        declaredPaths.add(path.getPath(i));
    }

    private void leftJoins(String parentPath, StringBuffer buf) {
        Set paths = (Set) ctermLeftJoinPaths.get(parentPath);
        if (subquerySupported || paths == null) {
            return;
        }

        for (Iterator iter = paths.iterator(); iter.hasNext(); ) {
            ASTPath path = (ASTPath) iter.next();

            JDBCCMRFieldBridge cmrField = (JDBCCMRFieldBridge) path.getCMRField();
            String parentAlias = aliasManager.getAlias(parentPath);

            if (cmrField.getRelationMetaData().isForeignKeyMappingStyle()) {
                JDBCEntityBridge childEntity = (JDBCEntityBridge) cmrField.getRelatedEntity();
                String childAlias = aliasManager.getAlias(path.getPath());

                buf.append(SQLUtil.LEFT_JOIN)
                        .append(childEntity.getQualifiedTableName())
                        .append(' ')
                        .append(childAlias)
                        .append(SQLUtil.ON);
                SQLUtil.getJoinClause(cmrField, parentAlias, childAlias, buf);
            } else {
                String relationTableAlias = aliasManager.getRelationTableAlias(path.getPath());
                buf.append(SQLUtil.LEFT_JOIN)
                        .append(cmrField.getQualifiedTableName())
                        .append(' ')
                        .append(relationTableAlias)
                        .append(SQLUtil.ON);
                SQLUtil.getRelationTableJoinClause(cmrField, parentAlias, relationTableAlias, buf);
            }
        }
    }

    private void createThetaJoin(StringBuffer buf) {
        Set joinedAliases = new HashSet();
        // add all the additional path tables
        if (!ctermJoinPaths.isEmpty()) {
            for (Iterator iter = ctermJoinPaths.iterator(); iter.hasNext(); ) {
                ASTPath path = (ASTPath) iter.next();
                for (int i = 0; i < path.size(); i++) {
                    createThetaJoin(path, i, joinedAliases, buf);
                }
            }
        }

        // add all the collection member path tables
        if (!ctermCollectionMemberJoinPaths.isEmpty()) {
            for (Iterator iter = ctermCollectionMemberJoinPaths.entrySet().iterator(); iter.hasNext(); ) {
                Map.Entry entry = (Map.Entry) iter.next();
                String childAlias = (String) entry.getKey();
                ASTPath path = (ASTPath) entry.getValue();

                // join the member path
                createThetaJoin(path, path.size() - 1, joinedAliases, childAlias, buf);

                // join the member path parents
                for (int i = 0; i < path.size() - 1; i++) {
                    createThetaJoin(path, i, joinedAliases, buf);
                }
            }
        }

        // get all the left joined paths
        if (!ctermLeftJoinPaths.isEmpty()) {
            Set allLeftJoins = new HashSet();
            for (Iterator iter = ctermLeftJoinPaths.values().iterator(); iter.hasNext(); ) {
                allLeftJoins.addAll((Set) iter.next());
            }

            // add all parent paths for left joins
            for (Iterator iter = allLeftJoins.iterator(); iter.hasNext(); ) {
                ASTPath path = (ASTPath) iter.next();
                // don't declare the last one as the first path was left joined
                for (int i = 0; i < path.size() - 1; i++) {
                    createThetaJoin(path, i, joinedAliases, buf);
                }
            }
        }
    }

    private void createThetaJoin(ASTPath path,
                                 int i,
                                 Set joinedAliases,
                                 StringBuffer buf) {
        String childAlias = aliasManager.getAlias(path.getPath(i));
        createThetaJoin(path, i, joinedAliases, childAlias, buf);
    }

    private void createThetaJoin(ASTPath path,
                                 int i,
                                 Set joinedAliases,
                                 String childAlias,
                                 StringBuffer buf) {
        if (!path.isCMRField(i) || joinedAliases.contains(childAlias)) {
            return;
        }

        JDBCCMRFieldBridge cmrField = (JDBCCMRFieldBridge) path.getCMRField(i);
        String parentAlias = aliasManager.getAlias(path.getPath(i - 1));

        if (joinedAliases.size() > 0) {
            buf.append(SQLUtil.AND);
        }

        if (cmrField.getRelationMetaData().isForeignKeyMappingStyle()) {
            SQLUtil.getJoinClause(cmrField, parentAlias, childAlias, buf);
        } else {
            String relationTableAlias = aliasManager.getRelationTableAlias(path.getPath(i));

            // parent to relation table
            SQLUtil.getRelationTableJoinClause(cmrField, parentAlias, relationTableAlias, buf)
                    .append(SQLUtil.AND);
            // child to relation table
            SQLUtil.getRelationTableJoinClause(cmrField.getRelatedCMRField(), childAlias, relationTableAlias, buf);
        }

        joinedAliases.add(childAlias);
    }


    public Object visit(ASTCollectionMemberDeclaration node, Object data) {
        StringBuffer buf = (StringBuffer) data;

        // first arg is a collection valued path
        ASTPath path = (ASTPath) node.jjtGetChild(0);

        // add this path to the list of declared paths
        declaredPaths.add(path.getPath());

        // get the entity at the end of this path
        JDBCEntityBridge entity = (JDBCEntityBridge) path.getEntity();

        // second arg is the identifier
        ASTIdentifier id = (ASTIdentifier) node.jjtGetChild(1);

        // get the alias
        String alias = aliasManager.getAlias(id.identifier);

        // add this path to the list of join paths so parent paths will be joined
        addCollectionMemberJoinPath(alias, path);

        // declare the alias mapping
        aliasManager.addAlias(path.getPath(), alias);

        buf.append(entity.getQualifiedTableName());
        buf.append(' ');
        buf.append(alias);
        leftJoins(path.getPath(), buf);

        if (onFindCMRJoin != null && alias.equals(selectAlias)) {
            buf.append(onFindCMRJoin);
            onFindCMRJoin = null;
        }

        // add the relation-table
        JDBCCMRFieldBridge cmrField = (JDBCCMRFieldBridge) path.getCMRField();
        if (cmrField.getRelationMetaData().isTableMappingStyle()) {
            String relationTableAlias = aliasManager.getRelationTableAlias(path.getPath());
            buf.append(SQLUtil.COMMA)
                    .append(cmrField.getQualifiedTableName())
                    .append(' ')
                    .append(relationTableAlias);
        }

        return buf;
    }

    public Object visit(ASTRangeVariableDeclaration node, Object data) {
        StringBuffer buf = (StringBuffer) data;

        ASTAbstractSchema schema = (ASTAbstractSchema) node.jjtGetChild(0);
        JDBCEntityBridge entity = (JDBCEntityBridge) schema.entity;
        ASTIdentifier id = (ASTIdentifier) node.jjtGetChild(1);

        String alias = aliasManager.getAlias(id.identifier);
        buf.append(entity.getQualifiedTableName())
                .append(' ')
                .append(alias);
        leftJoins(id.identifier, buf);

        if (onFindCMRJoin != null && alias.equals(selectAlias)) {
            buf.append(onFindCMRJoin);
            onFindCMRJoin = null;
        }

        return buf;
    }

    public Object visit(ASTSelect node, Object data) {
        StringBuffer buf = (StringBuffer) data;

        Node child0 = node.jjtGetChild(0);
        ASTPath path;
        if (child0 instanceof ASTPath) {
            path = (ASTPath) child0;

            if (path.isCMPField()) {
                // set the select object
                JDBCCMPFieldBridge selectField = (JDBCCMPFieldBridge) path.getCMPField();
                selectManager = (JDBCStoreManager) selectField.getManager();
                selectObject = selectField;
                setTypeFactory(selectManager.getJDBCTypeFactory());
                addJoinPath(path);
                selectAlias = aliasManager.getAlias(path.getPath(path.size() - 2));
                SQLUtil.getColumnNamesClause(selectField, selectAlias, buf);
            } else {
                // set the select object
                JDBCEntityBridge selectEntity = (JDBCEntityBridge) path.getEntity();
                selectManager = (JDBCStoreManager) selectEntity.getManager();
                selectObject = selectEntity;
                setTypeFactory(selectManager.getJDBCTypeFactory());
                selectEntity(path, node.distinct, buf);
            }
        } else {
            // the function should take a path expression as a parameter
            path = getPathFromChildren(child0);

            if (path == null) {
                throw new IllegalStateException("The function in SELECT clause does not contain a path expression.");
            }

            if (path.isCMPField()) {
                JDBCCMPFieldBridge selectField = (JDBCCMPFieldBridge) path.getCMPField();
                selectManager = (JDBCStoreManager) selectField.getManager();
                if (selectField.getJDBCType().hasMapper())
                    this.functionJDBCType = selectField.getJDBCType();
            } else if (path.isCMRField()) {
                JDBCCMRFieldBridge cmrField = (JDBCCMRFieldBridge) path.getCMRField();
                selectManager = (JDBCStoreManager) cmrField.getEntity().getManager();
                addJoinPath(path);
            } else {
                final JDBCEntityBridge entity = (JDBCEntityBridge) path.getEntity();
                selectManager = (JDBCStoreManager) entity.getManager();
                addJoinPath(path);
            }

            setTypeFactory(selectManager.getJDBCTypeFactory());
            selectObject = child0;
            child0.jjtAccept(this, buf);
        }

        return buf;
    }

    /**
     * Generates where clause without the "WHERE" keyword.
     */
    public Object visit(ASTWhere node, Object data) {
        node.jjtGetChild(0).jjtAccept(this, data);
        return data;
    }

    public Object visit(ASTNullComparison node, Object data) {
        StringBuffer buf = (StringBuffer) data;

        final Node child0 = node.jjtGetChild(0);
        if (child0 instanceof ASTPath) {
            ASTPath path = (ASTPath) child0;

            if (path.isCMRField()) {
                JDBCCMRFieldBridge cmrField = (JDBCCMRFieldBridge) path.getCMRField();
                if (cmrField.getRelationMetaData().isTableMappingStyle()) {
                    existsClause(path, buf, !node.not);
                    return buf;
                }
            }

            String alias = aliasManager.getAlias(path.getPath(path.size() - 2));
            JDBCFieldBridge field = (JDBCFieldBridge) path.getField();

            // if jdbc type is null then it should be a cmr field in
            // a one-to-one mapping that isn't a foreign key.
            // handle it the way the IS EMPTY on the one side of one-to-many
            // relationship is handled
            if (field.getJDBCType() == null) {
                existsClause(path, buf, !node.not);
                return buf;
            }

            // check the path for cmr fields and add them to join paths
            if (path.fieldList.size() > 2) {
                for (int i = 0; i < path.fieldList.size(); ++i) {
                    Object pathEl = path.fieldList.get(i);
                    if (pathEl instanceof JDBCCMRFieldBridge) {
                        addJoinPath(path);
                        break;
                    }
                }
            }

            buf = SQLUtil.getIsNullClause(node.not, field, alias, buf);
        } else if (child0 instanceof ASTParameter) {
            ASTParameter param = (ASTParameter) child0;
            Class type = getParameterType(param.number);

            QueryParameter queryParam = new QueryParameter(param.number - 1, typeFactory.getJDBCType(type));
            inputParameters.add(queryParam);

            buf.append("? IS ");
            if (node.not) {
                buf.append(SQLUtil.NOT);
            }
            buf.append(SQLUtil.NULL);
        } else {
            throw new IllegalStateException("Unexpected node in IS NULL clause: " + node);
        }

        return buf;
    }

    public Object visit(ASTIsEmpty node, Object data) {
        StringBuffer buf = (StringBuffer) data;
        ASTPath path = (ASTPath) node.jjtGetChild(0);

        existsClause(path, buf, !node.not);
        return buf;
    }

    /**
     * Compare entity
     */
    public Object visit(ASTMemberOf node, Object data) {
        StringBuffer buf = (StringBuffer) data;

        // setup compare to vars first, so we can compare types in from vars
        ASTPath toPath = (ASTPath) node.jjtGetChild(1);

        JDBCCMRFieldBridge toCMRField = (JDBCCMRFieldBridge) toPath.getCMRField();

        JDBCEntityBridge toChildEntity = (JDBCEntityBridge) toPath.getEntity();

        String pathStr = toPath.getPath(toPath.size() - 2);
        String toParentAlias = aliasManager.getAlias(pathStr);
        String toChildAlias = aliasManager.getAlias(toPath.getPath());
        String relationTableAlias = null;
        if (toCMRField.getRelationMetaData().isTableMappingStyle()) {
            relationTableAlias = aliasManager.getRelationTableAlias(toPath.getPath());
        }

        // setup from variables
        String fromAlias = null;
        int fromParamNumber = -1;
        if (node.jjtGetChild(0) instanceof ASTParameter) {
            ASTParameter fromParam = (ASTParameter) node.jjtGetChild(0);

            // can only compare like kind entities
            verifyParameterEntityType(fromParam.number, toChildEntity);

            fromParamNumber = fromParam.number;
        } else {
            ASTPath fromPath = (ASTPath) node.jjtGetChild(0);
            addJoinPath(fromPath);

            JDBCEntityBridge fromEntity = (JDBCEntityBridge) fromPath.getEntity();
            fromAlias = aliasManager.getAlias(fromPath.getPath());

            // can only compare like kind entities
            if (!fromEntity.equals(toChildEntity)) {
                throw new IllegalStateException("Only like types can be " +
                        "compared: from entity=" +
                        fromEntity.getEntityName() +
                        " to entity=" + toChildEntity.getEntityName());
            }
        }

        // add the path to the list of paths to left join
        addLeftJoinPath(pathStr, toPath);

        // first part makes toChild not in toParent.child
        if (!subquerySupported) {
            addJoinPath(toPath);

            // subquery not supported; use a left join and is not null
            if (node.not) {
                buf.append(SQLUtil.NOT);
            }
            buf.append('(');

            if (relationTableAlias == null) {
                SQLUtil.getIsNullClause(true, toChildEntity.getPrimaryKeyFields(), toChildAlias, buf);
            } else {
                SQLUtil.getIsNullClause(true, toCMRField.getTableKeyFields(), relationTableAlias, buf);
            }
        } else {
            // subquery supported; use exists subquery
            if (node.not) {
                buf.append(SQLUtil.NOT);
            }

            buf.append(SQLUtil.EXISTS).append('(');

            if (relationTableAlias == null) {
                buf.append(SQLUtil.SELECT);
                SQLUtil.getColumnNamesClause(toChildEntity.getPrimaryKeyFields(), toChildAlias, buf)
                        .append(SQLUtil.FROM)
                        .append(toChildEntity.getQualifiedTableName())
                        .append(' ')
                        .append(toChildAlias)
                        .append(SQLUtil.WHERE);
                SQLUtil.getJoinClause(toCMRField, toParentAlias, toChildAlias, buf);
            } else {
                buf.append(SQLUtil.SELECT);
                SQLUtil.getColumnNamesClause(toCMRField.getRelatedCMRField().getTableKeyFields(), relationTableAlias, buf)
                        .append(SQLUtil.FROM)
                        .append(toCMRField.getQualifiedTableName())
                        .append(' ')
                        .append(relationTableAlias)
                        .append(SQLUtil.WHERE);
                SQLUtil.getRelationTableJoinClause(toCMRField, toParentAlias, relationTableAlias, buf);
            }
        }

        buf.append(SQLUtil.AND);

        // second part makes fromNode equal toChild
        if (fromAlias != null) {
            // compare pk to pk
            if (relationTableAlias == null) {
                SQLUtil.getSelfCompareWhereClause(toChildEntity.getPrimaryKeyFields(),
                        toChildAlias,
                        fromAlias,
                        buf);
            } else {
                SQLUtil.getRelationTableJoinClause(toCMRField.getRelatedCMRField(),
                        fromAlias,
                        relationTableAlias,
                        buf);
            }
        } else {
            // add the parameters
            inputParameters.addAll(QueryParameter.createParameters(fromParamNumber - 1,
                    toChildEntity));

            // compare pk to parameter
            if (relationTableAlias == null) {
                SQLUtil.getWhereClause(toChildEntity.getPrimaryKeyFields(), toChildAlias, buf);
            } else {
                SQLUtil.getWhereClause(toCMRField.getRelatedCMRField().getTableKeyFields(),
                        relationTableAlias,
                        buf);
            }
        }

        buf.append(')');

        return buf;
    }

    public Object visit(ASTValueClassComparison node, Object data) {
        StringBuffer buf = (StringBuffer) data;

        boolean not = (node.opp.equals(SQLUtil.NOT_EQUAL));
        String comparison = node.opp;
        buf.append('(');
        if (not) {
            buf.append(SQLUtil.NOT).append('(');
            comparison = "=";
        }

        // setup the from path
        ASTPath fromPath = (ASTPath) node.jjtGetChild(0);
        addJoinPath(fromPath);
        String fromAlias = aliasManager.getAlias(fromPath.getPath(fromPath.size() - 2));
        JDBCCMPFieldBridge fromCMPField = (JDBCCMPFieldBridge) fromPath.getCMPField();

        Node toNode = node.jjtGetChild(1);
        if (toNode instanceof ASTParameter) {
            ASTParameter toParam = (ASTParameter) toNode;

            // can only compare like kind entities
            Class parameterType = getParameterType(toParam.number);
            if (!(fromCMPField.getFieldType().equals(parameterType))) {
                throw new IllegalStateException("Only like types can be " +
                        "compared: from CMP field=" +
                        fromCMPField.getFieldType() +
                        " to parameter=" + parameterType);
            }

            inputParameters.addAll(QueryParameter.createParameters(toParam.number - 1, fromCMPField));
            SQLUtil.getWhereClause(fromCMPField.getJDBCType(), fromAlias, comparison, buf);
        } else {
            ASTPath toPath = (ASTPath) toNode;
            addJoinPath(toPath);
            String toAlias = aliasManager.getAlias(toPath.getPath(toPath.size() - 2));
            JDBCCMPFieldBridge toCMPField = (JDBCCMPFieldBridge) toPath.getCMPField();

            // can only compare like kind entities
            if (!(fromCMPField.getFieldType().equals(toCMPField.getFieldType()))) {
                throw new IllegalStateException("Only like types can be " +
                        "compared: from CMP field=" +
                        fromCMPField.getFieldType() +
                        " to CMP field=" + toCMPField.getFieldType());
            }

            SQLUtil.getSelfCompareWhereClause(fromCMPField, toCMPField, fromAlias, toAlias, comparison, buf);
        }

        return (not ? buf.append(')') : buf).append(')');
    }

    /**
     * compareEntity(arg0, arg1)
     */
    public Object visit(ASTEntityComparison node, Object data) {
        StringBuffer buf = (StringBuffer) data;
        Node arg0 = node.jjtGetChild(0);
        Node arg1 = node.jjtGetChild(1);
        if (node.opp.equals(SQLUtil.NOT_EQUAL)) {
            compareEntity(true, arg0, arg1, buf);
        } else {
            compareEntity(false, arg0, arg1, buf);
        }
        return buf;
    }

    /**
     * Type-mapping function translation
     */
    public Object visit(ASTConcat node, Object data) {
        StringBuffer buf = (StringBuffer) data;
        JDBCFunctionMappingMetaData function = typeMapping.getFunctionMapping(JDBCTypeMappingMetaData.CONCAT);
        Object[] args = new Object[]{
                new NodeStringWrapper(node.jjtGetChild(0)),
                new NodeStringWrapper(node.jjtGetChild(1)),
        };
        function.getFunctionSql(args, buf);
        return buf;
    }

    /**
     * Type-mapping function translation
     */
    public Object visit(ASTSubstring node, Object data) {
        StringBuffer buf = (StringBuffer) data;
        JDBCFunctionMappingMetaData function = typeMapping.getFunctionMapping(JDBCTypeMappingMetaData.SUBSTRING);
        Object[] args = new Object[]{
                new NodeStringWrapper(node.jjtGetChild(0)),
                new NodeStringWrapper(node.jjtGetChild(1)),
                new NodeStringWrapper(node.jjtGetChild(2)),
        };
        function.getFunctionSql(args, buf);
        return buf;
    }

    /**
     * Type-mapping function translation
     */
    public Object visit(ASTLCase node, Object data) {
        StringBuffer buf = (StringBuffer) data;
        JDBCFunctionMappingMetaData function = typeMapping.getFunctionMapping(JDBCTypeMappingMetaData.LCASE);
        Object[] args = new Object[]{
                new NodeStringWrapper(node.jjtGetChild(0)),
        };
        function.getFunctionSql(args, buf);
        return buf;
    }

    /**
     * Type-mapping function translation
     */
    public Object visit(ASTUCase node, Object data) {
        StringBuffer buf = (StringBuffer) data;
        JDBCFunctionMappingMetaData function = typeMapping.getFunctionMapping(JDBCTypeMappingMetaData.UCASE);
        Object[] args = new Object[]{
                new NodeStringWrapper(node.jjtGetChild(0)),
        };
        function.getFunctionSql(args, buf);
        return buf;
    }

    /**
     * Type-mapping function translation
     */
    public Object visit(ASTLength node, Object data) {
        StringBuffer buf = (StringBuffer) data;
        JDBCFunctionMappingMetaData function = typeMapping.getFunctionMapping(JDBCTypeMappingMetaData.LENGTH);
        Object[] args = new Object[]{
                new NodeStringWrapper(node.jjtGetChild(0)),
        };
        function.getFunctionSql(args, buf);
        return buf;
    }

    /**
     * Type-mapping function translation
     */
    public Object visit(ASTLocate node, Object data) {
        StringBuffer buf = (StringBuffer) data;

        JDBCFunctionMappingMetaData function = typeMapping.getFunctionMapping(JDBCTypeMappingMetaData.LOCATE);
        Object[] args = new Object[3];
        args[0] = new NodeStringWrapper(node.jjtGetChild(0));
        args[1] = new NodeStringWrapper(node.jjtGetChild(1));
        if (node.jjtGetNumChildren() == 3) {
            args[2] = new NodeStringWrapper(node.jjtGetChild(2));
        } else {
            args[2] = "1";
        }

        // add the sql to the current buffer
        function.getFunctionSql(args, buf);
        return buf;
    }

    /**
     * Type-mapping function translation
     */
    public Object visit(ASTAbs node, Object data) {
        StringBuffer buf = (StringBuffer) data;
        JDBCFunctionMappingMetaData function = typeMapping.getFunctionMapping(JDBCTypeMappingMetaData.ABS);
        Object[] args = new Object[]{
                new NodeStringWrapper(node.jjtGetChild(0)),
        };
        function.getFunctionSql(args, buf);
        return buf;
    }

    /**
     * Type-mapping function translation
     */
    public Object visit(ASTMod node, Object data) {
        StringBuffer buf = (StringBuffer) data;
        JDBCFunctionMappingMetaData function = typeMapping.getFunctionMapping(JDBCTypeMappingMetaData.MOD);
        Object[] args = new Object[]{
                new NodeStringWrapper(node.jjtGetChild(0)),
                new NodeStringWrapper(node.jjtGetChild(1)),
        };
        function.getFunctionSql(args, buf);
        return buf;
    }

    /**
     * Type-mapping function translation
     */
    public Object visit(ASTSqrt node, Object data) {
        StringBuffer buf = (StringBuffer) data;
        JDBCFunctionMappingMetaData function = typeMapping.getFunctionMapping(JDBCTypeMappingMetaData.SQRT);
        Object[] args = new Object[]{
                new NodeStringWrapper(node.jjtGetChild(0))
        };
        function.getFunctionSql(args, buf);
        return buf;
    }

    public Object visit(ASTCount node, Object data) {
        StringBuffer buf = (StringBuffer) data;
        node.setResultType(returnType);

        Object[] args;
        final ASTPath cntPath = (ASTPath) node.jjtGetChild(0);
        if (cntPath.isCMPField()) {
            args = new Object[]{node.distinct, new NodeStringWrapper(cntPath)};
        } else {
            JDBCEntityBridge entity = (JDBCEntityBridge) cntPath.getEntity();
            final JDBCFieldBridge[] pkFields = entity.getPrimaryKeyFields();
            if (pkFields.length > 1) {
                countCompositePk = true;
                forceDistinct = node.distinct.length() > 0;
                selectEntity(cntPath, forceDistinct, buf);
                return buf;
            } else {

                final String alias = aliasManager.getAlias(cntPath.getPath());
                StringBuffer keyColumn = new StringBuffer(20);
                SQLUtil.getColumnNamesClause(pkFields[0], alias, keyColumn);
                args = new Object[]{node.distinct, keyColumn.toString()};
            }
        }

        return JDBCTypeMappingMetaData.COUNT_FUNC.getFunctionSql(args, buf);
    }

    public Object visit(ASTMax node, Object data) {
        if (functionJDBCType != null) {
            node.setResultType(functionJDBCType.getJavaTypes()[0]);
            node.setJDBCType(functionJDBCType);
        } else
            node.setResultType(returnType);
        StringBuffer buf = (StringBuffer) data;
        Object[] args = new Object[]{
                node.distinct,
                new NodeStringWrapper(node.jjtGetChild(0))
        };
        return JDBCTypeMappingMetaData.MAX_FUNC.getFunctionSql(args, buf);
    }

    public Object visit(ASTMin node, Object data) {
        if (functionJDBCType != null) {
            node.setResultType(functionJDBCType.getJavaTypes()[0]);
            node.setJDBCType(functionJDBCType);
        } else
            node.setResultType(returnType);
        StringBuffer buf = (StringBuffer) data;
        Object[] args = new Object[]{
                node.distinct,
                new NodeStringWrapper(node.jjtGetChild(0))
        };
        return JDBCTypeMappingMetaData.MIN_FUNC.getFunctionSql(args, buf);
    }

    public Object visit(ASTAvg node, Object data) {
        if (functionJDBCType != null) {
            node.setResultType(functionJDBCType.getJavaTypes()[0]);
            node.setJDBCType(functionJDBCType);
        } else
            node.setResultType(returnType);

        StringBuffer buf = (StringBuffer) data;
        Object[] args = new Object[]{
                node.distinct,
                new NodeStringWrapper(node.jjtGetChild(0)),
        };
        return JDBCTypeMappingMetaData.AVG_FUNC.getFunctionSql(args, buf);
    }

    public Object visit(ASTSum node, Object data) {
        if (functionJDBCType != null) {
            node.setResultType(functionJDBCType.getJavaTypes()[0]);
            node.setJDBCType(functionJDBCType);
        } else
            node.setResultType(returnType);
        StringBuffer buf = (StringBuffer) data;
        Object[] args = new Object[]{
                node.distinct,
                new NodeStringWrapper(node.jjtGetChild(0))
        };
        return JDBCTypeMappingMetaData.SUM_FUNC.getFunctionSql(args, buf);
    }

    /**
     * tableAlias.columnName
     */
    public Object visit(ASTPath node, Object data) {
        StringBuffer buf = (StringBuffer) data;
        if (!node.isCMPField()) {
            throw new IllegalStateException("Can only visit cmp valued path " +
                    "node. Should have been handled at a higher level.");
        }

        JDBCCMPFieldBridge cmpField = (JDBCCMPFieldBridge) node.getCMPField();

        // make sure this is mapped to a single column
        switch (node.type) {
            case EJBQLTypes.ENTITY_TYPE:
            case EJBQLTypes.VALUE_CLASS_TYPE:
                if (cmpField.getJDBCType().hasMapper() ||
                        cmpField.getJDBCType().getParameterSetter() != null) {
                    break;
                }
            case EJBQLTypes.UNKNOWN_TYPE:
                throw new IllegalStateException("Can not visit multi-column path " +
                        "node. Should have been handled at a higher level.");
        }

        addJoinPath(node);
        String alias = aliasManager.getAlias(node.getPath(node.size() - 2));
        SQLUtil.getColumnNamesClause(cmpField, alias, buf);
        return buf;
    }

    public Object visit(ASTAbstractSchema node, Object data) {
        throw new IllegalStateException("Can not visit abstract schema node. " +
                "Should have been handled at a higher level.");
    }

    /**
     * ?
     */
    public Object visit(ASTParameter node, Object data) {
        StringBuffer buf = (StringBuffer) data;
        Class type = getParameterType(node.number);

        // make sure this is mapped to a single column
        int ejbqlType = EJBQLTypes.getEJBQLType(type);
        if (ejbqlType == EJBQLTypes.ENTITY_TYPE ||
                ejbqlType == EJBQLTypes.VALUE_CLASS_TYPE ||
                ejbqlType == EJBQLTypes.UNKNOWN_TYPE) {
            throw new IllegalStateException("Can not visit multi-column " +
                    "parameter node. Should have been handled at a higher level.");
        }

        QueryParameter param = new QueryParameter(node.number - 1, typeFactory.getJDBCType(type));
        inputParameters.add(param);
        buf.append('?');
        return buf;
    }

    /**
     * typeMapping.get<True/False>Mapping()
     */
    public Object visit(ASTBooleanLiteral node, Object data) {
        StringBuffer buf = (StringBuffer) data;
        if (node.value) {
            buf.append(typeMapping.getTrueMapping());
        } else {
            buf.append(typeMapping.getFalseMapping());
        }
        return data;
    }

    public Object visit(ASTLimitOffset node, Object data) {
        int child = 0;
        if (node.hasOffset) {
            Node offsetNode = node.jjtGetChild(child++);
            if (offsetNode instanceof ASTParameter) {
                ASTParameter param = (ASTParameter) offsetNode;
                Class parameterType = getParameterType(param.number);
                if (int.class != parameterType && Integer.class != parameterType) {
                    throw new UnsupportedOperationException("OFFSET parameter must be an int");
                }
                offsetParam = param.number;
            } else {
                ASTExactNumericLiteral param = (ASTExactNumericLiteral) offsetNode;
                offsetValue = (int) param.value;
            }
        }
        if (node.hasLimit) {
            Node limitNode = node.jjtGetChild(child);
            if (limitNode instanceof ASTParameter) {
                ASTParameter param = (ASTParameter) limitNode;
                Class parameterType = getParameterType(param.number);
                if (int.class != parameterType && Integer.class != parameterType) {
                    throw new UnsupportedOperationException("LIMIT parameter must be an int");
                }
                limitParam = param.number;
            } else {
                ASTExactNumericLiteral param = (ASTExactNumericLiteral) limitNode;
                limitValue = (int) param.value;
            }
        }
        return data;
    }

    public Object visit(ASTWhereConditionalTerm node, Object data) {
        // clear per term paths
        clearPerTermJoinPaths();

        StringBuffer buf = (StringBuffer) data;
        buf.append('(');
        for (int i = 0; i < node.jjtGetNumChildren(); ++i) {
            node.jjtGetChild(i).jjtAccept(this, data);
        }

        StringBuffer thetaJoin = new StringBuffer();
        createThetaJoin(thetaJoin);

        if (thetaJoin.length() > 0) {
            buf.append(SQLUtil.AND).append(thetaJoin.toString());
        }

        buf.append(')');
        return data;
    }

    /**
     * Wrap a node with a class that when ever toString is called visits the
     * node.  This is used by the function implementations, for parameters.
     * <p/>
     * Be careful with this class because it visits the node for each call of
     * toString, which could have undesirable result if called multiple times.
     */
    private final class NodeStringWrapper {
        final Node node;

        public NodeStringWrapper(Node node) {
            this.node = node;
        }

        public String toString() {
            return node.jjtAccept(JDBCEJBQLCompiler.this, new StringBuffer()).toString();
        }
    }

    /**
     * Recursively searches for ASTPath among children.
     *
     * @param selectFunction a node implements SelectFunction
     * @return ASTPath child or null if there was no child of type ASTPath
     */
    private ASTPath getPathFromChildren(Node selectFunction) {
        for (int childInd = 0; childInd < selectFunction.jjtGetNumChildren(); ++childInd) {
            Node child = selectFunction.jjtGetChild(childInd);
            if (child instanceof ASTPath) {
                return (ASTPath) child;
            } else if (child instanceof SelectFunction) {
                Node path = getPathFromChildren(child);
                if (path != null) {
                    return (ASTPath) path;
                }
            }
        }
        return null;
    }

    /**
     * Checks whether the path passed in is already in the SELECT clause.
     *
     * @param path the path to check.
     * @return true  if the path is already in the SELECT clause.
     */
    private boolean isSelected(ASTPath path) {
        boolean selected = false;

        CMPFieldBridge cmpField = path.getCMPField();
        if (selectObject instanceof JDBCCMPFieldBridge && cmpField == selectObject) {
            selected = true;
        } else if (selectObject instanceof JDBCEntityBridge) {
            JDBCEntityBridge entity = (JDBCEntityBridge) selectObject;
            JDBCFieldBridge[] pkFields = entity.getPrimaryKeyFields();
            for (int pkInd = 0; pkInd < pkFields.length; ++pkInd) {
                if (pkFields[pkInd] == cmpField) {
                    selected = true;
                    break;
                }
            }
        } else if (selectObject instanceof SelectFunction) {
            Node funcNode = (Node) selectObject;
            ASTPath fieldPath = getPathFromChildren(funcNode);
            if (fieldPath.getCMPField() == cmpField) {
                selected = true;
            }
        }

        return selected;
    }

    private void selectEntity(ASTPath path, boolean distinct, StringBuffer buf) {
        JDBCEntityBridge selectEntity = (JDBCEntityBridge) path.getEntity();

        StringBuffer columnNamesClause = new StringBuffer(200);
        addJoinPath(path);
        selectAlias = aliasManager.getAlias(path.getPath());

        // get a list of all fields to be loaded
        // get the identifier for this field
        SQLUtil.getColumnNamesClause(selectEntity.getPrimaryKeyFields(), selectAlias, columnNamesClause);

        if (readAhead.isOnFind()) {
            String eagerLoadGroupName = readAhead.getEagerLoadGroup();
            boolean[] loadGroupMask = selectEntity.getLoadGroupMask(eagerLoadGroupName);
            if (distinct)
                SQLUtil.appendSearchableColumnNamesClause(selectEntity.getTableFields(), loadGroupMask, selectAlias, columnNamesClause);
            else
                SQLUtil.appendColumnNamesClause(selectEntity.getTableFields(), loadGroupMask, selectAlias, columnNamesClause);

            try {
                leftJoinCMRList = JDBCAbstractQueryCommand.getLeftJoinCMRNodes(
                        selectEntity, path.getPath(), readAhead.getLeftJoins(), declaredPaths);
            } catch (Exception e) {
                throw new IllegalStateException(e.getMessage());
            }

            if (!leftJoinCMRList.isEmpty()) {
                onFindCMRJoin = new StringBuffer(100);
                JDBCAbstractQueryCommand.leftJoinCMRNodes(selectAlias, leftJoinCMRList, aliasManager, onFindCMRJoin);
                JDBCAbstractQueryCommand.appendLeftJoinCMRColumnNames(leftJoinCMRList, aliasManager, columnNamesClause);
            }
        }
        buf.append(columnNamesClause);
    }

    private void addJoinPath(ASTPath path) {
        ctermJoinPaths.add(path);
        allJoinPaths.add(path);
    }

    private void addCollectionMemberJoinPath(String alias, ASTPath path) {
        ctermCollectionMemberJoinPaths.put(alias, path);
        allCollectionMemberJoinPaths.put(alias, path);
    }

    private void addLeftJoinPath(String pathStr, ASTPath path) {
        Set set = (Set) ctermLeftJoinPaths.get(pathStr);
        if (set == null) {
            set = new HashSet();
            ctermLeftJoinPaths.put(pathStr, set);
        }
        set.add(path);

        set = (Set) allLeftJoinPaths.get(pathStr);
        if (set == null) {
            set = new HashSet();
            allLeftJoinPaths.put(pathStr, set);
        }
        set.add(path);
    }

    private void clearPerTermJoinPaths() {
        ctermJoinPaths.clear();
        ctermCollectionMemberJoinPaths.clear();
        ctermLeftJoinPaths.clear();
    }

    private boolean isDistinct(Node selectNode) {
        return ((ASTSelect) selectNode).distinct || returnType.equals(Set.class) || forceDistinct;
    }
}
