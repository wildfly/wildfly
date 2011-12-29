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

package org.jboss.as.cmp.jdbc.metadata.parser;

import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.jboss.as.cmp.jdbc.SQLUtil;
import org.jboss.as.cmp.jdbc.metadata.JDBCApplicationMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCCMPFieldMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCCMPFieldPropertyMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCEntityCommandMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCFunctionMappingMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCLeftJoinMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCMappingMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCOptimisticLockingMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCQueryMetaDataFactory;
import org.jboss.as.cmp.jdbc.metadata.JDBCRelationMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCTypeMappingMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCUserTypeMappingMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCValueClassMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCValuePropertyMetaData;
import org.jboss.metadata.parser.util.MetaDataElementParser;

/**
 * @author John Bailey
 */
public class JDBCMetaDataParser extends MetaDataElementParser {

    public static JDBCApplicationMetaData parse(final XMLStreamReader reader, final JDBCApplicationMetaData defaults) throws XMLStreamException {
        moveToStart(reader);

        final ParsedApplication application = new ParsedApplication();

        for (Element element : children(reader)) {
            switch (element) {
                case DEFAULTS: {
                    application.defaultEntity = parseEntity(reader, defaults.getClassLoader());
                    break;
                }
                case RELATIONSHIPS: {
                    application.relationships = parseRelationships(reader);
                    break;
                }
                case ENTERPRISE_BEANS: {
                    application.entities = parseEnterpriseBeans(reader, defaults.getClassLoader());
                    break;
                }
                case TYPE_MAPPINGS: {
                    application.typeMappings = parseTypeMappings(reader);
                    break;
                }
                case ENTITY_COMMANDS: {
                    application.entityCommands = parseEntityCommands(reader, defaults.getClassLoader());
                    break;
                }
                case DEPENDENT_VALUE_CLASSES: {
                    application.valueClasses = parseDependentValueClasses(reader, defaults.getClassLoader());
                    break;
                }
                case USER_TYPE_MAPPINGS: {
                    application.userTypeMappings = parseUserTypeMappings(reader);
                    break;
                }
                case RESERVED_WORDS: {
                    parseReservedWords(reader);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

        return new JDBCApplicationMetaData(application, defaults);
    }

    private static List<JDBCTypeMappingMetaData> parseTypeMappings(final XMLStreamReader reader) throws XMLStreamException {
        final List<JDBCTypeMappingMetaData> typeMappings = new ArrayList<JDBCTypeMappingMetaData>();
        for (Element element : children(reader)) {
            switch (element) {
                case TYPE_MAPPING: {
                    typeMappings.add(parseTypeMapping(reader));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return typeMappings;
    }

    private static JDBCTypeMappingMetaData parseTypeMapping(final XMLStreamReader reader) throws XMLStreamException {

        final JDBCTypeMappingMetaData metaData = new JDBCTypeMappingMetaData();
        for (Element element : children(reader)) {
            switch (element) {
                case NAME: {
                    metaData.setName(getElementText(reader));
                    break;
                }
                case ADD_COLUMN_TEMPLATE: {
                    final String value = getElementText(reader);
                    if (!isEmpty(value)) {
                        metaData.setAddColomnTemplate(new JDBCFunctionMappingMetaData("add-column-template", value));
                    } else {
                        metaData.setAddColomnTemplate(new JDBCFunctionMappingMetaData("add-column-template", "ALTER TABLE ?1 ADD ?2 ?3"));
                    }
                    break;
                }
                case ALIAS_HEADER_PREFIX: {
                    final String value = getElementText(reader);
                    if (!isEmpty(value)) {
                        metaData.setAliasHeaderPrefix(value);
                    }
                    break;
                }
                case ALIAS_HEADER_SUFFIX: {
                    final String value = getElementText(reader);
                    if (!isEmpty(value)) {
                        metaData.setAliasHeaderSuffix(value);
                    }
                    break;
                }
                case ALIAS_MAX_LENGHT: {
                    final String value = getElementText(reader);
                    try {
                        final int aliasMaxLength = Integer.parseInt(value);
                        metaData.setAliasMaxLength(aliasMaxLength);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid number format in alias-max-length " + value + "': " + e);
                    }
                    break;
                }
                case ALTER_COLUMN_TEMPLATE: {
                    final String value = getElementText(reader);
                    if (!isEmpty(value)) {
                        metaData.setAlterColomnTemplate(new JDBCFunctionMappingMetaData("add-column-template", value));
                    } else {
                        metaData.setAlterColomnTemplate(new JDBCFunctionMappingMetaData("add-column-template", "ALTER TABLE ?1 ADD ?2 ?3"));
                    }
                    break;
                }
                case AUTO_INCREMENT_TEMPLATE: {
                    final String value = getElementText(reader);
                    if (!isEmpty(value)) {
                        metaData.setAutoIncrementTemplate(new JDBCFunctionMappingMetaData("auto-increment", value));
                    }
                    break;
                }
                case DROP_COLUMN_TEMPLATE: {
                    final String value = getElementText(reader);
                    if (!isEmpty(value)) {
                        metaData.setDropColomnTemplate(new JDBCFunctionMappingMetaData("drop-column-template", value));
                    } else {
                        metaData.setDropColomnTemplate(new JDBCFunctionMappingMetaData("drop-column-template", "ALTER TABLE ?1 DROP ?2"));
                    }
                    break;
                }
                case FALSE_MAPPING: {
                    final String value = getElementText(reader);
                    if (!isEmpty(value)) {
                        metaData.setFalseMapping(value);
                    }
                    break;
                }
                case FK_CONSTRAINT_TEMPLATE: {
                    final String value = getElementText(reader);
                    if (!isEmpty(value)) {
                        metaData.setFKConstraintTemplate(new JDBCFunctionMappingMetaData("fk-constraint", value));
                    }
                    break;
                }
                case FUNCTION_MAPPING: {
                    metaData.addFunctionMapping(parseFuctionMapping(reader));
                    break;
                }
                case MAPPING: {
                    metaData.addMapping(parseMapping(reader));
                    break;
                }
                case MAX_KEYS_IN_DELETE: {
                    final String value = getElementText(reader);
                    try {
                        final int maxKeys = Integer.parseInt(value);
                        metaData.setMaxKeysInDelete(maxKeys);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid number format in max-keys-in-delete " + value + "': " + e);
                    }
                    break;
                }
                case PK_CONSTRAINT_TEMPLATE: {
                    final String value = getElementText(reader);
                    if (!isEmpty(value)) {
                        metaData.setPKConstraintTemplate(new JDBCFunctionMappingMetaData("pk-constraint", value));
                    }
                    break;
                }
                case ROW_LOCKING_TEMPLATE: {
                    final String value = getElementText(reader);
                    if (!isEmpty(value)) {
                        metaData.setRowLockingTemplate(new JDBCFunctionMappingMetaData("row-locking", value));
                    }
                    break;
                }
                case SUBQUERY_SUPPORTED: {
                    metaData.setSubQuerySupported(Boolean.valueOf(getElementText(reader)));
                    break;
                }
                case TRUE_MAPPING: {
                    final String value = getElementText(reader);
                    if (!isEmpty(value)) {
                        metaData.setTrueMapping(value);
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

        return metaData;
    }

    private static JDBCFunctionMappingMetaData parseFuctionMapping(final XMLStreamReader reader) throws XMLStreamException {

        final JDBCFunctionMappingMetaData metaData = new JDBCFunctionMappingMetaData();
        for (Element element : children(reader)) {
            switch (element) {
                case FUNCTION_NAME: {
                    metaData.setFunctionName(getElementText(reader));
                    break;
                }
                case FUNCTION_SQL: {
                    metaData.setFunctionSql(getElementText(reader));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return metaData;
    }

    private static JDBCMappingMetaData parseMapping(final XMLStreamReader reader) throws XMLStreamException {

        final JDBCMappingMetaData metaData = new JDBCMappingMetaData();
        for (Element element : children(reader)) {
            switch (element) {
                case JAVA_TYPE: {
                    metaData.setJavaType(getElementText(reader));
                    break;
                }
                case JDBC_TYPE: {
                    metaData.setJdbcType(getJdbcTypeFromName(getElementText(reader)));
                    break;
                }
                case SQL_TYPE: {
                    metaData.setSqlType(getElementText(reader));
                    break;
                }
                case PARAM_SETTER: {
                    metaData.setParamSetter(getElementText(reader));
                    break;
                }
                case RESULT_READER: {
                    metaData.setResultReader(getElementText(reader));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return metaData;
    }

    private static ParsedReadAhead parseReadAhead(final XMLStreamReader reader) throws XMLStreamException {
        final ParsedReadAhead metaData = new ParsedReadAhead();
        final List<JDBCLeftJoinMetaData> leftJoins = new ArrayList<JDBCLeftJoinMetaData>();
        for (Element element : children(reader)) {
            switch (element) {
                case STRATEGY: {
                    metaData.strategy = getElementText(reader);
                    break;
                }
                case PAGE_SIZE: {
                    metaData.pageSize = Integer.parseInt(getElementText(reader));
                    break;
                }
                case EAGER_LOAD_GROUP: {
                    metaData.eagerLoadGroup = getElementText(reader);
                    break;
                }
                case LEFT_JOIN: {
                    leftJoins.add(parseLeftJoin(reader));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        metaData.leftJoinList = leftJoins;
        return metaData;
    }

    private static JDBCLeftJoinMetaData parseLeftJoin(XMLStreamReader reader) throws XMLStreamException {
        final JDBCLeftJoinMetaData metaData = new JDBCLeftJoinMetaData();
        final List<JDBCLeftJoinMetaData> leftJoins = new ArrayList<JDBCLeftJoinMetaData>();

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case CMR_FIELD: {
                    metaData.setCmrField(reader.getAttributeValue(i));
                    break;
                }
                case EAGER_LOAD_GROUP: {
                    metaData.setEagerLoadGroup(reader.getAttributeValue(i));
                    break;
                }
                default: {
                    throw unexpectedAttribute(reader, i);
                }
            }
        }

        for (Element element : children(reader)) {
            switch (element) {
                case LEFT_JOIN: {
                    leftJoins.add(parseLeftJoin(reader));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        metaData.setLeftJoins(leftJoins);
        return metaData;
    }

    private static ParsedCmpField parseUnknownPk(final XMLStreamReader reader, final ClassLoader classLoader) throws XMLStreamException {
        final ParsedCmpField parsedCmpField = new ParsedCmpField();

        for (Element element : children(reader)) {
            switch (element) {
                case UNKNOWN_KEY_CLASS: {
                    try {
                        parsedCmpField.unknownPk = classLoader.loadClass(getElementText(reader));
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("Failed to load field type", e);
                    }
                    break;
                }
                case FIELD_NAME: {
                    parsedCmpField.fieldName = getElementText(reader);
                    break;
                }
                case READ_ONLY: {
                    parsedCmpField.readOnly = Boolean.parseBoolean(getElementText(reader));
                    break;
                }
                case READ_TIMEOUT: {
                    parsedCmpField.readTimeOut = Integer.parseInt(getElementText(reader));
                    break;
                }
                case COLUMN_NAME: {
                    parsedCmpField.columnName = getElementText(reader);
                    break;
                }
                case JDBC_TYPE: {
                    parsedCmpField.jdbcType = getJdbcTypeFromName(getElementText(reader));
                    break;
                }
                case SQL_TYPE: {
                    parsedCmpField.sqlType = getElementText(reader);
                    break;
                }
                case AUTO_INCREMENT: {
                    parsedCmpField.autoIncrement = Boolean.parseBoolean(getElementText(reader));
                    break;
                }
                case KEY_GENERATOR_FACTORY: {
                    getElementText(reader); // TODO: Is this used?
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return parsedCmpField;
    }

    private static JDBCEntityCommandMetaData parseEntityCommand(final XMLStreamReader reader, final ClassLoader classLoader) throws XMLStreamException {

        final JDBCEntityCommandMetaData metaData = new JDBCEntityCommandMetaData();

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    metaData.setName(reader.getAttributeValue(i));
                    break;
                }
                case CLASS: {
                    try {
                        metaData.setClass(classLoader.loadClass(reader.getAttributeValue(i)));
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("Failed to load entity command class", e);
                    }

                    break;
                }
                default: {
                    throw unexpectedAttribute(reader, i);
                }
            }
        }

        for (Element element : children(reader)) {
            switch (element) {
                case ATTRIBUTE: {
                    parseAttribute(reader, metaData);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return metaData;
    }

    private static void parseAttribute(final XMLStreamReader reader, JDBCEntityCommandMetaData metaData) throws XMLStreamException {

        String name = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    name = reader.getAttributeValue(i);
                    break;
                }
                default: {
                    throw unexpectedAttribute(reader, i);
                }
            }
        }
        final String value = getElementText(reader);
        if (name != null) {
            metaData.addAttribute(name, value);
        }
    }

    private static List<JDBCEntityCommandMetaData> parseEntityCommands(final XMLStreamReader reader, final ClassLoader classLoader) throws XMLStreamException {
        final List<JDBCEntityCommandMetaData> commands = new ArrayList<JDBCEntityCommandMetaData>();
        for (Element element : children(reader)) {
            switch (element) {
                case ENTITY_COMMAND: {
                    commands.add(parseEntityCommand(reader, classLoader));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return commands;
    }

    private static List<ParsedRelationship> parseRelationships(final XMLStreamReader reader) throws XMLStreamException {
        final List<ParsedRelationship> relationships = new ArrayList<ParsedRelationship>();
        for (Element element : children(reader)) {
            switch (element) {
                case EJB_RELATION: {
                    relationships.add(parseRelationship(reader));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return relationships;
    }

    private static ParsedRelationship parseRelationship(final XMLStreamReader reader) throws XMLStreamException {
        final ParsedRelationship metaData = new ParsedRelationship();

        for (Element element : children(reader)) {
            switch (element) {
                case EJB_RELATION_NAME: {
                    metaData.relationName = getElementText(reader);
                    break;
                }
                case READ_ONLY: {
                    metaData.readOnly = Boolean.parseBoolean(getElementText(reader));
                    break;
                }
                case READ_TIMEOUT: {
                    metaData.readTimeOut = Integer.parseInt(getElementText(reader));
                    break;
                }
                case FOREIGN_KEY_MAPPING: {
                    metaData.mappingStyle = JDBCRelationMetaData.MappingStyle.FOREIGN_KEY;
                    getElementText(reader);
                    break;
                }
                case RELATION_TABLE_MAPPING: {
                    metaData.mappingStyle = JDBCRelationMetaData.MappingStyle.TABLE;
                    for (Element tableElement : children(reader)) {
                        switch (tableElement) {
                            case TABLE_NAME: {
                                metaData.tableName = getElementText(reader);
                                break;
                            }
                            case DATASOURCE: {
                                metaData.dataSourceName = getElementText(reader);
                                break;
                            }
                            case DATASOURCE_MAPPING: {
                                metaData.datasourceMapping = getElementText(reader);
                                break;
                            }
                            case CREATE_TABLE: {
                                metaData.createTable = Boolean.parseBoolean(getElementText(reader));
                                break;
                            }
                            case REMOVE_TABLE: {
                                metaData.removeTable = Boolean.parseBoolean(getElementText(reader));
                                break;
                            }
                            case ALTER_TABLE: {
                                metaData.alterTable = Boolean.parseBoolean(getElementText(reader));
                                break;
                            }
                            case POST_TABLE_CREATE: {
                                for (String cmd : parsePostTableCreate(reader)) {
                                    metaData.tablePostCreateCmd.add(cmd);
                                }
                                break;
                            }
                            case ROW_LOCKING: {
                                metaData.rowLocking = Boolean.parseBoolean(getElementText(reader));
                                break;
                            }
                            case PK_CONSTRAINT: {
                                metaData.primaryKeyConstraint = Boolean.parseBoolean(getElementText(reader));
                                break;
                            }
                            default: {
                                throw unexpectedElement(reader);
                            }
                        }
                    }
                    break;
                }
                case EJB_RELATIONSHIP_ROLE: {
                    metaData.roles.add(parseEjbRelationshipRole(reader));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

        return metaData;
    }

    private static ParsedRelationshipRole parseEjbRelationshipRole(final XMLStreamReader reader) throws XMLStreamException {
        final ParsedRelationshipRole metaData = new ParsedRelationshipRole();
        for (Element element : children(reader)) {
            switch (element) {
                case EJB_RELATIONSHIP_ROLE_NAME: {
                    metaData.relationshipRoleName = getElementText(reader);
                    break;
                }
                case FK_CONSTRAINT: {
                    metaData.foreignKeyConstraint = Boolean.parseBoolean(getElementText(reader));
                    break;
                }
                case READ_AHEAD: {
                    metaData.readAhead = parseReadAhead(reader);
                    break;
                }
                case KEY_FIELDS: {
                    metaData.keyFields = parseKeyFields(reader);
                    break;
                }
                case BATCH_CASCADE_DELETE: {
                    metaData.batchCascadeDelete = Boolean.parseBoolean(getElementText(reader));
                    break;
                }
                case CMR_FIELD: {
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return metaData;
    }

    private static List<ParsedCmpField> parseKeyFields(final XMLStreamReader reader) throws XMLStreamException {
        final List<ParsedCmpField> parsedCmpFields = new ArrayList<ParsedCmpField>();
        for (Element element : children(reader)) {
            switch (element) {
                case KEY_FIELD: {
                    parsedCmpFields.add(parseCmpField(reader));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return parsedCmpFields;
    }


    private static JDBCCMPFieldPropertyMetaData parseProperty(final XMLStreamReader reader) throws XMLStreamException {

        final JDBCCMPFieldPropertyMetaData metaData = new JDBCCMPFieldPropertyMetaData();
        for (Element element : children(reader)) {
            switch (element) {
                case PROPERTY_NAME: {
                    metaData.setPropertyName(getElementText(reader));
                    break;
                }
                case COLUMN_NAME: {
                    metaData.setColumnName(getElementText(reader));
                    break;
                }
                case NOT_NULL: {
                    metaData.setNotNul(Boolean.parseBoolean(getElementText(reader)));
                    break;
                }
                case JDBC_TYPE: {
                    metaData.setJdbcType(getJdbcTypeFromName(getElementText(reader)));
                    break;
                }
                case SQL_TYPE: {
                    metaData.setSqlType(getElementText(reader));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return metaData;
    }

    private static List<String> parsePostTableCreate(XMLStreamReader reader) throws XMLStreamException {
        final List<String> statements = new ArrayList<String>();
        for (Element element : children(reader)) {
            switch (element) {
                case SQL_STATEMENT: {
                    statements.add(getElementText(reader));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return statements;
    }

    private static List<ParsedEntity> parseEnterpriseBeans(final XMLStreamReader reader, final ClassLoader classLoader) throws XMLStreamException {
        final List<ParsedEntity> entities = new ArrayList<ParsedEntity>();
        for (Element element : children(reader)) {
            switch (element) {
                case ENTITY: {
                    entities.add(parseEntity(reader, classLoader));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return entities;
    }

    private static ParsedEntity parseEntity(final XMLStreamReader reader, final ClassLoader classLoader) throws XMLStreamException {
        final ParsedEntity metaData = new ParsedEntity();
        for (Element element : children(reader)) {
            switch (element) {
                case EJB_NAME: {
                    metaData.entityName = getElementText(reader);
                    break;
                }
                case DATASOURCE: {
                    metaData.dataSourceName = getElementText(reader);
                    break;
                }
                case DATASOURCE_MAPPING: {
                    metaData.dataSourceMappingName = getElementText(reader);
                    break;
                }
                case CREATE_TABLE: {
                    metaData.createTable = Boolean.parseBoolean(getElementText(reader));
                    break;
                }
                case REMOVE_TABLE: {
                    metaData.removeTable = Boolean.parseBoolean(getElementText(reader));
                    break;
                }
                case POST_TABLE_CREATE: {
                    for (String cmd : parsePostTableCreate(reader)) {
                        metaData.tablePostCreateCmd.add(cmd);
                    }
                    break;
                }
                case READ_ONLY: {
                    metaData.readOnly = Boolean.parseBoolean(getElementText(reader));
                    break;
                }
                case READ_TIMEOUT: {
                    metaData.readTimeOut = Integer.parseInt(getElementText(reader));
                    break;
                }
                case ROW_LOCKING: {
                    metaData.rowLocking = Boolean.parseBoolean(getElementText(reader));
                    break;
                }
                case PK_CONSTRAINT: {
                    metaData.primaryKeyConstraint = Boolean.parseBoolean(getElementText(reader));
                    break;
                }
                case FK_CONSTRAINT: {
                    metaData.preferredMappingStyle = getElementText(reader);
                    break;
                }
                case READ_AHEAD: {
                    metaData.readAhead = parseReadAhead(reader);
                    break;
                }
                case LIST_CACHE_MAX: {
                    metaData.listCacheMax = Integer.parseInt(getElementText(reader));
                    break;
                }
                case CLEAN_READ_AHEAD: {
                    metaData.cleanReadAheadOnLoad = Boolean.parseBoolean(getElementText(reader));
                    break;
                }
                case FETCH_SIZE: {
                    metaData.fetchSize = Integer.parseInt(getElementText(reader));
                    break;
                }
                case TABLE_NAME: {
                    metaData.tableName = getElementText(reader);
                    break;
                }
                case CMP_FIELD: {
                    metaData.cmpFields.add(parseCmpField(reader));
                    break;
                }
                case LOAD_GROUPS: {
                    metaData.loadGroups.putAll(parseLoadGroups(reader));
                    break;
                }
                case EAGER_LOAD_GROUP: {
                    metaData.eagerLoadGroup = getElementText(reader);
                    break;
                }
                case LAZY_LOAD_GROUPS: {
                    metaData.lazyLoadGroups.addAll(parseLazyLoadGroups(reader));
                    break;
                }
                case QUERY: {
                    metaData.queries.add(parseQuery(reader, classLoader));
                    break;
                }
                case UNKNOWN_PK: {
                    metaData.upkField = parseUnknownPk(reader, classLoader);
                    break;
                }
                case ENTITY_COMMAND: {
                    metaData.entityCommand = parseEntityCommand(reader, classLoader);
                    break;
                }
                case OPTIMISTIC_LOCKING: {
                    metaData.optimisticLocking = parseOptimisticLocking(reader, classLoader);
                    break;
                }
                case AUDIT: {
                    metaData.audit = parseAudit(reader);
                    break;
                }
                case PREFERED_RELATION: {
                    getElementText(reader); // TODO: jeb How to handle this
                    break;
                }
                case QL_COMPILER: {
                    final String qlCompiler = getElementText(reader);
                    try {
                        metaData.qlCompiler = classLoader.loadClass(qlCompiler);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("Failed to load compiler implementation: " + qlCompiler, e);
                    }
                    break;
                }
                case THROW_RUNTIME_EX: {
                    metaData.throwRuntimeExceptions = Boolean.valueOf(getElementText(reader));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return metaData;
    }

    private static List<String> parseLazyLoadGroups(XMLStreamReader reader) throws XMLStreamException {
        final List<String> groups = new ArrayList<String>();
        for (Element element : children(reader)) {
            switch (element) {
                case LOAD_GROUP_NAME: {
                    groups.add(getElementText(reader));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return groups;
    }

    private static ParsedQuery parseQuery(final XMLStreamReader reader, final ClassLoader classLoader) throws XMLStreamException {
        final ParsedQuery metaData = new ParsedQuery();
        for (Element element : children(reader)) {
            switch (element) {
                case QUERY_METHOD: {
                    for (Element queryMethodChild : children(reader)) {
                        switch (queryMethodChild) {
                            case METHOD_NAME: {
                                metaData.methodName = getElementText(reader);
                                break;
                            }
                            case METHOD_PARAMS: {
                                for (Element paramChild : children(reader)) {
                                    switch (paramChild) {
                                        case METHOD_PARAM: {
                                            metaData.methodParams.add(getElementText(reader));
                                            break;
                                        }
                                        default: {
                                            throw unexpectedElement(reader);
                                        }
                                    }
                                }
                                break;
                            }
                            default: {
                                throw unexpectedElement(reader);
                            }
                        }
                    }
                    break;
                }
                case JBOSS_QL: {
                    metaData.type = JDBCQueryMetaDataFactory.Type.JBOSS_QL;
                    metaData.query = getElementText(reader);
                    break;
                }
                case DYNAMIC_QL: {
                    metaData.type = JDBCQueryMetaDataFactory.Type.DYNAMIC_QL;
                    getElementText(reader);
                    break;
                }
                case DECLARED_QL: {
                    metaData.type = JDBCQueryMetaDataFactory.Type.DECLARED_QL;
                    for (Element declaredChild : children(reader)) {
                        switch (declaredChild) {
                            case FROM: {
                                metaData.declaredParts.put("from", getElementText(reader));
                                break;
                            }
                            case WHERE: {
                                metaData.declaredParts.put("where", getElementText(reader));
                                break;
                            }
                            case ORDER: {
                                metaData.declaredParts.put("order", getElementText(reader));
                                break;
                            }
                            case OTHER: {
                                metaData.declaredParts.put("other", getElementText(reader));
                                break;
                            }
                            case SELECT: {
                                for (Element selectChild : children(reader)) {
                                    switch (selectChild) {
                                        case DISTINCT: {
                                            metaData.declaredParts.put("distinct", getElementText(reader));
                                            break;
                                        }
                                        case EJB_NAME: {
                                            metaData.declaredParts.put("ejb-name", getElementText(reader));
                                            break;
                                        }
                                        case FIELD_NAME: {
                                            metaData.declaredParts.put("field-name", getElementText(reader));
                                            break;
                                        }
                                        case ALIAS: {
                                            metaData.declaredParts.put("alias", getElementText(reader));
                                            break;
                                        }
                                        case ADDITIONAL_COLUMNS: {
                                            metaData.declaredParts.put("additional-columns", getElementText(reader));
                                            break;
                                        }
                                        default: {
                                            throw unexpectedElement(reader);
                                        }
                                    }
                                }
                                break;
                            }
                            default: {
                                throw unexpectedElement(reader);
                            }
                        }
                    }
                    break;
                }
                case RAW_SQL: {
                    metaData.type = JDBCQueryMetaDataFactory.Type.RAW_SQL;
                    metaData.query = getElementText(reader);
                    break;
                }
                case READ_AHEAD: {
                    metaData.readAheadMetaData = parseReadAhead(reader);
                    break;
                }
                case QL_COMPILER: {
                    final String qlCompiler = getElementText(reader);
                    try {
                        metaData.qlCompiler = classLoader.loadClass(qlCompiler);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("Failed to load compiler implementation: " + qlCompiler, e);
                    }
                    break;
                }
                case LAZY_RESULTSET_LOADING: {
                    metaData.lazyResultsetLoading = Boolean.parseBoolean(getElementText(reader));
                    break;
                }
                case DESCRIPTION: {
                    getElementText(reader);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return metaData;
    }

    private static ParsedAudit parseAudit(final XMLStreamReader reader) throws XMLStreamException {
        final ParsedAudit metaData = new ParsedAudit();
        for (Element element : children(reader)) {
            switch (element) {
                case CREATED_BY: {
                    metaData.createdBy = parseAuditField(reader, "audit_created_by");
                    break;
                }
                case CREATED_TIME: {
                    metaData.createdTime = parseAuditField(reader, "audit_created_time");
                    break;
                }
                case UPDATED_BY: {
                    metaData.updatedBy = parseAuditField(reader, "audit_updated_by");
                    break;
                }
                case UPDATED_TIME: {
                    metaData.updatedTime = parseAuditField(reader, "audit_updated_time");
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return metaData;
    }

    private static ParsedCmpField parseAuditField(XMLStreamReader reader, String fieldName) throws XMLStreamException {
        final ParsedCmpField field = parseCmpField(reader);
        if (field.fieldName == null) {
            field.fieldName = fieldName;
        }
        if (field.columnName == null) {
            field.columnName = fieldName;
        }
        return field;
    }

    private static ParsedOptimisticLocking parseOptimisticLocking(final XMLStreamReader reader, final ClassLoader classLoader) throws XMLStreamException {
        final ParsedOptimisticLocking metaData = new ParsedOptimisticLocking();
        ParsedCmpField lockingField = null;
        for (Element element : children(reader)) {
            switch (element) {
                case GROUP_NAME: {
                    metaData.lockingStrategy = JDBCOptimisticLockingMetaData.LockingStrategy.FIELD_GROUP_STRATEGY;
                    metaData.groupName = getElementText(reader);
                    break;
                }
                case MODIFIED_STRATEGY: {
                    metaData.lockingStrategy = JDBCOptimisticLockingMetaData.LockingStrategy.MODIFIED_STRATEGY;
                    getElementText(reader);
                    break;
                }
                case READ_STRATEGY: {
                    metaData.lockingStrategy = JDBCOptimisticLockingMetaData.LockingStrategy.READ_STRATEGY;
                    getElementText(reader);
                    break;
                }
                case VERSION_COLUMN: {
                    metaData.lockingStrategy = JDBCOptimisticLockingMetaData.LockingStrategy.VERSION_COLUMN_STRATEGY;
                    getElementText(reader);
                    break;
                }
                case TIMESTAMP_COLUMN: {
                    metaData.lockingStrategy = JDBCOptimisticLockingMetaData.LockingStrategy.TIMESTAMP_COLUMN_STRATEGY;
                    getElementText(reader);
                    break;
                }
                case KEY_GENERATOR_FACTORY: {
                    metaData.lockingStrategy = JDBCOptimisticLockingMetaData.LockingStrategy.KEYGENERATOR_COLUMN_STRATEGY;
                    metaData.keyGeneratorFactory = getElementText(reader);
                    break;
                }
                case FIELD_TYPE: {
                    if (lockingField == null) lockingField = new ParsedCmpField();
                    try {
                        lockingField.fieldType = classLoader.loadClass(getElementText(reader));
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("Failed to load field type", e);
                    }
                    break;
                }
                case FIELD_NAME: {
                    if (lockingField == null) lockingField = new ParsedCmpField();
                    lockingField.fieldName = getElementText(reader);
                    break;
                }
                case COLUMN_NAME: {
                    if (lockingField == null) lockingField = new ParsedCmpField();
                    lockingField.columnName = getElementText(reader);
                    break;
                }
                case JDBC_TYPE: {
                    if (lockingField == null) lockingField = new ParsedCmpField();
                    lockingField.jdbcType = getJdbcTypeFromName(getElementText(reader));
                    break;
                }
                case SQL_TYPE: {
                    if (lockingField == null) lockingField = new ParsedCmpField();
                    lockingField.sqlType = getElementText(reader);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        metaData.lockingField = lockingField;
        return metaData;
    }


    private static Map<String, List<String>> parseLoadGroups(final XMLStreamReader reader) throws XMLStreamException {
        final Map<String, List<String>> groups = new HashMap<String, List<String>>();
        for (Element element : children(reader)) {
            switch (element) {
                case LOAD_GROUP: {
                    parseLoadGroup(reader, groups);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return groups;
    }

    private static void parseLoadGroup(final XMLStreamReader reader, Map<String, List<String>> groups) throws XMLStreamException {
        String groupName = null;
        final List<String> fields = new ArrayList<String>();
        for (Element element : children(reader)) {
            switch (element) {
                case LOAD_GROUP_NAME: {
                    groupName = getElementText(reader);
                    break;
                }
                case FIELD_NAME: {
                    fields.add(getElementText(reader));
                    break;
                }
                case DESCRIPTION: {
                    getElementText(reader);
                    break;
                }                
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        if (groupName != null) {
            groups.put(groupName, fields);
        }
    }

    private static ParsedCmpField parseCmpField(final XMLStreamReader reader) throws XMLStreamException {

        final ParsedCmpField metaData = new ParsedCmpField();
        for (Element element : children(reader)) {
            switch (element) {
                case COLUMN_NAME: {
                    metaData.columnName = getElementText(reader);
                    break;
                }
                case FIELD_NAME: {
                    metaData.fieldName = getElementText(reader);
                    break;
                }
                case READ_ONLY: {
                    metaData.readOnly = Boolean.parseBoolean(getElementText(reader));
                    break;
                }
                case READ_TIMEOUT: {
                    metaData.readTimeOut = Integer.parseInt(getElementText(reader));
                    break;
                }
                case NOT_NULL: {
                    metaData.notNull = Boolean.parseBoolean(getElementText(reader));
                    break;
                }
                case JDBC_TYPE: {
                    metaData.jdbcType = getJdbcTypeFromName(getElementText(reader));
                    break;
                }
                case SQL_TYPE: {
                    metaData.sqlType = getElementText(reader);
                    break;
                }
                case PROPERTY: {
                    metaData.propertyOverrides.add(parseProperty(reader));
                    break;
                }
                case AUTO_INCREMENT: {
                    getElementText(reader);
                    metaData.autoIncrement = true;
                    break;
                }
                case DB_INDEX: {
                    getElementText(reader);
                    metaData.genIndex = true;
                    break;
                }
                case CHECK_DIRTY_AFTER_GET: {
                    metaData.checkDirtyAfterGet = Boolean.parseBoolean(getElementText(reader));
                    break;
                }
                case STATE_FACTORY: {
                    metaData.stateFactory = getElementText(reader);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return metaData;
    }

    private static List<JDBCValueClassMetaData> parseDependentValueClasses(final XMLStreamReader reader, final ClassLoader classLoader) throws XMLStreamException {
        final List<JDBCValueClassMetaData> valueClasses = new ArrayList<JDBCValueClassMetaData>();
        for (Element element : children(reader)) {
            switch (element) {
                case DEPENDENT_VALUE_CLASS: {
                    valueClasses.add(parseValueClass(reader, classLoader));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return valueClasses;
    }

    private static JDBCValueClassMetaData parseValueClass(final XMLStreamReader reader, final ClassLoader classLoader) throws XMLStreamException {
        final Map<String, JDBCValuePropertyMetaData> properties = new HashMap<String, JDBCValuePropertyMetaData>();
        final JDBCValueClassMetaData valueClass = new JDBCValueClassMetaData();
        for (Element element : children(reader)) {
            switch (element) {
                case CLASS: {
                    try {
                        valueClass.setClass(classLoader.loadClass(getElementText(reader)));
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("Failed to load value class", e);
                    }
                    break;
                }
                case PROPERTY: {
                    parseValueProperty(reader, properties);
                    break;
                }
                case DESCRIPTION: {
                    getElementText(reader);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

        for (String propertyName : properties.keySet()) {
            final JDBCValuePropertyMetaData propertyMetaData = properties.get(propertyName);
            propertyMetaData.setPropertyName(propertyName, valueClass.getJavaType());
            valueClass.addProperty(propertyMetaData);
        }
        return valueClass;
    }

    private static void parseValueProperty(final XMLStreamReader reader, final Map<String, JDBCValuePropertyMetaData> properties) throws XMLStreamException {
        String propertyName = null;
        final JDBCValuePropertyMetaData metaData = new JDBCValuePropertyMetaData();
        for (Element element : children(reader)) {
            switch (element) {
                case PROPERTY_NAME: {
                    propertyName = getElementText(reader);
                    break;
                }
                case COLUMN_NAME: {
                    metaData.setColumnName(getElementText(reader));
                    break;
                }
                case NOT_NULL: {
                    metaData.setNotNul(Boolean.parseBoolean(getElementText(reader)));
                    break;
                }
                case JDBC_TYPE: {
                    metaData.setJdbcType(getJdbcTypeFromName(getElementText(reader)));
                    break;
                }
                case SQL_TYPE: {
                    metaData.setSqlType(getElementText(reader));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        properties.put(propertyName, metaData);
    }

    private static List<JDBCUserTypeMappingMetaData> parseUserTypeMappings(final XMLStreamReader reader) throws XMLStreamException {
        final List<JDBCUserTypeMappingMetaData> userTypeMappings = new ArrayList<JDBCUserTypeMappingMetaData>();
        for (Element element : children(reader)) {
            switch (element) {
                case USER_TYPE_MAPPING: {
                    userTypeMappings.add(parseUserTypeMapping(reader));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return userTypeMappings;
    }

    private static JDBCUserTypeMappingMetaData parseUserTypeMapping(final XMLStreamReader reader) throws XMLStreamException {

        final JDBCUserTypeMappingMetaData metaData = new JDBCUserTypeMappingMetaData();
        for (Element element : children(reader)) {
            switch (element) {
                case JAVA_TYPE: {
                    metaData.setJavaType(getElementText(reader));
                    break;
                }
                case MAPPED_TYPE: {
                    metaData.setMappedType(getElementText(reader));
                    break;
                }
                case MAPPER: {
                    metaData.setMapper(getElementText(reader));
                    break;
                }
                case CHECK_DIRTY_AFTER_GET: {
                    metaData.setCheckDirtyAfterGet(Boolean.parseBoolean(getElementText(reader)));
                    break;
                }
                case STATE_FACTORY: {
                    metaData.setStateFactory(getElementText(reader));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return metaData;
    }

    private static void parseReservedWords(final XMLStreamReader reader) throws XMLStreamException {

        for (Element element : children(reader)) {
            switch (element) {
                case WORD: {
                    SQLUtil.addToRwords(getElementText(reader));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private static void moveToStart(final XMLStreamReader reader) throws XMLStreamException {
        reader.require(START_DOCUMENT, null, null);
        while (reader.hasNext() && reader.next() != START_ELEMENT) {
        }
    }

    private static boolean isEmpty(final String value) {
        return value == null || value.trim().equals("");
    }

    /**
     * Gets the JDBC type constant int for the name. The mapping from name to jdbc
     * type is contained in java.sql.Types.
     *
     * @param name the name for the jdbc type
     * @return the int type constant from java.sql.Types
     * @see java.sql.Types
     */
    public static int getJdbcTypeFromName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("jdbc-type cannot be null");
        }

        try {
            return (Integer) Types.class.getField(name).get(null);
        } catch (Exception e) {
            return Types.OTHER;
        }
    }


    private static Iterable<Element> children(final XMLStreamReader reader) throws XMLStreamException {
        return new Iterable<Element>() {
            public Iterator<Element> iterator() {
                return new Iterator<Element>() {
                    public boolean hasNext() {
                        try {
                            return reader.hasNext() && reader.nextTag() != END_ELEMENT;
                        } catch (XMLStreamException e) {
                            throw new IllegalStateException("Unable to get next element: ", e);
                        }
                    }

                    public Element next() {
                        return Element.forName(reader.getLocalName());
                    }

                    public void remove() {
                        throw new UnsupportedOperationException("Remove not supported");
                    }
                };
            }
        };
    }
}
