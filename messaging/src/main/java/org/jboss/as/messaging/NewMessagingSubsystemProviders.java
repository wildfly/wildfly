/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.messaging;

import static org.jboss.as.messaging.CommonAttributes.ACCEPTOR;
import static org.jboss.as.messaging.CommonAttributes.ADDRESS_SETTING;
import static org.jboss.as.messaging.CommonAttributes.BACKUP;
import static org.jboss.as.messaging.CommonAttributes.BACKUP_CONNECTOR_REF;
import static org.jboss.as.messaging.CommonAttributes.BINDINGS_DIRECTORY;
import static org.jboss.as.messaging.CommonAttributes.BROADCAST_PERIOD;
import static org.jboss.as.messaging.CommonAttributes.CLUSTERED;
import static org.jboss.as.messaging.CommonAttributes.CLUSTER_PASSWORD;
import static org.jboss.as.messaging.CommonAttributes.CLUSTER_USER;
import static org.jboss.as.messaging.CommonAttributes.CONNECTION_TTL_OVERRIDE;
import static org.jboss.as.messaging.CommonAttributes.CONNECTOR;
import static org.jboss.as.messaging.CommonAttributes.CREATE_BINDINGS_DIR;
import static org.jboss.as.messaging.CommonAttributes.CREATE_JOURNAL_DIR;
import static org.jboss.as.messaging.CommonAttributes.ID_CACHE_SIZE;
import static org.jboss.as.messaging.CommonAttributes.JMX_DOMAIN;
import static org.jboss.as.messaging.CommonAttributes.JMX_MANAGEMENT_ENABLED;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_BUFFER_SIZE;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_BUFFER_TIMEOUT;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_COMPACT_MIN_FILES;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_COMPACT_PERCENTAGE;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_DIRECTORY;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_FILE_SIZE;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_MAX_IO;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_MIN_FILES;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_SYNC_NON_TRANSACTIONAL;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_TYPE;
import static org.jboss.as.messaging.CommonAttributes.LARGE_MESSAGES_DIRECTORY;
import static org.jboss.as.messaging.CommonAttributes.PAGING_DIRECTORY;
import static org.jboss.as.messaging.CommonAttributes.PERF_BLAST_PAGES;
import static org.jboss.as.messaging.CommonAttributes.PERSISTENCE_ENABLED;
import static org.jboss.as.messaging.CommonAttributes.PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY;
import static org.jboss.as.messaging.CommonAttributes.PERSIST_ID_CACHE;
import static org.jboss.as.messaging.CommonAttributes.QUEUE;
import static org.jboss.as.messaging.CommonAttributes.SECURITY_SETTING;

import java.util.Locale;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 */
class NewMessagingSubsystemProviders {

    static final String[] MESSAGING_ROOT_ATTRIBUTES = new String[] { ACCEPTOR, ADDRESS_SETTING, BACKUP,
        BACKUP_CONNECTOR_REF, BINDINGS_DIRECTORY, BROADCAST_PERIOD, CLUSTERED, CLUSTER_PASSWORD, CLUSTER_USER, CONNECTION_TTL_OVERRIDE, CONNECTOR,
        CREATE_BINDINGS_DIR, CREATE_BINDINGS_DIR, CREATE_JOURNAL_DIR, ID_CACHE_SIZE, JMX_DOMAIN, JMX_MANAGEMENT_ENABLED,
        JOURNAL_BUFFER_SIZE, JOURNAL_BUFFER_TIMEOUT, JOURNAL_COMPACT_MIN_FILES, JOURNAL_COMPACT_PERCENTAGE, JOURNAL_DIRECTORY,
        JOURNAL_MIN_FILES, JOURNAL_SYNC_NON_TRANSACTIONAL, JOURNAL_TYPE, JOURNAL_FILE_SIZE, JOURNAL_MAX_IO, LARGE_MESSAGES_DIRECTORY, PAGING_DIRECTORY,
        PERF_BLAST_PAGES, PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY, PERSIST_ID_CACHE, PERSISTENCE_ENABLED, QUEUE,
        SECURITY_SETTING};

    static final DescriptionProvider SUBSYSTEM = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            return MessagingDescriptions.getRootResource(locale);
        }
    };

    static final DescriptionProvider SUBSYSTEM_ADD = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            return MessagingDescriptions.getSubsystemAdd(locale);
        }
    };

    static final DescriptionProvider SUBSYSTEM_REMOVE = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            return MessagingDescriptions.getSubsystemRemove(locale);
        }
    };

    static final DescriptionProvider QUEUE_RESOURCE = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            return MessagingDescriptions.getQueueResource(locale);
        }
    };

    static final DescriptionProvider QUEUE_ADD = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            return MessagingDescriptions.getQueueAdd(locale);
        }
    };

    static final DescriptionProvider QUEUE_REMOVE = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            return MessagingDescriptions.getQueueRemove(locale);
        }
    };

}
