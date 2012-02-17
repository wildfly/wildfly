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

import java.util.HashMap;
import java.util.Map;

/**
 * @author John Bailey
 */
enum Element {
    UNKNOWN(null),
    ADD_COLUMN_TEMPLATE("add-column-template"),
    ADDITIONAL_COLUMNS("additional-columns"),
    ALIAS("alias"),
    ALIAS_HEADER_PREFIX("alias-header-prefix"),
    ALIAS_HEADER_SUFFIX("alias-header-suffix"),
    ALIAS_MAX_LENGHT("alias-max-length"),
    ALTER_TABLE("alter-table"),
    ALTER_COLUMN_TEMPLATE("alter-column-template"),
    ATTRIBUTE("attribute"),
    AUDIT("audit"),
    AUTO_INCREMENT("auto-increment"),
    AUTO_INCREMENT_TEMPLATE("auto-increment-template"),
    BATCH_CASCADE_DELETE("batch-cascade-delete"),
    CHECK_DIRTY_AFTER_GET("check-dirty-after-get"),
    CLASS("class"),
    CLEAN_READ_AHEAD("clean-read-ahead-on-load"),
    CMP_FIELD("cmp-field"),
    CMR_FIELD("cmr-field-name"),
    COLUMN_NAME("column-name"),
    CREATE_TABLE("create-table"),
    CREATED_BY("created-by"),
    CREATED_TIME("created-time"),
    DATASOURCE("datasource"),
    DATASOURCE_MAPPING("datasource-mapping"),
    DB_INDEX("dbindex"),
    DECLARED_QL("declared-sql"),
    DEFAULTS("defaults"),
    DEPENDENT_VALUE_CLASS("dependent-value-class"),
    DEPENDENT_VALUE_CLASSES("dependent-value-classes"),
    DESCRIPTION("description"),
    DISTINCT("distinct"),
    DROP_COLUMN_TEMPLATE("drop-column-template"),
    DYNAMIC_QL("dynamic-ql"),
    EAGER_LOAD_GROUP("eager-load-group"),
    EJB_NAME("ejb-name"),
    EJB_RELATION("ejb-relation"),
    EJB_RELATION_NAME("ejb-relation-name"),
    EJB_RELATIONSHIP_ROLE("ejb-relationship-role"),
    EJB_RELATIONSHIP_ROLE_NAME("ejb-relationship-role-name"),
    ENTERPRISE_BEANS("enterprise-beans"),
    ENTITY("entity"),
    ENTITY_COMMAND("entity-command"),
    ENTITY_COMMANDS("entity-commands"),
    FALSE_MAPPING("false-mapping"),
    FETCH_SIZE("fetch-size"),
    FIELD_NAME("field-name"),
    FIELD_TYPE("field-type"),
    FK_CONSTRAINT("fk-constraint"),
    FK_CONSTRAINT_TEMPLATE("fk-constraint-template"),
    FOREIGN_KEY_MAPPING("foreign-key-mapping"),
    FROM("from"),
    FUNCTION_MAPPING("function-mapping"),
    FUNCTION_NAME("function-name"),
    FUNCTION_SQL("function-sql"),
    GROUP_NAME("group-name"),
    JAVA_TYPE("java-type"),
    JBOSS_QL("jboss-ql"),
    JDBC_TYPE("jdbc-type"),
    KEY_FIELD("key-field"),
    KEY_FIELDS("key-fields"),
    KEY_GENERATOR_FACTORY("key-generator-factory"),
    LAZY_LOAD_GROUPS("lazy-load-groups"),
    LAZY_RESULTSET_LOADING("lazy-resultset-loading"),
    LEFT_JOIN("left-join"),
    LIST_CACHE_MAX("list-cache-max"),
    LOAD_GROUP("load-group"),
    LOAD_GROUPS("load-groups"),
    LOAD_GROUP_NAME("load-group-name"),
    MAPPER("mapper"),
    MAPPED_TYPE("mapped-type"),
    MAPPING("mapping"),
    MAX_KEYS_IN_DELETE("max-keys-in-delete"),
    METHOD_NAME("method-name"),
    METHOD_PARAM("method-param"),
    METHOD_PARAMS("method-params"),
    MODIFIED_STRATEGY("modified-strategy"),
    NAME("name"),
    NOT_NULL("not-null"),
    OPTIMISTIC_LOCKING("optimistic-locking"),
    ORDER("order"),
    OTHER("other"),
    PAGE_SIZE("page-size"),
    PARAM_SETTER("param-setter"),
    PK_CONSTRAINT("pk-constraint"),
    PK_CONSTRAINT_TEMPLATE("pk-constraint-template"),
    POST_TABLE_CREATE("post-table-create"),
    PREFERRED_RELATION("preferred-relation-mapping"),
    PROPERTY("property"),
    PROPERTY_NAME("property-name"),
    QL_COMPILER("ql-compiler"),
    QUERY("query"),
    QUERY_METHOD("query-method"),
    RAW_SQL("raw-sql"),
    READ_AHEAD("read-ahead"),
    READ_ONLY("read-only"),
    READ_TIMEOUT("read-time-out"),
    READ_STRATEGY("read-strategy"),
    RELATIONSHIPS("relationships"),
    RELATION_TABLE_MAPPING("relation-table-mapping"),
    REMOVE_TABLE("remove-table"),
    RESERVED_WORDS("reserved-words"),
    RESULT_READER("result-reader"),
    ROW_LOCKING("row-locking"),
    ROW_LOCKING_TEMPLATE("row-locking-template"),
    SELECT("select"),
    SUBQUERY_SUPPORTED("subquery-supported"),
    SQL_STATEMENT("sql-statement"),
    SQL_TYPE("sql-type"),
    STATE_FACTORY("state-factory"),
    STRATEGY("strategy"),
    TABLE_NAME("table-name"),
    THROW_RUNTIME_EX("throw-runtime-exceptions"),
    TIMESTAMP_COLUMN("timestamp-column"),
    TRUE_MAPPING("true-mapping"),
    TYPE_MAPPING("type-mapping"),
    TYPE_MAPPINGS("type-mappings"),
    UNKNOWN_KEY_CLASS("unknown-pk-class"),
    UNKNOWN_PK("unknown-pk"),
    UPDATED_BY("updated-by"),
    UPDATED_TIME("updated-time"),
    USER_TYPE_MAPPINGS("user-type-mappings"),
    USER_TYPE_MAPPING("user-type-mapping"),
    VERSION_COLUMN("version-column"),
    WHERE("where"),
    WORD("word");

    private final String name;

    Element(final String name) {
        this.name = name;
    }


    public String getLocalName() {
        return name;
    }

    private static final Map<String, Element> MAP;

    static {
        final Map<String, Element> map = new HashMap<String, Element>();
        for (Element element : values()) {
            final String name = element.getLocalName();
            if (name != null) map.put(name, element);
        }
        MAP = map;
    }

    public static Element forName(String localName) {
        final Element element = MAP.get(localName);
        return element == null ? UNKNOWN : element;
    }

}
