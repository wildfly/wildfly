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

package org.jboss.as.messaging.jms;

import java.util.Locale;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 */
public class JMSSubsystemProviders {

    static final DescriptionProvider SUBSYSTEM = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return JMSDescriptions.getSubsystem(locale);
        }
    };

    static final DescriptionProvider SUBSYSTEM_ADD = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return JMSDescriptions.getSubsystemAdd(locale);
        }
    };

    static final DescriptionProvider SUBSYSTEM_DESCRIBE = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return JMSDescriptions.getSubsystemDescribe(locale);
        }
    };

    static final DescriptionProvider JMS_QUEUE = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return JMSDescriptions.getQueue(locale);
        }

    };

    static final DescriptionProvider JMS_QUEUE_ADD = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return JMSDescriptions.getQueueAdd(locale);
        }
    };

    static final DescriptionProvider JMS_QUEUE_REMOVE = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return JMSDescriptions.getQueueRemove(locale);
        }
    };

    static final DescriptionProvider CF = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return JMSDescriptions.getConnectionFactory(locale);
        }

    };

    static final DescriptionProvider CF_ADD = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return JMSDescriptions.getConnectionFactoryAdd(locale);
        }
    };

    static final DescriptionProvider CF_REMOVE = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return JMSDescriptions.getConnectionFactoryRemove(locale);
        }
    };

    static final DescriptionProvider JMS_TOPIC = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return JMSDescriptions.getTopic(locale);
        }
    };

    static final DescriptionProvider JMS_TOPIC_ADD = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return JMSDescriptions.getTopicAdd(locale);
        }
    };

    static final DescriptionProvider JMS_TOPIC_REMOVE = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return JMSDescriptions.getTopicRemove(locale);
        }
    };
}
