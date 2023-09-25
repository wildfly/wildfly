/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.undertow;

/**
 * Configuration object for a Cookie.
 * The config can be overridden on a per-app basis, and may not be present.
 *
 * @author Stuart Douglas
 */
public class CookieConfig {

    private final String name;
    private final String domain;
    private final Boolean httpOnly;
    private final Boolean secure;
    private final Integer maxAge;

    public CookieConfig(final String name, final String domain, final Boolean httpOnly, final Boolean secure, final Integer maxAge) {
        this.name = name;
        this.domain = domain;
        this.httpOnly = httpOnly;
        this.secure = secure;
        this.maxAge = maxAge;
    }

    public String getName() {
        return name;
    }

    public String getDomain() {
        return domain;
    }

    public Boolean getHttpOnly() {
        return httpOnly;
    }

    public Boolean getSecure() {
        return secure;
    }

    public Integer getMaxAge() {
        return maxAge;
    }
}
