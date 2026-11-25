/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import javax.net.ssl.SSLContext;
import org.jboss.as.controller.capability.RuntimeCapability;

/**
 * Capabilities for the messaging-activemq extension. This is not to be used outside of this extension.
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2017 Red Hat Inc.
 */
public class Capabilities {

    /**
     * Represents the data source capability
     *
     * @see <a href="https://github.com/wildfly/wildfly-capabilities/blob/main/org/wildfly/data-source/capability.adoc">Capability documentation</a>
     */
    static final String DATA_SOURCE_CAPABILITY = "org.wildfly.data-source";

    /**
     * The capability name for the JMX management.
     *
     * @see <a href="https://github.com/wildfly/wildfly-capabilities/blob/main/org/wildfly/management/jmx/capability.adoc">Capability documentation</a>
     */
    static final String JMX_CAPABILITY = "org.wildfly.management.jmx";

    /**
     * The capability name for the current messaging-activemq server configuration.
     *
     * @see <a href="https://github.com/wildfly/wildfly-capabilities/blob/main/org/wildfly/messaging/activemq/server/capability.adoc">Capability documentation</a>
     */
    static final String ACTIVEMQ_SERVER_CAPABILITY_NAME = "org.wildfly.messaging.activemq.server";

    /**
     * A capability for the current messaging-activemq server configuration.
     *
     * @see <a href="https://github.com/wildfly/wildfly-capabilities/blob/main/org/wildfly/messaging/activemq/server/capability.adoc">Capability documentation</a>
     */
    static final RuntimeCapability<Void> ACTIVEMQ_SERVER_CAPABILITY = RuntimeCapability.Builder.of(ACTIVEMQ_SERVER_CAPABILITY_NAME, true, ActiveMQBroker.class)
            //.addRuntimeOnlyRequirements(JMX_CAPABILITY) -- has no function so don't use it
            .build();

    /**
     * The capability name  for the legacy security domain.
     * @see <a href="https://github.com/wildfly/wildfly-capabilities/blob/main/org/wildfly/security/security-domain/capability.adoc">Capability documentation</a>
     */
    static final RuntimeCapability<Void> LEGACY_SECURITY_DOMAIN_CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.security.legacy-security-domain", true)
            .build();

    /**
     * The capability name for the Elytron security domain.
     * @see <a href="https://github.com/wildfly/wildfly-capabilities/blob/master/org/wildfly/security/security-domain/capability.adoc">documentation</a>
     */
    static final String ELYTRON_DOMAIN_CAPABILITY = "org.wildfly.security.security-domain";

    /**
     * The capability for the Http Listener Registry
     *
     * @see <a href="https://github.com/wildfly/wildfly-capabilities/blob/main/org/wildfly/remoting/http-listener-registry/capability.adoc">documentation</a>
     */
    static final String HTTP_LISTENER_REGISTRY_CAPABILITY_NAME = "org.wildfly.remoting.http-listener-registry";

    /**
     * The capability for the Http Upgrade Registry
     *
     * @see <a href="https://github.com/wildfly/wildfly-capabilities/blob/main/org/wildfly/undertow/listener/http-upgrade-registry/capability.adoc">documentation</a>
     */
    static final String HTTP_UPGRADE_REGISTRY_CAPABILITY_NAME = "org.wildfly.undertow.listener.http-upgrade-registry";

    /**
     * The capability name for the Elytron SSL context.
     *
     * @see <a href="https://github.com/wildfly/wildfly-capabilities/blob/master/org/wildfly/security/ssl-context/capability.adoc">documentation</a>
     */
    public static final String ELYTRON_SSL_CONTEXT_CAPABILITY_NAME = "org.wildfly.security.ssl-context";

    /**
     * The capability name for the Elytron SSL context.
     *
     * @see <a href="https://github.com/wildfly/wildfly-capabilities/blob/master/org/wildfly/security/ssl-context/capability.adoc">documentation</a>
     */
    public static final RuntimeCapability<Void> ELYTRON_SSL_CONTEXT_CAPABILITY = RuntimeCapability.Builder.of(ELYTRON_SSL_CONTEXT_CAPABILITY_NAME, true, SSLContext.class).build();
}
