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

import java.util.Locale;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.messaging.CommonAttributes.*;
import static org.jboss.as.messaging.CommonAttributes.JMS_QUEUE;
import static org.jboss.as.messaging.CommonAttributes.JMS_TOPIC;

/**
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 */
class MessagingSubsystemProviders {

    static final String[] MESSAGING_ROOT_ATTRIBUTES = new String[] { ACCEPTOR, ADDRESS_SETTING,
        CONNECTION_FACTORY, CONNECTOR_REF, BINDINGS_DIRECTORY, BROADCAST_PERIOD, CONNECTOR,
        GROUPING_HANDLER, JMS_QUEUE, JMS_TOPIC, JOURNAL_DIRECTORY, LARGE_MESSAGES_DIRECTORY,
        PAGING_DIRECTORY, POOLED_CONNECTION_FACTORY, QUEUE, SECURITY_SETTING };

    static final DescriptionProvider SUBSYSTEM = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return MessagingDescriptions.getRootResource(locale);
        }
    };

    static final DescriptionProvider SUBSYSTEM_ADD = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return MessagingDescriptions.getSubsystemAdd(locale);
        }
    };

    static final DescriptionProvider SUBSYSTEM_REMOVE = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            // Private method; description not needed
            return new ModelNode();
        }
    };

    static final DescriptionProvider QUEUE_RESOURCE = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return MessagingDescriptions.getQueueResource(locale);
        }
    };

    public static final DescriptionProvider JMS_QUEUE_RESOURCE = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return MessagingDescriptions.getJmsQueueResource(locale);
        }

    };

    public static final DescriptionProvider CF = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return MessagingDescriptions.getConnectionFactory(locale);
        }

    };

    public static final DescriptionProvider CF_ADD = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return MessagingDescriptions.getConnectionFactoryAdd(locale);
        }
    };

    public static final DescriptionProvider CF_REMOVE = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return MessagingDescriptions.getConnectionFactoryRemove(locale);
        }
    };

    public static final DescriptionProvider JMS_TOPIC_RESOURCE = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return MessagingDescriptions.getTopic(locale);
        }
    };

    public static final DescriptionProvider RA = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return MessagingDescriptions.getPooledConnectionFactory(locale);
        }
    };

    public static final DescriptionProvider RA_ADD = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return MessagingDescriptions.getPooledConnectionFactoryAdd(locale);
        }
    };

    public static final DescriptionProvider RA_REMOVE = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return MessagingDescriptions.getPooledConnectionFactoryRemove(locale);
        }
    };

    public static final DescriptionProvider DIVERT_RESOURCE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return MessagingDescriptions.getDivertResource(locale);
        }
    };
}
