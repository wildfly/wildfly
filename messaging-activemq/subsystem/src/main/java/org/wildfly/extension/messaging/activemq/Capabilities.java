/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.messaging.activemq;

import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.as.network.SocketBinding;

/**
 * Capabilities for the messaging-activemq extension. This is not to be used outside of this extension.
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2017 Red Hat Inc.
 */
public class Capabilities {

    /**
     * Represents the data source capability
     *
     * @see <a href="https://github.com/wildfly/wildfly-capabilities/blob/master/org/wildfly/data-source/capability.adoc">Capability documentation</a>
     */
    static final String DATA_SOURCE_CAPABILITY = "org.wildfly.data-source";

    /**
     * The capability name for the JMX management.
     *
     * @see <a href="https://github.com/wildfly/wildfly-capabilities/blob/master/org/wildfly/management/jmx/capability.adoc">Capability documentation</a>
     */
    static final String JMX_CAPABILITY = "org.wildfly.management.jmx";

    /**
     * A capability for the current messaging-activemq server configuration.
     *
     * @see <a href="https://github.com/wildfly/wildfly-capabilities/blob/master/org/wildfly/messaging/activemq/server/capability.adoc">Capability documentation</a>
     */
    static final RuntimeCapability<Void> ACTIVEMQ_SERVER_CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.messaging.activemq.server", true)
            //.addRuntimeOnlyRequirements(JMX_CAPABILITY) -- has no function so don't use it
            .build();

    /**
     * A capability name for the legacy security domain.
     * @see <a href="https://github.com/wildfly/wildfly-capabilities/blob/master/org/wildfly/security/security-domain/capability.adoc">Capability documentation</a>
     */
    static final RuntimeCapability<Void> LEGACY_SECURITY_DOMAIN_CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.security.legacy-security-domain", true)
            .build();

    /**
     * The capability name for the Elytron security domain.
     */
    static final String ELYTRON_DOMAIN_CAPABILITY = "org.wildfly.security.security-domain";

    /**
     * The capability for the PathManager
     *
     * @see <a href="https://github.com/wildfly/wildfly-capabilities/blob/master/org/wildfly/management/path-manager/capability.adoc">documentation</a>
     */
    static final String PATH_MANAGER_CAPABILITY = "org.wildfly.management.path-manager";

    /**
     * The capability for the Http Listener Registry
     *
     * @see <a href="https://github.com/wildfly/wildfly-capabilities/blob/master/org/wildfly/remoting/http-listener-registry/capability.adoc">documentation</a>
     */
    static final String HTTP_LISTENER_REGISTRY_CAPABILITY_NAME = "org.wildfly.remoting.http-listener-registry";

    /**
     * The capability for the Http Upgrade Registry
     *
     * @see <a href="https://github.com/wildfly/wildfly-capabilities/blob/master/org/wildfly/undertow/http-upgrade-registry/capability.adoc">documentation</a>
     */
    static final String HTTP_UPGRADE_REGISTRY_CAPABILITY_NAME = "org.wildfly.undertow.listener.http-upgrade-registry";

    /**
     * The capability for the SocketBinding capability
     *
     * @see <a href="https://github.com/wildfly/wildfly-capabilities/blob/master/org/wildfly/network/socket-binding/capability.adoc">documentation</a>
     */
    static final RuntimeCapability<Void> SOCKET_BINDING_CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.network.socket-binding", true, SocketBinding.class)
            .build();

    /**
     * A capability for the current messaging-activemq server configuration.
     *
     * @see <a href="https://github.com/wildfly/wildfly-capabilities/blob/master/org/wildfly/network/outbound-socket-binding/capability.adoc">Capability documentation</a>
     */
    static final RuntimeCapability<Void> OUTBOUND_SOCKET_BINDING_CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.network.outbound-socket-binding", true, OutboundSocketBinding.class)
            .build();
}
