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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jboss.as.cmp.CmpMessages;
import org.jboss.as.cmp.jdbc.metadata.parser.ParsedApplication;
import org.jboss.as.cmp.jdbc.metadata.parser.ParsedEntity;
import org.jboss.as.cmp.jdbc.metadata.parser.ParsedRelationship;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;
import org.jboss.metadata.ejb.spec.EnterpriseBeanMetaData;
import org.jboss.metadata.ejb.spec.EntityBeanMetaData;
import org.jboss.metadata.ejb.spec.RelationMetaData;

/**
 * This class contains information about the application
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @author <a href="sebastien.alborini@m4x.org">Sebastien Alborini</a>
 * @author <a href="alex@jboss.org">Alexey Loubyansky</a>
 * @version $Revision: 81030 $
 */
public final class JDBCApplicationMetaData {
    /**
     * The class loader for this application.  The class loader is used to
     * load all classes used by this application.
     */
    private final ClassLoader classLoader;

    /**
     * Map with user defined type mapping, e.g. enum mappings
     */
    private final Map<String, JDBCUserTypeMappingMetaData> userTypeMappings = new HashMap<String, JDBCUserTypeMappingMetaData>();

    /**
     * Map of the type mappings by name.
     */
    private final Map<String, JDBCTypeMappingMetaData> typeMappings = new HashMap<String, JDBCTypeMappingMetaData>();

    /**
     * Map of the entities managed by jbosscmp-jdbc by bean name.
     */
    private final Map<String, JDBCEntityMetaData> entities = new HashMap<String, JDBCEntityMetaData>();

    /**
     * Collection of relations in this application.
     */
    private final Map<String, JDBCRelationMetaData> relationships = new HashMap<String, JDBCRelationMetaData>();

    /**
     * Map of the collection relationship roles for each entity by entity object.
     */
    private final Map<String, Set<JDBCRelationshipRoleMetaData>> entityRoles = new HashMap<String, Set<JDBCRelationshipRoleMetaData>>();

    /**
     * Map of the dependent value classes by java class type.
     */
    private final Map<Class<?>, JDBCValueClassMetaData> valueClasses = new HashMap<Class<?>, JDBCValueClassMetaData>();

    /**
     * Map from abstract schema name to entity name
     */
    private final Map<String, JDBCEntityMetaData> entitiesByAbstractSchemaName = new HashMap<String, JDBCEntityMetaData>();

    /**
     * Map from entity interface(s) java type to entity name
     */
    private final Map<Class<?>, JDBCEntityMetaData> entitiesByInterface = new HashMap<Class<?>, JDBCEntityMetaData>();

    /**
     * Map of the entity commands by name.
     */
    private final Map<String, JDBCEntityCommandMetaData> entityCommands = new HashMap<String, JDBCEntityCommandMetaData>();

    private JDBCEntityMetaData defaultEntity;

    public JDBCApplicationMetaData(final EjbJarMetaData ejbJarMetaData, final ClassLoader classLoader) {
        this.classLoader = classLoader;

        defaultEntity = new JDBCEntityMetaData(this);

        for (EnterpriseBeanMetaData bean : ejbJarMetaData.getEnterpriseBeans()) {
            // only take entities
            if (bean.isEntity()) {
                final EntityBeanMetaData entity = EntityBeanMetaData.class.cast(bean);
                if (entity.isCMP()) {
                    JDBCEntityMetaData jdbcEntity = new JDBCEntityMetaData(this, entity);

                    entities.put(entity.getEjbName(), jdbcEntity);

                    String schemaName = jdbcEntity.getAbstractSchemaName();
                    if (schemaName != null) {
                        entitiesByAbstractSchemaName.put(schemaName, jdbcEntity);
                    }

                    final Class<?> remote = jdbcEntity.getRemoteClass();
                    if (remote != null) {
                        entitiesByInterface.put(remote, jdbcEntity);
                    }

                    final Class<?> local = jdbcEntity.getLocalClass();
                    if (local != null) {
                        entitiesByInterface.put(local, jdbcEntity);
                    }

                    // initialized the entity roles collection
                    entityRoles.put(entity.getEjbName(), new HashSet<JDBCRelationshipRoleMetaData>());
                }
            }
        }

        // relationships
        if (ejbJarMetaData.getRelationships() != null)
            for (RelationMetaData relationMetaData : ejbJarMetaData.getRelationships()) {
                // Relationship metadata
                JDBCRelationMetaData jdbcRelation = new JDBCRelationMetaData(this, relationMetaData);
                relationships.put(jdbcRelation.getRelationName(), jdbcRelation);

                // Left relationship-role metadata
                JDBCRelationshipRoleMetaData left = jdbcRelation.getLeftRelationshipRole();
                Set<JDBCRelationshipRoleMetaData> leftEntityRoles = entityRoles.get(left.getEntity().getName());
                leftEntityRoles.add(left);

                // Right relationship-role metadata
                JDBCRelationshipRoleMetaData right = jdbcRelation.getRightRelationshipRole();
                Set<JDBCRelationshipRoleMetaData> rightEntityRoles = entityRoles.get(right.getEntity().getName());
                rightEntityRoles.add(right);
            }

    }

    public JDBCApplicationMetaData(ParsedApplication parsed, JDBCApplicationMetaData defaultValues) {
        classLoader = defaultValues.classLoader;

        if (parsed.getDefaultEntity() != null) {
            defaultEntity = new JDBCEntityMetaData(this, parsed.getDefaultEntity(), defaultValues.getDefaultEntity());
        } else {
            defaultEntity = new JDBCEntityMetaData(this, defaultValues.getDefaultEntity());
        }

        if (parsed.getUserTypeMappings() != null) {
            for (JDBCUserTypeMappingMetaData userTypeMapping : parsed.getUserTypeMappings()) {
                userTypeMappings.put(userTypeMapping.getJavaType(), userTypeMapping);
            }
        } else {
            userTypeMappings.putAll(defaultValues.userTypeMappings);
        }

        // type-mappings: (optional, always set in standardjbosscmp-jdbc.xml)
        typeMappings.putAll(defaultValues.typeMappings);
        if (parsed.getTypeMappings() != null) {
            for (JDBCTypeMappingMetaData typeMapping : parsed.getTypeMappings()) {
                typeMappings.put(typeMapping.getName(), typeMapping);
            }
        }

        // dependent-value-objects
        valueClasses.putAll(defaultValues.valueClasses);
        if (parsed.getValueClasses() != null) {
            for (JDBCValueClassMetaData valueClass : parsed.getValueClasses()) {
                valueClasses.put(valueClass.getJavaType(), valueClass);
            }
        }

        // entity-commands: (optional, always set in standardjbosscmp-jdbc.xml)
        entityCommands.putAll(defaultValues.entityCommands);
        if (parsed.getEntityCommands() != null) {
            for (JDBCEntityCommandMetaData entityCommand : parsed.getEntityCommands()) {
                entityCommands.put(entityCommand.getCommandName(), entityCommand);
            }
        }

        // defaults: apply defaults for entities (optional, always
        // set in standardjbosscmp-jdbc.xml)
        entities.putAll(defaultValues.entities);
        entitiesByAbstractSchemaName.putAll(defaultValues.entitiesByAbstractSchemaName);
        entitiesByInterface.putAll(defaultValues.entitiesByInterface);
        if (parsed.getDefaultEntity() != null) {
            final List<JDBCEntityMetaData> values = new ArrayList<JDBCEntityMetaData>(entities.values());
            for (JDBCEntityMetaData entityMetaData : values) {
                // create the new metadata with the defaults applied
                entityMetaData = new JDBCEntityMetaData(this, parsed.getDefaultEntity(), entityMetaData);

                // replace the old meta data with the new
                entities.put(entityMetaData.getName(), entityMetaData);

                String schemaName = entityMetaData.getAbstractSchemaName();
                if (schemaName != null) {
                    entitiesByAbstractSchemaName.put(schemaName, entityMetaData);
                }

                Class<?> remote = entityMetaData.getRemoteClass();
                if (remote != null) {
                    entitiesByInterface.put(remote, entityMetaData);
                }

                Class<?> local = entityMetaData.getLocalClass();
                if (local != null) {
                    entitiesByInterface.put(local, entityMetaData);
                }
            }
        }

        // enterprise-beans: apply entity specific configuration
        // (only in jbosscmp-jdbc.xml)
        if (parsed.getEntities() != null) {
            for (ParsedEntity parsedEntity : parsed.getEntities()) {
                // get entity by name, if not found, it is a config error
                String ejbName = parsedEntity.getEntityName();
                JDBCEntityMetaData entityMetaData = getBeanByEjbName(ejbName);

                if (entityMetaData == null) {
                    throw CmpMessages.MESSAGES.entityNotFoundInEjbJarXml(ejbName);
                }
                entityMetaData = new JDBCEntityMetaData(this, parsedEntity, entityMetaData);
                entities.put(entityMetaData.getName(), entityMetaData);

                String schemaName = entityMetaData.getAbstractSchemaName();
                if (schemaName != null) {
                    entitiesByAbstractSchemaName.put(schemaName, entityMetaData);
                }

                Class<?> remote = entityMetaData.getRemoteClass();
                if (remote != null) {
                    entitiesByInterface.put(remote, entityMetaData);
                }

                Class<?> local = entityMetaData.getLocalClass();
                if (local != null) {
                    entitiesByInterface.put(local, entityMetaData);
                }
            }
        }

        // defaults: apply defaults for relationships (optional, always
        // set in standardjbosscmp-jdbc.xml)
        if (parsed.getDefaultEntity() == null) {
            // no defaults just copy over the existing relationships and roles
            relationships.putAll(defaultValues.relationships);
            entityRoles.putAll(defaultValues.entityRoles);
        } else {
            // create a new empty role collection for each entity
            for (JDBCEntityMetaData entity : entities.values()) {
                entityRoles.put(entity.getName(), new HashSet<JDBCRelationshipRoleMetaData>());
            }

            // for each relationship, apply defaults and store
            for (JDBCRelationMetaData relationMetaData : defaultValues.relationships.values()) {
                // create the new metadata with the defaults applied
                relationMetaData = new JDBCRelationMetaData(this, parsed.getDefaultEntity(), relationMetaData);

                // replace the old metadata with the new
                relationships.put(relationMetaData.getRelationName(), relationMetaData);

                // store new left role
                JDBCRelationshipRoleMetaData left = relationMetaData.getLeftRelationshipRole();
                Set<JDBCRelationshipRoleMetaData> leftEntityRoles = entityRoles.get(left.getEntity().getName());
                leftEntityRoles.add(left);

                // store new right role
                JDBCRelationshipRoleMetaData right =
                        relationMetaData.getRightRelationshipRole();
                Set<JDBCRelationshipRoleMetaData> rightEntityRoles = entityRoles.get(right.getEntity().getName());
                rightEntityRoles.add(right);
            }
        }

        // relationships: apply entity specific configuration
        // (only in jbosscmp-jdbc.xml)
        if (parsed.getRelationships() != null) {
            for (ParsedRelationship parsedRelationship : parsed.getRelationships()) {
                // get relation by name, if not found, it is a config error
                String relationName = parsedRelationship.getRelationName();
                JDBCRelationMetaData oldRelation = relationships.get(relationName);

                if (oldRelation == null) {
                    throw CmpMessages.MESSAGES.relationNotFoundInEjbJarXml(relationName);
                }
                // create new metadata with relation specific config applied
                JDBCRelationMetaData newRelation = new JDBCRelationMetaData(this, parsedRelationship, oldRelation);

                // replace the old metadata with the new
                relationships.put(newRelation.getRelationName(), newRelation);

                // replace the old left role with the new
                JDBCRelationshipRoleMetaData newLeft = newRelation.getLeftRelationshipRole();
                Set<JDBCRelationshipRoleMetaData> leftEntityRoles = entityRoles.get(newLeft.getEntity().getName());
                leftEntityRoles.remove(oldRelation.getLeftRelationshipRole());
                leftEntityRoles.add(newLeft);

                // replace the old right role with the new
                JDBCRelationshipRoleMetaData newRight =
                        newRelation.getRightRelationshipRole();
                Set<JDBCRelationshipRoleMetaData> rightEntityRoles = entityRoles.get(newRight.getEntity().getName());
                rightEntityRoles.remove(oldRelation.getRightRelationshipRole());
                rightEntityRoles.add(newRight);
            }
        }
    }

    /**
     * Gets the type mapping with the specified name
     *
     * @param name the name for the type mapping
     * @return the matching type mapping or null if not found
     */
    public JDBCTypeMappingMetaData getTypeMappingByName(String name) {
        return typeMappings.get(name);
    }

    /**
     * Gets the relationship roles for the entity with the specified name.
     *
     * @param entityName the name of the entity whose roles are returned
     * @return an unmodifiable collection of JDBCRelationshipRoles
     *         of the specified entity
     */
    public Collection<JDBCRelationshipRoleMetaData> getRolesForEntity(String entityName) {
        Collection<JDBCRelationshipRoleMetaData> roles = entityRoles.get(entityName);
        return Collections.unmodifiableCollection(roles);
    }

    /**
     * Gets dependent value classes that are directly managed by the container.
     *
     * @returns an unmodifiable collection of JDBCValueClassMetaData
     */
    public Collection<JDBCValueClassMetaData> getValueClasses() {
        return Collections.unmodifiableCollection(valueClasses.values());
    }

    /**
     * Gets the metadata for an entity bean by name.
     *
     * @param name the name of the entity meta data to return
     * @return the entity meta data for the specified name
     */
    public JDBCEntityMetaData getBeanByEjbName(String name) {
        return entities.get(name);
    }

    /**
     * Gets the entity command with the specified name
     *
     * @param name the name for the entity-command
     * @return the matching entity command or null if not found
     */
    public JDBCEntityCommandMetaData getEntityCommandByName(final String name) {
        return entityCommands.get(name);
    }

    public Map<String, JDBCUserTypeMappingMetaData> getUserTypeMappings() {
        return Collections.unmodifiableMap(userTypeMappings);
    }

    public void addTypeMapping(final JDBCTypeMappingMetaData metaData) {
        typeMappings.put(metaData.getName(), metaData);
    }

    public void addEntityCommand(final JDBCEntityCommandMetaData entityCommand) {
        this.entityCommands.put(entityCommand.getCommandName(), entityCommand);
    }

    public void addRelationship(final JDBCRelationMetaData relationMetaData) {
        final JDBCRelationMetaData oldRelation = this.relationships.put(relationMetaData.getRelationName(), relationMetaData);
        Set<JDBCRelationshipRoleMetaData> leftRoles = this.entityRoles.get(relationMetaData.getLeftRelationshipRole().getRelationshipRoleName());
        if (leftRoles == null) {
            leftRoles = new HashSet<JDBCRelationshipRoleMetaData>();
            entityRoles.put(relationMetaData.getLeftRelationshipRole().getRelationshipRoleName(), leftRoles);
        }
        Set<JDBCRelationshipRoleMetaData> rightRoles = this.entityRoles.get(relationMetaData.getRightRelationshipRole().getRelationshipRoleName());
        if (rightRoles == null) {
            rightRoles = new HashSet<JDBCRelationshipRoleMetaData>();
            entityRoles.put(relationMetaData.getRightRelationshipRole().getRelationshipRoleName(), rightRoles);
        }
        if (oldRelation != null) {
            leftRoles.remove(oldRelation.getLeftRelationshipRole());
            rightRoles.remove(oldRelation.getRightRelationshipRole());
        }
        leftRoles.add(relationMetaData.getLeftRelationshipRole());
        rightRoles.add(relationMetaData.getRightRelationshipRole());
    }

    public void addEntity(final JDBCEntityMetaData entityMetaData) {
        this.entities.put(entityMetaData.getName(), entityMetaData);
        if (entityMetaData.getRemoteClass() != null) {
            this.entitiesByInterface.put(entityMetaData.getRemoteClass(), entityMetaData);
        }
        if (entityMetaData.getLocalClass() != null) {
            this.entitiesByInterface.put(entityMetaData.getLocalClass(), entityMetaData);
        }
    }

    public void addValueClass(final JDBCValueClassMetaData jdbcValueClassMetaData) {
        this.valueClasses.put(jdbcValueClassMetaData.getJavaType(), jdbcValueClassMetaData);
    }

    public void addUserTypeMapping(final JDBCUserTypeMappingMetaData jdbcUserTypeMappingMetaData) {
        this.userTypeMappings.put(jdbcUserTypeMappingMetaData.getJavaType(), jdbcUserTypeMappingMetaData);
    }

    public void addTypeMappings(final Collection<JDBCTypeMappingMetaData> jdbcTypeMappingMetaDatas) {
        for (JDBCTypeMappingMetaData typeMappingMetaData : jdbcTypeMappingMetaDatas) {
            addTypeMapping(typeMappingMetaData);
        }
    }

    public Collection<JDBCTypeMappingMetaData> getTypeMappings() {
        return typeMappings.values();
    }

    public void addUserTypeMappings(final List<JDBCUserTypeMappingMetaData> userTypeMappings) {
        for (JDBCUserTypeMappingMetaData userTypeMappingMetaData : userTypeMappings) {
            addUserTypeMapping(userTypeMappingMetaData);
        }
    }

    public void setUserTypeMappings(final Map<String, JDBCUserTypeMappingMetaData> userTypeMappings) {
        this.userTypeMappings.putAll(userTypeMappings);
    }

    public void addValueClasses(final Collection<JDBCValueClassMetaData> valueClasses) {
        for (JDBCValueClassMetaData valueClass : valueClasses) {
            addValueClass(valueClass);
        }
    }

    public Map<String, JDBCEntityCommandMetaData> getEntityCommands() {
        return Collections.unmodifiableMap(entityCommands);
    }

    public void addEntityCommands(final Map<String, JDBCEntityCommandMetaData> commands) {
        this.entityCommands.putAll(commands);
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public Collection<JDBCEntityMetaData> getBeans() {
        return entities.values();
    }

    public JDBCRelationMetaData getRelationship(String relationName) {
        return this.relationships.get(relationName);
    }

    public JDBCEntityMetaData getDefaultEntity() {
        return defaultEntity;
    }

    public void setDefaultEntity(JDBCEntityMetaData defaultEntity) {
        this.defaultEntity = defaultEntity;
    }
}
