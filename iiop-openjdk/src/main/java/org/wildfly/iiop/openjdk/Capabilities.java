/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.iiop.openjdk;

/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */

final class Capabilities {
    /*
   Capabilities in this subsystem
    */
    public static final String IIOP_CAPABILITY = "org.wildfly.iiop";

    /*
    References to capabilities outside of the subsystem
     */

    public static final String LEGACY_SECURITY = "org.wildfly.legacy-security";
    public static final String LEGACY_SECURITY_DOMAIN_CAPABILITY = "org.wildfly.security.legacy-security-domain";

    public static final String SSL_CONTEXT_CAPABILITY = "org.wildfly.security.ssl-context";
    public static final String AUTH_CONTEXT_CAPABILITY = "org.wildfly.security.authentication-context";
}