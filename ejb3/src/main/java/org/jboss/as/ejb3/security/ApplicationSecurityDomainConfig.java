/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.security;

import java.util.Objects;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ApplicationSecurityDomainConfig {

    private String name;
    private boolean enableJacc;
    private boolean legacyCompliantPrincipalPropagation;

    public ApplicationSecurityDomainConfig(String name, boolean enableJacc, boolean legacyCompliantPrincipalPropagation) {
        this.name = name;
        this.enableJacc = enableJacc;
        this.legacyCompliantPrincipalPropagation = legacyCompliantPrincipalPropagation;
    }

    public boolean isSameDomain(String other) {
        return name.equals(other);
    }

    public boolean isEnableJacc() {
        return enableJacc;
    }

    public boolean isLegacyCompliantPrincipalPropagation() {
        return legacyCompliantPrincipalPropagation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApplicationSecurityDomainConfig that = (ApplicationSecurityDomainConfig) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
