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
package org.jboss.as.jpa.hibernate4.management;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Model and operation descriptions for the Hibernate Persistence Provider Adaptor
 *
 * @author Scott Marlow
 */
public class HibernateDescriptions {

    static final String RESOURCE_NAME = HibernateDescriptions.class.getPackage().getName() + ".LocalDescriptions";

    private HibernateDescriptions() {
    }

    static ResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        return new StandardResourceDescriptionResolver(keyPrefix, RESOURCE_NAME, HibernateDescriptions.class.getClassLoader(), true, true);
    }

    static ModelNode describeTopLevelAttributes(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        ModelNode subsystem = new ModelNode();
        subsystem.get(DESCRIPTION).set(bundle.getString(HibernateDescriptionConstants.HIBERNATE_DESCRIPTION));
        // the attribute names should always be in the same language (english) but the descriptions
        // could be translated into other languages.
        subsystem.get(ATTRIBUTES, "entity-delete-count", DESCRIPTION).set(bundle.getString(HibernateDescriptionConstants.ENTITY_DELETE_COUNT));
        subsystem.get(ATTRIBUTES, "entity-delete-count", TYPE).set(ModelType.INT);

        subsystem.get(ATTRIBUTES, "entity-insert-count", DESCRIPTION).set(bundle.getString(HibernateDescriptionConstants.ENTITY_INSERT_COUNT));
        subsystem.get(ATTRIBUTES, "entity-insert-count", TYPE).set(ModelType.INT);

        subsystem.get(ATTRIBUTES, "entity-load-count", DESCRIPTION).set(bundle.getString(HibernateDescriptionConstants.ENTITY_LOAD_COUNT));
        subsystem.get(ATTRIBUTES, "entity-load-count", TYPE).set(ModelType.INT);

        subsystem.get(ATTRIBUTES, "entity-fetch-count", DESCRIPTION).set(bundle.getString(HibernateDescriptionConstants.ENTITY_FETCH_COUNT));
        subsystem.get(ATTRIBUTES, "entity-fetch-count", TYPE).set(ModelType.INT);

        subsystem.get(ATTRIBUTES, "entity-update-count", DESCRIPTION).set(bundle.getString(HibernateDescriptionConstants.ENTITY_UPDATE_COUNT));
        subsystem.get(ATTRIBUTES, "entity-update-count", TYPE).set(ModelType.INT);

        subsystem.get(ATTRIBUTES, "query-execution-count", DESCRIPTION).set(bundle.getString(HibernateDescriptionConstants.QUERY_EXECUTION_COUNT));
        subsystem.get(ATTRIBUTES, "query-execution-count", TYPE).set(ModelType.INT);

        subsystem.get(ATTRIBUTES, "query-execution-max-time", DESCRIPTION).set(bundle.getString(HibernateDescriptionConstants.QUERY_EXECUTION_MAX_TIME));
        subsystem.get(ATTRIBUTES, "query-execution-max-time", TYPE).set(ModelType.INT);

        subsystem.get(ATTRIBUTES, "query-execution-max-time-query-string", DESCRIPTION).set(bundle.getString(HibernateDescriptionConstants.QUERY_EXECUTION_MAX_TIME_QUERY_STRING));
        subsystem.get(ATTRIBUTES, "query-execution-max-time-query-string", TYPE).set(ModelType.STRING);

        subsystem.get(ATTRIBUTES, "query-cache-hit-count", DESCRIPTION).set(bundle.getString(HibernateDescriptionConstants.QUERY_CACHE_HIT_COUNT));
        subsystem.get(ATTRIBUTES, "query-cache-hit-count", TYPE).set(ModelType.INT);

        subsystem.get(ATTRIBUTES, "query-cache-miss-count", DESCRIPTION).set(bundle.getString(HibernateDescriptionConstants.QUERY_CACHE_MISS_COUNT));
        subsystem.get(ATTRIBUTES, "query-cache-miss-count", TYPE).set(ModelType.INT);

        subsystem.get(ATTRIBUTES, "query-cache-put-count", DESCRIPTION).set(bundle.getString(HibernateDescriptionConstants.QUERY_CACHE_PUT_COUNT));
        subsystem.get(ATTRIBUTES, "query-cache-put-count", TYPE).set(ModelType.INT);


        subsystem.get(ATTRIBUTES, "flush-count", DESCRIPTION).set(bundle.getString(HibernateDescriptionConstants.FLUSH_COUNT));
        subsystem.get(ATTRIBUTES, "flush-count", TYPE).set(ModelType.INT);

        subsystem.get(ATTRIBUTES, "connect-count", DESCRIPTION).set(bundle.getString(HibernateDescriptionConstants.CONNECT_COUNT));
        subsystem.get(ATTRIBUTES, "connect-count", TYPE).set(ModelType.INT);

        subsystem.get(ATTRIBUTES, "second-level-cache-hit-count", DESCRIPTION).set(bundle.getString(HibernateDescriptionConstants.SECOND_LEVEL_CACHE_HIT_COUNT));
        subsystem.get(ATTRIBUTES, "second-level-cache-hit-count", TYPE).set(ModelType.INT);

        subsystem.get(ATTRIBUTES, "second-level-cache-miss-count", DESCRIPTION).set(bundle.getString(HibernateDescriptionConstants.SECOND_LEVEL_CACHE_MISS_COUNT));
        subsystem.get(ATTRIBUTES, "second-level-cache-miss-count", TYPE).set(ModelType.INT);

        subsystem.get(ATTRIBUTES, "second-level-cache-put-count", DESCRIPTION).set(bundle.getString(HibernateDescriptionConstants.SECOND_LEVEL_CACHE_PUT_COUNT));
        subsystem.get(ATTRIBUTES, "second-level-cache-put-count", TYPE).set(ModelType.INT);

        subsystem.get(ATTRIBUTES, "session-close-count", DESCRIPTION).set(bundle.getString(HibernateDescriptionConstants.SESSION_CLOSE_COUNT));
        subsystem.get(ATTRIBUTES, "session-close-count", TYPE).set(ModelType.INT);

        subsystem.get(ATTRIBUTES, "session-open-count", DESCRIPTION).set(bundle.getString(HibernateDescriptionConstants.SESSION_OPEN_COUNT));
        subsystem.get(ATTRIBUTES, "session-open-count", TYPE).set(ModelType.INT);

        subsystem.get(ATTRIBUTES, "collection-load-count", DESCRIPTION).set(bundle.getString(HibernateDescriptionConstants.COLLECTION_LOAD_COUNT));
        subsystem.get(ATTRIBUTES, "collection-load-count", TYPE).set(ModelType.INT);

        subsystem.get(ATTRIBUTES, "collection-fetch-count", DESCRIPTION).set(bundle.getString(HibernateDescriptionConstants.COLLECTION_FETCH_COUNT));
        subsystem.get(ATTRIBUTES, "collection-fetch-count", TYPE).set(ModelType.INT);

        subsystem.get(ATTRIBUTES, "collection-update-count", DESCRIPTION).set(bundle.getString(HibernateDescriptionConstants.COLLECTION_UPDATE_COUNT));
        subsystem.get(ATTRIBUTES, "collection-update-count", TYPE).set(ModelType.INT);

        subsystem.get(ATTRIBUTES, "collection-remove-count", DESCRIPTION).set(bundle.getString(HibernateDescriptionConstants.COLLECTION_REMOVE_COUNT));
        subsystem.get(ATTRIBUTES, "collection-remove-count", TYPE).set(ModelType.INT);

        subsystem.get(ATTRIBUTES, "collection-recreated-count", DESCRIPTION).set(bundle.getString(HibernateDescriptionConstants.COLLECTION_RECREATED_COUNT));
        subsystem.get(ATTRIBUTES, "collection-recreated-count", TYPE).set(ModelType.INT);

        subsystem.get(ATTRIBUTES, "successful-transaction-count", DESCRIPTION).set(bundle.getString(HibernateDescriptionConstants.SUCCESSFUL_TRANSACTION_COUNT));
        subsystem.get(ATTRIBUTES, "successful-transaction-count", TYPE).set(ModelType.INT);

        subsystem.get(ATTRIBUTES, "completed-transaction-count", DESCRIPTION).set(bundle.getString(HibernateDescriptionConstants.COMPLETED_TRANSACTION_COUNT));
        subsystem.get(ATTRIBUTES, "completed-transaction-count", TYPE).set(ModelType.INT);

        subsystem.get(ATTRIBUTES, "prepared-statement-count", DESCRIPTION).set(bundle.getString(HibernateDescriptionConstants.PREPARED_STATEMENT_COUNT));
        subsystem.get(ATTRIBUTES, "prepared-statement-count", TYPE).set(ModelType.INT);

        subsystem.get(ATTRIBUTES, "close-statement-count", DESCRIPTION).set(bundle.getString(HibernateDescriptionConstants.CLOSE_STATEMENT_COUNT));
        subsystem.get(ATTRIBUTES, "close-statement-count", TYPE).set(ModelType.INT);

        subsystem.get(ATTRIBUTES, "optimistic-failure-count", DESCRIPTION).set(bundle.getString(HibernateDescriptionConstants.OPTIMISTIC_FAILURE_COUNT));
        subsystem.get(ATTRIBUTES, "optimistic-failure-count", TYPE).set(ModelType.INT);

        subsystem.get(ATTRIBUTES, "enabled", DESCRIPTION).set(bundle.getString(HibernateDescriptionConstants.CHECK_STATISTICS));
        subsystem.get(ATTRIBUTES, "enabled", TYPE).set(ModelType.BOOLEAN);

        subsystem.get(OPERATIONS);  // placeholder

        subsystem.get(CHILDREN, "entity-cache", DESCRIPTION).set(bundle.getString(HibernateDescriptionConstants.SECOND_LEVEL_CACHE));
        subsystem.get(CHILDREN, "entity-cache", MODEL_DESCRIPTION); // placeholder

        subsystem.get(CHILDREN, "query-cache", DESCRIPTION).set(bundle.getString(HibernateDescriptionConstants.QUERY_STATISTICS));
        subsystem.get(CHILDREN, "query-cache", MODEL_DESCRIPTION); // placeholder

        subsystem.get(CHILDREN, "entity", DESCRIPTION).set(bundle.getString(HibernateDescriptionConstants.ENTITY_STATISTICS));
        subsystem.get(CHILDREN, "entity", MODEL_DESCRIPTION); // placeholder

        subsystem.get(CHILDREN, "collection", DESCRIPTION).set(bundle.getString(HibernateDescriptionConstants.COLLECTION_STATISTICS));
        subsystem.get(CHILDREN, "collection", MODEL_DESCRIPTION); // placeholder

        return subsystem;
    }

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

    public static ModelNode getDescriptionOnlyOperation(final Locale locale, final String operationName, final String descriptionPrefix) {
        final ResourceBundle bundle = getResourceBundle(locale);

        return CommonDescriptions.getDescriptionOnlyOperation(bundle, operationName, descriptionPrefix);
    }

    //
    public static ModelNode clear(final Locale locale) {
        return getDescriptionOnlyOperation(locale, HibernateDescriptionConstants.CLEAR_STATISTICS, HibernateDescriptionConstants.OPERATION_PREFIX);
    }

    public static ModelNode evictall(final Locale locale) {
        return getDescriptionOnlyOperation(locale, HibernateDescriptionConstants.EVICTALL_2LC, HibernateDescriptionConstants.OPERATION_PREFIX);
    }

    public static ModelNode summary(Locale locale) {
        return getDescriptionOnlyOperation(locale, HibernateDescriptionConstants.SUMMARY_STATISTICS, HibernateDescriptionConstants.OPERATION_PREFIX);
    }

}
