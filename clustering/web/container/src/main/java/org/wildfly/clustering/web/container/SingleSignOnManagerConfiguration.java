/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.container;

import java.util.function.Supplier;

/**
 * Defines the container configuration for security domain-based single sign-on management.
 * @author Paul Ferraro
 */
public interface SingleSignOnManagerConfiguration {
    /**
     * Returns the name of the associated security domain
     * @return a security domain name
     */
    String getSecurityDomainName();

    /**
     * Returns the identifier generator to be used for generating single sign-on identifiers.
     * @return an identifier generator
     */
    Supplier<String> getIdentifierGenerator();
}
