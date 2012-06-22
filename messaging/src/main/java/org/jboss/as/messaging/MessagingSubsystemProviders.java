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

/**
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 */
class MessagingSubsystemProviders {

    static final DescriptionProvider ADDRESS_SETTING = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return MessagingDescriptions.getAddressSetting(locale);
        }
    };

    static final DescriptionProvider ADDRESS_SETTING_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return MessagingDescriptions.getAddressSettingAdd(locale);
        }
    };

    static final DescriptionProvider ADDRESS_SETTING_REMOVE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return MessagingDescriptions.getAddressSettingRemove(locale);
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

    public static final DescriptionProvider JMS_TOPIC_ADD = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return MessagingDescriptions.getTopicAdd(locale);
        }
    };

    public static final DescriptionProvider JMS_TOPIC_REMOVE = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return MessagingDescriptions.getTopicRemove(locale);
        }
    };

    public static final DescriptionProvider ACCEPTOR = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return MessagingDescriptions.getAcceptor(locale);
        }
    };

    public static final DescriptionProvider ACCEPTOR_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return MessagingDescriptions.getAcceptorAdd(locale);
        }
    };

    public static final DescriptionProvider ACCEPTOR_REMOVE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return MessagingDescriptions.getAcceptorRemove(locale);
        }
    };

    public static final DescriptionProvider REMOTE_ACCEPTOR = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return MessagingDescriptions.getRemoteAcceptor(locale);
        }
    };

    public static final DescriptionProvider REMOTE_ACCEPTOR_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return MessagingDescriptions.getRemoteAcceptorAdd(locale);
        }
    };

    public static final DescriptionProvider IN_VM_ACCEPTOR = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return MessagingDescriptions.getInVMAcceptor(locale);
        }
    };

    public static final DescriptionProvider IN_VM_ACCEPTOR_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return MessagingDescriptions.getInVMAcceptorAdd(locale);
        }
    };

    public static final DescriptionProvider CONNECTOR = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return MessagingDescriptions.getConnector(locale);
        }
    };

    public static final DescriptionProvider CONNECTOR_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return MessagingDescriptions.getConnectorAdd(locale);
        }
    };

    public static final DescriptionProvider CONNECTOR_REMOVE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return MessagingDescriptions.getConnectorRemove(locale);
        }
    };

    public static final DescriptionProvider REMOTE_CONNECTOR = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return MessagingDescriptions.getRemoteConnector(locale);
        }
    };

    public static final DescriptionProvider REMOTE_CONNECTOR_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return MessagingDescriptions.getRemoteConnectorAdd(locale);
        }
    };

    public static final DescriptionProvider IN_VM_CONNECTOR = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return MessagingDescriptions.getInVMConnector(locale);
        }
    };

    public static final DescriptionProvider IN_VM_CONNECTOR_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return MessagingDescriptions.getInVMConnectorAdd(locale);
        }
    };

    public static final DescriptionProvider PARAM = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return MessagingDescriptions.getParam(locale);
        }
    };

    public static final DescriptionProvider PARAM_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return MessagingDescriptions.getParamAdd(locale);
        }
    };

    public static final DescriptionProvider PARAM_REMOVE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return MessagingDescriptions.getParamRemove(locale);
        }
    };

    public static class PathProvider implements DescriptionProvider  {
        final String pathType;

        public PathProvider(String pathType) {
            this.pathType = pathType;
        }

        @Override
        public ModelNode getModelDescription(Locale locale) {
            return MessagingDescriptions.getPathResource(locale, pathType);
        }
    };

    public static final DescriptionProvider PATH_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return MessagingDescriptions.getPathAdd(locale);
        }
    };

    public static final DescriptionProvider PATH_REMOVE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return MessagingDescriptions.getPathRemove(locale);
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

    public static final DescriptionProvider CONNECTOR_SERVICE_RESOURCE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return MessagingDescriptions.getConnectorServiceResource(locale);
        }
    };

    public static final DescriptionProvider CONNECTOR_SERVICE_PARAM_RESOURCE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return MessagingDescriptions.getConnectorServiceParamResource(locale);
        }
    };

    public static final DescriptionProvider SECURITY_ROLE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return MessagingDescriptions.getSecurityRoleResource(locale);
        }
    };

    public static final DescriptionProvider SECURITY_SETTING = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return MessagingDescriptions.getSecuritySettingResource(locale);
        }
    };

    public static final DescriptionProvider CORE_ADDRESS = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return MessagingDescriptions.getCoreAddressResource(locale);
        }
    };
}
