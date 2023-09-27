/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.management.jca;

/**
 * Type of connection security.
 *
 * @author Flavia Rainone
 */
public enum ConnectionSecurityType {
    // PicketBox security domain
    SECURITY_DOMAIN,
    // PicketBox security domain and application
    SECURITY_DOMAIN_AND_APPLICATION,
    // Application managed security
    APPLICATION,
    // Elytron managed security, with current authentication context
    ELYTRON,
    // Elytron managed security, with specified authentication context
    ELYTRON_AUTHENTICATION_CONTEXT,
    // Elytron and Application managed security, with specified authentication context
    ELYTRON_AUTHENTICATION_CONTEXT_AND_APPLICATION,
    // user name and password
    USER_PASSWORD
}
