/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.undertow;

/**
 *
 * Service that provides the default session cookie config. The config can be overriden on a per
 * app basis, and may not be present.
 *
 * @author Stuart Douglas
 */
public class SessionCookieConfig {

    private final String name;
    private final String domain;
    private final String comment;
    private final Boolean httpOnly;
    private final Boolean secure;
    private final Integer maxAge;

    public SessionCookieConfig(final String name, final String domain, final String comment, final Boolean httpOnly, final Boolean secure, final Integer maxAge) {
        this.name = name;
        this.domain = domain;
        this.comment = comment;
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

    public String getComment() {
        return comment;
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
