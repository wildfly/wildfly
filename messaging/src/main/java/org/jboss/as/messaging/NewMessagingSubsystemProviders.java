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

import static org.jboss.as.messaging.CommonAttributes.*;

import java.util.Locale;
import java.util.ResourceBundle;

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

    static final String RESOURCE_NAME = NewMessagingSubsystemProviders.class.getPackage().getName() + ".LocalDescriptions";

    static final DescriptionProvider SUBSYSTEM = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            // TODO
            return node;
        }
    };

    static final DescriptionProvider SUBSYSTEM_ADD = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            // TODO
            return node;
        }
    };

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

}
