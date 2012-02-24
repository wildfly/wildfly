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
package org.jboss.as.cmp.jdbc.metadata;

import java.util.ArrayList;
import java.util.List;
import static org.jboss.as.cmp.CmpMessages.MESSAGES;
import org.jboss.as.cmp.jdbc.metadata.parser.ParsedEntity;
import org.jboss.as.cmp.jdbc.metadata.parser.ParsedRelationship;
import org.jboss.as.cmp.jdbc.metadata.parser.ParsedRelationshipRole;
import org.jboss.metadata.ejb.spec.RelationMetaData;
import org.jboss.metadata.ejb.spec.RelationRoleMetaData;

/**
 * This class represents one ejb-relation element in the ejb-jar.xml file. Most
 * properties of this class are immutable. The mutable properties have set
 * methods.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom </a>
 * @author <a href="mailto:heiko.rupp@cellent.de">Heiko W. Rupp </a>
 * @version $Revision: 81030 $
 */
public final class JDBCRelationMetaData {
    public enum MappingStyle {TABLE, FOREIGN_KEY}

    /**
     * Name of the relation. Loaded from the ejb-relation-name element.
     */
    private String relationName;

    /**
     * The mapping style for this relation (i.e., TABLE or FOREIGN_KEY).
     */
    private MappingStyle mappingStyle;

    /**
     * data source name in jndi
     */
    private String dataSourceName;

    /**
     * type mapping used for the relation table
     */
    private JDBCTypeMappingMetaData datasourceMapping;

    /**
     * the name of the table to use for this bean
     */
    private String tableName;

    /**
     * is table created
     */
    private boolean tableCreated;

    /**
     * is table dropped
     */
    private boolean tableDropped;

    /**
     * should we create the table when deployed
     */
    private Boolean createTable;

    /**
     * should we drop the table when deployed
     */
    private Boolean removeTable;

    /**
     * should we alter the table when deployed
     */
    private Boolean alterTable;

    /**
     * What commands should be issued directly after creation of a table?
     */
    private final List<String> tablePostCreateCmd = new ArrayList<String>();

    /**
     * should we use 'SELECT ... FOR UPDATE' syntax?
     */
    private Boolean rowLocking;

    /**
     * should the table have a primary key constraint?
     */
    private Boolean primaryKeyConstraint;

    /**
     * is the relationship read-only?
     */
    private Boolean readOnly;

    /**
     * how long is read valid
     */
    private Integer readTimeOut;


    /**
     * The left jdbc relationship role. Loaded from an ejb-relationship-role.
     * Left/right assignment is completely arbitrary.
     */
    private JDBCRelationshipRoleMetaData left;

    /**
     * The right relationship role. Loaded from an ejb-relationship-role.
     * Left/right assignment is completely arbitrary.
     */
    private JDBCRelationshipRoleMetaData right;

    /**
     * For parsed roles only
     */
    private List<JDBCRelationshipRoleMetaData> roles = new ArrayList<JDBCRelationshipRoleMetaData>();

    /**
     * Parse only constructor.
     */
    public JDBCRelationMetaData() {
    }

    /**
     * Constructs jdbc relation meta data with the data from the relation
     * metadata loaded from the ejb-jar.xml file.
     *
     * @param jdbcApplication  used to retrieve the entities of this relation
     * @param relationMetaData relation meta data loaded from the ejb-jar.xml
     *                         file
     */
    public JDBCRelationMetaData(final JDBCApplicationMetaData jdbcApplication, final RelationMetaData relationMetaData) {
        final RelationRoleMetaData leftRole = relationMetaData.getLeftRole();
        final RelationRoleMetaData rightRole = relationMetaData.getRightRole();

        // set the default mapping style
        if (leftRole.isMultiplicityMany() && rightRole.isMultiplicityMany()) {
            mappingStyle = MappingStyle.TABLE;
        } else {
            mappingStyle = MappingStyle.FOREIGN_KEY;
        }

        left = new JDBCRelationshipRoleMetaData(this, jdbcApplication, leftRole);
        right = new JDBCRelationshipRoleMetaData(this, jdbcApplication, rightRole);

        left.init(right);
        right.init(left);

        relationName = getNonNullRelationName(left, right, relationMetaData.getEjbRelationName());

        if (mappingStyle == MappingStyle.TABLE) {
            tableName = createDefaultTableName();
        } else {
            tableName = null;
        }
    }

    public JDBCRelationMetaData(JDBCApplicationMetaData jdbcApplication, JDBCRelationMetaData defaultValues) {
        final JDBCEntityMetaData defaultEntity = jdbcApplication.getDefaultEntity();

        mappingStyle = defaultValues.mappingStyle;
        readOnly = defaultValues.readOnly != null ? defaultValues.readOnly : defaultEntity.isReadOnly();
        readTimeOut = defaultValues.readTimeOut != null ? defaultValues.readTimeOut : defaultEntity.getReadTimeOut();
        dataSourceName = defaultValues.dataSourceName != null ? defaultValues.dataSourceName : defaultEntity.getDataSourceName();
        datasourceMapping = defaultValues.datasourceMapping != null ? defaultValues.datasourceMapping : defaultEntity.getTypeMapping();

        String tableNameString = defaultValues.getDefaultTableName();
        if (tableNameString == null) {
            tableNameString = defaultValues.createDefaultTableName();
        }
        tableName = tableNameString;
        createTable = defaultValues.createTable != null ? defaultValues.createTable : defaultEntity.getCreateTable();
        removeTable = defaultValues.removeTable != null ? defaultValues.removeTable : defaultEntity.getRemoveTable();
        if (defaultValues.tablePostCreateCmd != null && !defaultValues.tablePostCreateCmd.isEmpty()) {
            tablePostCreateCmd.addAll(defaultValues.tablePostCreateCmd);
        } else if (defaultEntity.getDefaultTablePostCreateCmd() != null) {
            tablePostCreateCmd.addAll(defaultEntity.getDefaultTablePostCreateCmd());
        }
        alterTable = defaultValues.alterTable != null ? defaultValues.alterTable : defaultEntity.getAlterTable();
        rowLocking = defaultValues.rowLocking != null ? defaultValues.rowLocking : defaultEntity.hasRowLocking();
        primaryKeyConstraint = defaultValues.primaryKeyConstraint != null ? defaultValues.primaryKeyConstraint : defaultEntity.hasPrimaryKeyConstraint();

        JDBCRelationshipRoleMetaData defaultLeft = defaultValues.getLeftRelationshipRole();
        JDBCRelationshipRoleMetaData defaultRight = defaultValues.getRightRelationshipRole();

        left = new JDBCRelationshipRoleMetaData(this, jdbcApplication, defaultLeft);
        right = new JDBCRelationshipRoleMetaData(this, jdbcApplication, defaultRight);
        left.init(right);
        right.init(left);

        this.relationName = getNonNullRelationName(left, right, defaultValues.getRelationName());

        // at least one side of a fk relation must have keys
        if (isForeignKeyMappingStyle() && left.getKeyFields().isEmpty() && right.getKeyFields().isEmpty()) {
            throw MESSAGES.atLeastOneRelationshipRoleMustHaveField(relationName);
        }

        // both sides of a table relation must have keys
        if (isTableMappingStyle() && (left.getKeyFields().isEmpty() || right.getKeyFields().isEmpty())) {
            throw MESSAGES.bothRolesMustHaveFields(relationName);
        }
    }

    public JDBCRelationMetaData(JDBCApplicationMetaData jdbcApplication, ParsedEntity defaultEntity, JDBCRelationMetaData defaultValues) {
        String preferredRelationMapping = defaultEntity.getPreferredMappingStyle();
        if ("relation-table".equals(preferredRelationMapping) || defaultValues.isManyToMany()) {
            mappingStyle = MappingStyle.TABLE;
        } else {
            mappingStyle = MappingStyle.FOREIGN_KEY;
        }

        // read-only
        if (defaultEntity.getReadOnly() != null) {
            readOnly = defaultEntity.getReadOnly();
        } else {
            readOnly = defaultValues.isReadOnly();
        }

        // read-time-out
        if (defaultEntity.getReadTimeOut() != null) {
            readTimeOut = defaultEntity.getReadTimeOut();
        } else {
            readTimeOut = defaultValues.getReadTimeOut();
        }

        if (defaultEntity.getDataSourceName() != null)
            dataSourceName = defaultEntity.getDataSourceName();
        else
            dataSourceName = defaultValues.getDataSourceName();

        // get the type mapping for this datasource (optional, but always
        // set in standardjbosscmp-jdbc.xml)
        if (defaultEntity.getDataSourceMappingName() != null) {
            datasourceMapping = jdbcApplication.getTypeMappingByName(defaultEntity.getDataSourceMappingName());
            if (datasourceMapping == null) {
                throw MESSAGES.datasourceMappingNotFound(defaultEntity.getDataSourceMappingName());
            }
        } else if (defaultValues.getTypeMapping() != null) {
            datasourceMapping = defaultValues.getTypeMapping();
        }

        // get table name
        String tableNameString = defaultEntity.getTableName();
        if (tableNameString == null) {
            tableNameString = defaultValues.getDefaultTableName();
            if (tableNameString == null) {
                // use defaultValues to create default, because left/right
                // have not been assigned yet, and values used to generate
                // default table name never change
                tableNameString = defaultValues.createDefaultTableName();
            }
        }
        tableName = tableNameString;

        // create table? If not provided, keep default.
        if (defaultEntity.getCreateTable() != null) {
            createTable = defaultEntity.getCreateTable();
        } else {
            createTable = defaultValues.getCreateTable();
        }

        // remove table? If not provided, keep default.
        if (defaultEntity.getRemoveTable() != null) {
            removeTable = defaultEntity.getRemoveTable();
        } else {
            removeTable = defaultValues.getRemoveTable();
        }

        // post-table-create commands
        if (!defaultEntity.getTablePostCreateCmd().isEmpty()) {
            tablePostCreateCmd.addAll(defaultEntity.getTablePostCreateCmd());
        } else {
            tablePostCreateCmd.addAll(defaultValues.getDefaultTablePostCreateCmd());
        }

        // alter table? If not provided, keep default.
        if (defaultEntity.getAlterTable() != null) {
            alterTable = defaultEntity.getAlterTable();
        } else {
            alterTable = defaultValues.getAlterTable();
        }

        // select for update
        if (defaultEntity.getRowLocking() != null) {
            rowLocking = !isReadOnly() && defaultEntity.getRowLocking();
        } else {
            rowLocking = defaultValues.hasRowLocking();
        }

        // primary key constraint? If not provided, keep default.
        if (defaultEntity.getPrimaryKeyConstraint() != null) {
            primaryKeyConstraint = defaultEntity.getPrimaryKeyConstraint();
        } else {
            primaryKeyConstraint = defaultValues.hasPrimaryKeyConstraint();
        }

        //
        // load metadata for each specified role
        //
        JDBCRelationshipRoleMetaData defaultLeft = defaultValues.getLeftRelationshipRole();
        JDBCRelationshipRoleMetaData defaultRight = defaultValues.getRightRelationshipRole();

        left = new JDBCRelationshipRoleMetaData(this, jdbcApplication, defaultLeft);
        right = new JDBCRelationshipRoleMetaData(this, jdbcApplication, defaultRight);
        left.init(right);
        right.init(left);

        this.relationName = getNonNullRelationName(left, right, defaultValues.getRelationName());
    }

    public JDBCRelationMetaData(JDBCApplicationMetaData jdbcApplication, ParsedRelationship parsedRelationship, JDBCRelationMetaData defaultValues) {
        if (parsedRelationship.getMappingStyle() != null) {
            mappingStyle = parsedRelationship.getMappingStyle();
        } else {
            mappingStyle = defaultValues.mappingStyle;
        }

        // read-only
        if (parsedRelationship.getReadOnly() != null) {
            readOnly = parsedRelationship.getReadOnly();
        } else {
            readOnly = defaultValues.isReadOnly();
        }

        // read-time-out
        if (parsedRelationship.getReadTimeOut() != null) {
            readTimeOut = parsedRelationship.getReadTimeOut();
        } else {
            readTimeOut = defaultValues.getReadTimeOut();
        }

        // datasource name
        if (parsedRelationship.getDataSourceName() != null)
            dataSourceName = parsedRelationship.getDataSourceName();
        else
            dataSourceName = defaultValues.getDataSourceName();

        // get the type mapping for this datasource (optional, but always
        // set in standardjbosscmp-jdbc.xml)
        if (parsedRelationship.getDatasourceMapping() != null) {
            datasourceMapping = jdbcApplication.getTypeMappingByName(parsedRelationship.getDatasourceMapping());
            if (datasourceMapping == null) {
                throw MESSAGES.datasourceMappingNotFound(parsedRelationship.getDatasourceMapping());
            }
        } else if (defaultValues.getTypeMapping() != null) {
            datasourceMapping = defaultValues.getTypeMapping();
        } else {
            //datasourceMapping = JDBCEntityMetaData.obtainTypeMappingFromLibrary(dataSourceName);
        }

        // get table name
        String tableNameString = parsedRelationship.getTableName();
        if (tableNameString == null) {
            tableNameString = defaultValues.getDefaultTableName();
            if (tableNameString == null) {
                // use defaultValues to create default, because left/right
                // have not been assigned yet, and values used to generate
                // default table name never change
                tableNameString = defaultValues.createDefaultTableName();
            }
        }
        tableName = tableNameString;

        // create table? If not provided, keep default.
        if (parsedRelationship.getCreateTable() != null) {
            createTable = parsedRelationship.getCreateTable();
        } else {
            createTable = defaultValues.getCreateTable();
        }

        // remove table? If not provided, keep default.
        if (parsedRelationship.getRemoveTable() != null) {
            removeTable = parsedRelationship.getRemoveTable();
        } else {
            removeTable = defaultValues.getRemoveTable();
        }

        // post-table-create commands
        if (!parsedRelationship.getTablePostCreateCmd().isEmpty()) {
            tablePostCreateCmd.addAll(parsedRelationship.getTablePostCreateCmd());
        } else {
            tablePostCreateCmd.addAll(defaultValues.getDefaultTablePostCreateCmd());
        }

        // alter table? If not provided, keep default.
        if (parsedRelationship.getAlterTable() != null) {
            alterTable = parsedRelationship.getAlterTable();
        } else {
            alterTable = defaultValues.getAlterTable();
        }

        // select for update
        if (parsedRelationship.getRowLocking() != null) {
            rowLocking = !isReadOnly() && parsedRelationship.getRowLocking();
        } else {
            rowLocking = defaultValues.hasRowLocking();
        }

        // primary key constraint? If not provided, keep default.
        if (parsedRelationship.getPrimaryKeyConstraint() != null) {
            primaryKeyConstraint = parsedRelationship.getPrimaryKeyConstraint();
        } else {
            primaryKeyConstraint = defaultValues.hasPrimaryKeyConstraint();
        }

        //
        // load metadata for each specified role
        //
        JDBCRelationshipRoleMetaData defaultLeft = defaultValues.getLeftRelationshipRole();
        JDBCRelationshipRoleMetaData defaultRight = defaultValues.getRightRelationshipRole();

        if (parsedRelationship.getRoles().isEmpty()) {
            // no roles specified use the defaults
            left = new JDBCRelationshipRoleMetaData(this, jdbcApplication, defaultLeft);
            right = new JDBCRelationshipRoleMetaData(this, jdbcApplication, defaultRight);

            left.init(right);
            right.init(left);
        } else {
            ParsedRelationshipRole leftRole = getEJBRelationshipRole(parsedRelationship.getRoles(), defaultLeft);
            left = new JDBCRelationshipRoleMetaData(this, jdbcApplication, leftRole, defaultLeft);

            ParsedRelationshipRole rightRole = getEJBRelationshipRole(parsedRelationship.getRoles(), defaultRight);
            right = new JDBCRelationshipRoleMetaData(this, jdbcApplication, rightRole, defaultRight);

            left.init(right, leftRole);
            right.init(left, rightRole);
        }

        this.relationName = getNonNullRelationName(left, right, defaultValues.getRelationName());

        // at least one side of a fk relation must have keys
        if (isForeignKeyMappingStyle() && left.getKeyFields().isEmpty() && right.getKeyFields().isEmpty()) {
            throw MESSAGES.atLeastOneRelationshipRoleMustHaveField(relationName);
        }

        // both sides of a table relation must have keys
        if (isTableMappingStyle() && (left.getKeyFields().isEmpty() || right.getKeyFields().isEmpty())) {
            throw MESSAGES.bothRolesMustHaveFields(relationName);
        }
    }

    private ParsedRelationshipRole getEJBRelationshipRole(List<ParsedRelationshipRole> roles, JDBCRelationshipRoleMetaData defaultRole) {
        final String roleName = defaultRole.getRelationshipRoleName();

        if (roleName == null) {
            throw MESSAGES.noEjbRelationRoleNameElement();
        }

        for (ParsedRelationshipRole role : roles) {
            if (roleName.equals(role.getRelationshipRoleName())) {
                return role;
            }
        }
        throw MESSAGES.noEjbRelationshipRole(roleName);
    }

    /**
     * Gets the left jdbc relationship role. The relationship role is loaded
     * from an ejb-relationship-role. Left/right assignment is completely
     * arbitrary.
     *
     * @return the left JDBCRelationshipRoleMetaData
     */
    public JDBCRelationshipRoleMetaData getLeftRelationshipRole() {
        return left;
    }

    /**
     * Gets the right jdbc relationship role. The relationship role is loaded
     * from an ejb-relationship-role. Left/right assignment is completely
     * arbitrary.
     *
     * @return the right JDBCRelationshipRoleMetaData
     */
    public JDBCRelationshipRoleMetaData getRightRelationshipRole() {
        return right;
    }

    /**
     * Gets the relationship role related to the specified role.
     *
     * @param role the relationship role that the related role is desired
     * @return the relationship role related to the specified role. right role
     *         of this relation
     */
    public JDBCRelationshipRoleMetaData getOtherRelationshipRole(JDBCRelationshipRoleMetaData role) {
        if (left == role) {
            return right;
        } else if (right == role) {
            return left;
        } else {
            throw MESSAGES.roleNotLeftOrRightRole(role.getRelationshipRoleName());
        }
    }

    /**
     * Gets the relation name. Relation name is loaded from the
     * ejb-relation-name element.
     *
     * @return the name of this relation
     */
    public String getRelationName() {
        return relationName;
    }


    /**
     * Should this relation be mapped to a relation table.
     *
     * @return true if this relation is mapped to a table
     */
    public boolean isTableMappingStyle() {
        return mappingStyle == MappingStyle.TABLE;
    }

    /**
     * Should this relation use foreign keys for storage.
     *
     * @return true if this relation is mapped to foreign keys
     */
    public boolean isForeignKeyMappingStyle() {
        return mappingStyle == MappingStyle.FOREIGN_KEY;
    }

    /**
     * Gets the name of the datasource in jndi for this entity
     *
     * @return the name of datasource in jndi
     */
    public String getDataSourceName() {
        return dataSourceName;
    }

    /**
     * Gets the name of the relation table.
     *
     * @return the name of the relation table to which is relation is mapped
     */
    public String getDefaultTableName() {
        return tableName;
    }

    /**
     * Gets the (user-defined) SQL commands that should be issued to the db
     * after table creation.
     *
     * @return the SQL command
     */
    public List<String> getDefaultTablePostCreateCmd() {
        return tablePostCreateCmd;
    }

    /**
     * Does the table exist yet? This does not mean that table has been created
     * by the application, or the the database metadata has been checked for the
     * existence of the table, but that at this point the table is assumed to
     * exist.
     *
     * @return true if the table exists
     */
    public boolean isTableCreated() {
        return tableCreated;
    }

    public void setTableCreated() {
        tableCreated = true;
    }

    /**
     * Sets table dropped flag.
     */
    public void setTableDropped() {
        this.tableDropped = true;
    }

    public boolean isTableDropped() {
        return tableDropped;
    }

    /**
     * Should the relation table be created on startup.
     *
     * @return true if the store manager should attempt to create the relation
     *         table
     */
    public boolean getCreateTable() {
        return createTable != null && createTable;
    }

    /**
     * Should the relation table be removed on shutdown.
     *
     * @return true if the store manager should attempt to remove the relation
     *         table
     */
    public boolean getRemoveTable() {
        return removeTable != null && removeTable;
    }

    /**
     * Should the relation table be altered on deploy.
     */
    public boolean getAlterTable() {
        return alterTable != null && alterTable;
    }

    /**
     * When the relation table is created, should it have a primary key
     * constraint.
     *
     * @return true if the store manager should add a primary key constraint
     *         to the the create table sql statement
     */
    public boolean hasPrimaryKeyConstraint() {
        return primaryKeyConstraint != null && primaryKeyConstraint;
    }

    /**
     * Is this relation read-only?
     */
    public boolean isReadOnly() {
        return readOnly != null && readOnly;
    }

    /**
     * Gets the read time out length.
     */
    public int getReadTimeOut() {
        return readTimeOut != null ? readTimeOut : -1;
    }

    /**
     * Should select queries do row locking
     */
    public boolean hasRowLocking() {
        return rowLocking != null && rowLocking;
    }

    public void setRelationName(final String relationName) {
        this.relationName = relationName;
    }

    public void setReadOnly(final boolean readOnly) {
        this.readOnly = readOnly;
    }

    public void setReadTimeOut(final int readTimeOut) {
        this.readTimeOut = readTimeOut;
    }

    public void setPrimaryKeyConstraint(final boolean primaryKeyConstraint) {
        this.primaryKeyConstraint = primaryKeyConstraint;
    }

    public void setTableName(final String tableName) {
        this.tableName = tableName;
    }

    public void setDatasourceMapping(final JDBCTypeMappingMetaData datasourceMapping) {
        this.datasourceMapping = datasourceMapping;
    }

    public void setDataSourceName(final String dataSourceName) {
        this.dataSourceName = dataSourceName;
    }

    public void setCreateTable(final boolean createTable) {
        this.createTable = createTable;
    }

    public void setRemoveTable(final boolean removeTable) {
        this.removeTable = removeTable;
    }

    public void setAlterTable(final boolean alterTable) {
        this.alterTable = alterTable;
    }

    public void setRowLocking(final boolean rowLocking) {
        this.rowLocking = rowLocking;
    }

    public void setMappingStyle(final MappingStyle mappingStyle) {
        this.mappingStyle = mappingStyle;
    }

    public void addPostTableCreate(String command) {
        this.tablePostCreateCmd.add(command);
    }

    private static String getNonNullRelationName(JDBCRelationshipRoleMetaData left, JDBCRelationshipRoleMetaData right, String relationName) {
        // JBossCMP needs ejb-relation-name if jbosscmp-jdbc.xml is used to map relationships.
        if (relationName == null) {
            // generate unique name, we can't rely on ejb-relationship-role-name being unique
            relationName = left.getEntity().getName() +
                    (!left.isNavigable() ? "" : "_" + left.getCMRFieldName()) +
                    "-" +
                    right.getEntity().getName() +
                    (!right.isNavigable() ? "" : "_" + right.getCMRFieldName());
        }
        return relationName;
    }

    private String createDefaultTableName() {
        String defaultTableName = left.getEntity().getName();
        if (left.getCMRFieldName() != null) {
            defaultTableName += "_" + left.getCMRFieldName();
        }
        defaultTableName += "_" + right.getEntity().getName();
        if (right.getCMRFieldName() != null) {
            defaultTableName += "_" + right.getCMRFieldName();
        }
        return defaultTableName;
    }

    /**
     * Gets the jdbc type mapping for this entity
     *
     * @return the jdbc type mapping for this entity
     */
    public JDBCTypeMappingMetaData getTypeMapping() {
        return datasourceMapping;
    }

    public void setLeft(JDBCRelationshipRoleMetaData jdbcRelationshipRoleMetaData) {
        this.left = jdbcRelationshipRoleMetaData;
    }

    public void setRight(JDBCRelationshipRoleMetaData jdbcRelationshipRoleMetaData) {
        this.left = jdbcRelationshipRoleMetaData;
    }

    private boolean isManyToMany() {
        return left.isMultiplicityMany() && right.isMultiplicityMany();
    }
}
