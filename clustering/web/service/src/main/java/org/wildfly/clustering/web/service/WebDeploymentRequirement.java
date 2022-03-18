/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.service;

import org.jboss.as.clustering.controller.UnaryRequirementServiceNameFactory;
import org.jboss.as.clustering.controller.UnaryServiceNameFactory;
import org.jboss.as.clustering.controller.UnaryServiceNameFactoryProvider;
import org.wildfly.clustering.service.UnaryRequirement;
import org.wildfly.clustering.web.routing.RouteLocator;
import org.wildfly.clustering.web.session.SessionManagerFactory;
import org.wildfly.clustering.web.sso.SSOManagerFactory;

/**
 * @author Paul Ferraro
 */
public enum WebDeploymentRequirement implements UnaryRequirement, UnaryServiceNameFactoryProvider {
    LOCAL_ROUTE("org.wildfly.clustering.web.local-route", String.class),
    ROUTE_LOCATOR("org.wildfly.clustering.web.route-locator", RouteLocator.class),
    SESSION_MANAGER_FACTORY("org.wildfly.clustering.web.session-manager-factory", SessionManagerFactory.class),
    SSO_MANAGER_FACTORY("org.wildfly.clustering.web.single-sign-on-manager-factory", SSOManagerFactory.class),
    ;
    private final String name;
    private final Class<?> type;
    private final UnaryServiceNameFactory factory;

    WebDeploymentRequirement(String name, Class<?> type) {
        this.name = name;
        this.type = type;
        this.factory = new UnaryRequirementServiceNameFactory(this);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Class<?> getType() {
        return this.type;
    }

    @Override
    public UnaryServiceNameFactory getServiceNameFactory() {
        return this.factory;
    }
}
