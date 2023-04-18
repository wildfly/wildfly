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

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import io.undertow.server.session.PathParameterSessionConfig;
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

    private final Map<SessionConfig.SessionCookieSource, SessionConfig> affinityConfigMap = new EnumMap<>(SessionConfig.SessionCookieSource.class);
    private final AffinityLocator locator;

    public AffinitySessionConfigWrapper(CookieConfig config, AffinityLocator locator) {
        this.locator = locator;

        // Setup SessionCookieSource->SessionConfig mapping:

        // SessionConfig.SessionCookieSource.COOKIE
        SessionCookieConfig cookieSessionConfig = new SessionCookieConfig();

        cookieSessionConfig.setCookieName(config.getName());
        if (config.getDomain() != null) {
            cookieSessionConfig.setDomain(config.getDomain());
        }
        if (config.getHttpOnly() != null) {
            cookieSessionConfig.setHttpOnly(config.getHttpOnly());
        }
        if (config.getSecure() != null) {
            cookieSessionConfig.setSecure(config.getSecure());
        }
        if (config.getMaxAge() != null) {
            cookieSessionConfig.setMaxAge(config.getMaxAge());
        }

        affinityConfigMap.put(SessionConfig.SessionCookieSource.COOKIE, cookieSessionConfig);

        // SessionConfig.SessionCookieSource.URL

        // In this case use the cookie name as the path parameter
        String pathParameterName = config.getName().toLowerCase(Locale.ENGLISH);
        PathParameterSessionConfig pathParameterSessionConfig = new PathParameterSessionConfig(pathParameterName);
        affinityConfigMap.put(SessionConfig.SessionCookieSource.URL, pathParameterSessionConfig);
    }

    @Override
    public SessionConfig wrap(SessionConfig sessionConfig, Deployment deployment) {
        return new AffinitySessionConfig(sessionConfig, this.affinityConfigMap, this.locator);
    }
}
