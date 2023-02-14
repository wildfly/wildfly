/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow.session;

import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionCookieConfig;
import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.SessionConfigWrapper;
import org.jboss.as.web.session.AffinityLocator;
import org.wildfly.extension.undertow.CookieConfig;

/**
 * Adds affinity locator handling to a {@link SessionConfig}.
 *
 * @author Radoslav Husar
 */
public class AffinitySessionConfigWrapper implements SessionConfigWrapper {

    private final SessionConfig sessionCookieConfig;
    private final AffinityLocator locator;

    public AffinitySessionConfigWrapper(CookieConfig config, AffinityLocator locator) {
        SessionCookieConfig sessionCookieConfig = new SessionCookieConfig();

        sessionCookieConfig.setCookieName(config.getName());
        if (config.getDomain() != null) {
            sessionCookieConfig.setDomain(config.getDomain());
        }
        if (config.getHttpOnly() != null) {
            sessionCookieConfig.setHttpOnly(config.getHttpOnly());
        }
        if (config.getSecure() != null) {
            sessionCookieConfig.setSecure(config.getSecure());
        }
        if (config.getMaxAge() != null) {
            sessionCookieConfig.setMaxAge(config.getMaxAge());
        }

        this.sessionCookieConfig = sessionCookieConfig;
        this.locator = locator;
    }

    @Override
    public SessionConfig wrap(SessionConfig sessionConfig, Deployment deployment) {
        // neeeds to depend on the session cookie source that i am wrapping
//        sessionConfig.sessionCookieSource()


        return new AffinitySessionConfig(sessionConfig, this.sessionCookieConfig, this.locator);
    }
}
