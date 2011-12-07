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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.as.cmp.jdbc.metadata.parser.ParsedCmpField;
import org.jboss.as.cmp.jdbc.metadata.parser.ParsedRelationshipRole;
import org.jboss.metadata.ejb.spec.RelationRoleMetaData;

/**
 * Imutable class which represents one ejb-relationship-role element found in
 * the ejb-jar.xml file's ejb-relation elements.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @version $Revision: 81030 $
 */
public final class JDBCRelationshipRoleMetaData {
    /**
     * Relation to which this role belongs.
     */
    private final JDBCRelationMetaData relationMetaData;

    /**
     * Role name
     */
    private final String relationshipRoleName;

    /**
     * Should this role have a foreign key constraint?
     */
    private final boolean foreignKeyConstraint;

    /**
     * Should the cascade-delete be batched.
     */
    private final boolean batchCascadeDelete;

    private boolean genIndex;

    /**
     * Type of the cmr field (i.e., collection or set)
     */
    private JDBCReadAheadMetaData readAhead;

    /**
     * Is the multiplicity one? If not, multiplicity is many.
     */
    private final boolean multiplicityOne;

    /**
     * Should this entity be deleted when related entity is deleted.
     */
    private final boolean cascadeDelete;

    /**
     * The entity that has this role.
     */
    private final JDBCEntityMetaData entity;

    /**
     * Name of the entity's cmr field for this role.
     */
    private final String cmrFieldName;

    /**
     * true if this side is navigable
     */
    private final boolean navigable;

    /**
     * Type of the cmr field (i.e., collection or set)
     */
    private final String cmrFieldType;

    /**
     * The other role in this relationship.
     */
    private JDBCRelationshipRoleMetaData relatedRole;

    /**
     * The key fields used by this role by field name.
     */
    private Map<String, JDBCCMPFieldMetaData> keyFields = new HashMap<String, JDBCCMPFieldMetaData>();

    public JDBCRelationshipRoleMetaData(final JDBCRelationMetaData relationMetaData, final JDBCApplicationMetaData application, final RelationRoleMetaData role) {
        this.relationMetaData = relationMetaData;

        relationshipRoleName = role.getEjbRelationshipRoleName();
        multiplicityOne = role.isMultiplicityOne();
        cascadeDelete = role.isCascadedDelete();
        batchCascadeDelete = false;
        foreignKeyConstraint = false;
        readAhead = null;

        String fieldName = role.getCmrField() != null ? role.getCmrField().getCmrFieldName() : null;
        if (fieldName == null) {
            cmrFieldName = generateNonNavigableCMRName(role);
            navigable = false;
        } else {
            cmrFieldName = fieldName;
            navigable = true;
        }
        cmrFieldType = role.getCmrField() != null ? role.getCmrField().getCmrFieldType() : null;
        // get the entity for this role
        entity = application.getBeanByEjbName(role.getRoleSource().getEjbName());
        if (entity == null) {
            throw new IllegalArgumentException("Entity: " + role.getRoleSource().getEjbName() +
                    " not found for relation: " + role.getRelation().getEjbRelationName());
        }
    }

    public JDBCRelationshipRoleMetaData(JDBCRelationMetaData relationMetaData, JDBCApplicationMetaData application, JDBCRelationshipRoleMetaData defaultValues) {
        this.relationMetaData = relationMetaData;
        this.entity = application.getBeanByEjbName(defaultValues.getEntity().getName());

        relationshipRoleName = defaultValues.getRelationshipRoleName();
        multiplicityOne = defaultValues.isMultiplicityOne();
        cascadeDelete = defaultValues.isCascadeDelete();

        cmrFieldName = defaultValues.getCMRFieldName();
        navigable = defaultValues.isNavigable();
        cmrFieldType = defaultValues.getCMRFieldType();

        foreignKeyConstraint = defaultValues.hasForeignKeyConstraint();
        readAhead = entity.getReadAhead();

        batchCascadeDelete = defaultValues.isBatchCascadeDelete();
        if (batchCascadeDelete) {
            if (!cascadeDelete)
                throw new RuntimeException(relationMetaData.getRelationName() + '/' + relationshipRoleName + " has batch-cascade-delete in jbosscmp-jdbc.xml but has no cascade-delete in ejb-jar.xml");

            if (relationMetaData.isTableMappingStyle()) {
                throw new RuntimeException("Relationship " + relationMetaData.getRelationName() + " with relation-table-mapping style was setup for batch cascade-delete." + " Batch cascade-delete supported only for foreign key mapping style.");
            }
        }
    }

    public JDBCRelationshipRoleMetaData(JDBCRelationMetaData relationMetaData, JDBCApplicationMetaData application, ParsedRelationshipRole parsedRelationshipRole, JDBCRelationshipRoleMetaData defaultValues) {
        this.relationMetaData = relationMetaData;
        this.entity = application.getBeanByEjbName(defaultValues.getEntity().getName());

        relationshipRoleName = defaultValues.getRelationshipRoleName();
        multiplicityOne = defaultValues.isMultiplicityOne();
        cascadeDelete = defaultValues.isCascadeDelete();

        cmrFieldName = defaultValues.getCMRFieldName();
        navigable = defaultValues.isNavigable();
        cmrFieldType = defaultValues.getCMRFieldType();

        // foreign key constraint?  If not provided, keep default.
        if (parsedRelationshipRole.getForeignKeyConstraint() != null) {
            foreignKeyConstraint = parsedRelationshipRole.getForeignKeyConstraint();
        } else {
            foreignKeyConstraint = defaultValues.hasForeignKeyConstraint();
        }

        // read-ahead
        if (parsedRelationshipRole.getReadAhead() != null) {
            readAhead = new JDBCReadAheadMetaData(parsedRelationshipRole.getReadAhead(), entity.getReadAhead());
        } else {
            readAhead = entity.getReadAhead();
        }
        batchCascadeDelete = parsedRelationshipRole.getBatchCascadeDelete() != null && parsedRelationshipRole.getBatchCascadeDelete();
        if (batchCascadeDelete) {
            if (!cascadeDelete)
                throw new RuntimeException(
                        relationMetaData.getRelationName() + '/' + relationshipRoleName
                                + " has batch-cascade-delete in jbosscmp-jdbc.xml but has no cascade-delete in ejb-jar.xml"
                );

            if (relationMetaData.isTableMappingStyle()) {
                throw new RuntimeException(
                        "Relationship " + relationMetaData.getRelationName()
                                + " with relation-table-mapping style was setup for batch cascade-delete."
                                + " Batch cascade-delete supported only for foreign key mapping style."
                );
            }
        }
    }

    public void init(final JDBCRelationshipRoleMetaData relatedRole) {
        this.relatedRole = relatedRole;
        keyFields.putAll(loadKeyFields());
    }

    public void init(final JDBCRelationshipRoleMetaData relatedRole, final ParsedRelationshipRole parsedRole) {
        this.relatedRole = relatedRole;
        keyFields.putAll(loadKeyFields(parsedRole));
    }


    /**
     * Gets the relation to which this role belongs.
     */
    public JDBCRelationMetaData getRelationMetaData() {
        return relationMetaData;
    }

    /**
     * Gets the name of this role.
     */
    public String getRelationshipRoleName() {
        return relationshipRoleName;
    }

    /**
     * Should this role use a foreign key constraint.
     *
     * @return true if the store mananager will execute an ALTER TABLE ADD
     *         CONSTRAINT statement to add a foreign key constraint.
     */
    public boolean hasForeignKeyConstraint() {
        return foreignKeyConstraint;
    }

    public boolean isBatchCascadeDelete() {
        return batchCascadeDelete;
    }

    /**
     * Gets the read ahead meta data
     */
    public JDBCReadAheadMetaData getReadAhead() {
        return readAhead;
    }

    public JDBCEntityMetaData getEntity() {
        return entity;
    }

    /**
     * Gets the key fields of this role.
     *
     * @return an unmodifiable collection of JDBCCMPFieldMetaData objects
     */
    public Collection<JDBCCMPFieldMetaData> getKeyFields() {
        return Collections.unmodifiableCollection(keyFields.values());
    }

    public boolean isIndexed() {
        return genIndex;
    }

    private static String generateNonNavigableCMRName(final RelationRoleMetaData role) {
        RelationRoleMetaData relatedRole = role.getRelatedRole();
        return relatedRole.getRoleSource().getEjbName() + "_" + relatedRole.getCmrField().getCmrFieldName();
    }

    /**
     * Checks if the multiplicity is one.
     */
    public boolean isMultiplicityOne() {
        return multiplicityOne;
    }

    /**
     * Checks if the multiplicity is many.
     */
    public boolean isMultiplicityMany() {
        return !multiplicityOne;
    }

    /**
     * Should this entity be deleted when related entity is deleted.
     */
    public boolean isCascadeDelete() {
        return cascadeDelete;
    }

    /**
     * Gets the name of the entity's cmr field for this role.
     */
    public String getCMRFieldName() {
        return cmrFieldName;
    }

    public boolean isNavigable() {
        return navigable;
    }

    /**
     * Gets the type of the cmr field (i.e., collection or set)
     */
    public String getCMRFieldType() {
        return cmrFieldType;
    }

    /**
     * Gets the related role's jdbc meta data.
     */
    public JDBCRelationshipRoleMetaData getRelatedRole() {
        return relationMetaData.getOtherRelationshipRole(this);
    }

    /**
     * Loads the key fields for this role based on the primary keys of the
     * this entity.
     */
    private Map<String, JDBCCMPFieldMetaData> loadKeyFields() {
        // with foreign key mapping, foreign key fields are no added if
        // - it is the many side of one-to-many relationship
        // - it is the one side of one-to-one relationship and related side is not navigable
        if (relationMetaData.isForeignKeyMappingStyle()) {
            if (isMultiplicityMany())
                return Collections.emptyMap();
            else if (getRelatedRole().isMultiplicityOne() && !getRelatedRole().isNavigable())
                return Collections.emptyMap();
        }

        // get all of the pk fields
        List<JDBCCMPFieldMetaData> pkFields = new ArrayList<JDBCCMPFieldMetaData>();
        for (JDBCCMPFieldMetaData cmpField : entity.getCMPFields()) {
            if (cmpField.isPrimaryKeyMember()) {
                pkFields.add(cmpField);
            }
        }

        // generate a new key field for each pk field
        Map<String, JDBCCMPFieldMetaData> fields = new HashMap<String, JDBCCMPFieldMetaData>(pkFields.size());
        for (JDBCCMPFieldMetaData cmpField : pkFields) {
            String columnName;
            if (relationMetaData.isTableMappingStyle()) {
                if (entity.equals(relatedRole.getEntity()))
                    columnName = getCMRFieldName();
                else
                    columnName = entity.getName();
            } else {
                columnName = relatedRole.getCMRFieldName();
            }

            if (pkFields.size() > 1) {
                columnName += "_" + cmpField.getFieldName();
            }

            genIndex = (genIndex) || cmpField.isIndexed();

            cmpField = new JDBCCMPFieldMetaData(
                    entity,
                    cmpField,
                    columnName,
                    false,
                    relationMetaData.isTableMappingStyle(),
                    relationMetaData.isReadOnly(),
                    relationMetaData.getReadTimeOut(),
                    relationMetaData.isTableMappingStyle());
            fields.put(cmpField.getFieldName(), cmpField);
        }
        return Collections.unmodifiableMap(fields);
    }

    private Map<String, JDBCCMPFieldMetaData> loadKeyFields(final ParsedRelationshipRole parsedRole) {

        // no field overrides, we're done
        final List<ParsedCmpField> keyFields = parsedRole.getKeyFields();
        if (keyFields == null) {
            return loadKeyFields();
        }

        if(keyFields.isEmpty()) {
            return Collections.EMPTY_MAP;
        }

        if (relationMetaData.isForeignKeyMappingStyle() && isMultiplicityMany()) {
            throw new RuntimeException("Role: " + relationshipRoleName + " with multiplicity many using " +
                    "foreign-key mapping is not allowed to have key-fields");
        }

        // load the default field values
        Map<String, JDBCCMPFieldMetaData> defaultFields = getPrimaryKeyFields();

        // load overrides
        Map<String, JDBCCMPFieldMetaData> fields = new HashMap<String, JDBCCMPFieldMetaData>(defaultFields.size());
        for (ParsedCmpField keyField : keyFields) {
            String fieldName = keyField.getFieldName();

            JDBCCMPFieldMetaData cmpField = defaultFields.remove(fieldName);
            if (cmpField == null) {
                throw new RuntimeException("Role '" + relationshipRoleName + "' on Entity Bean '" + entity.getName() + "' : CMP field for key not found: field " + "name='" + fieldName + "'");
            }
            genIndex = keyField.getGenIndex() != null && keyField.getGenIndex();


            cmpField = new JDBCCMPFieldMetaData(
                    entity,
                    keyField,
                    cmpField,
                    false,
                    relationMetaData.isTableMappingStyle(),
                    relationMetaData.isReadOnly(),
                    relationMetaData.getReadTimeOut(),
                    relationMetaData.isTableMappingStyle());
            fields.put(cmpField.getFieldName(), cmpField);
        }

        // all fields must be overriden
        if (!defaultFields.isEmpty()) {
            throw new RuntimeException("Mappings were not provided for all " +
                    "fields: unmaped fields=" + defaultFields.keySet() +
                    " in role=" + relationshipRoleName);
        }
        return Collections.unmodifiableMap(fields);
    }

    /**
     * Returns the primary key fields of the entity mapped by field name.
     */
    private Map<String, JDBCCMPFieldMetaData> getPrimaryKeyFields() {
        Map<String, JDBCCMPFieldMetaData> pkFields = new HashMap<String, JDBCCMPFieldMetaData>();
        for (JDBCCMPFieldMetaData cmpField : entity.getCMPFields()) {
            if (cmpField.isPrimaryKeyMember())
                pkFields.put(cmpField.getFieldName(), cmpField);
        }
        return pkFields;
    }
}
