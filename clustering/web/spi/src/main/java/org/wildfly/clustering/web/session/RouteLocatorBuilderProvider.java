/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.web.session;

import org.jboss.msc.value.Value;
import org.wildfly.clustering.service.Builder;

/**
 * Builds a {@link RouteLocator} service.
 * @author Paul Ferraro
 */
public interface RouteLocatorBuilderProvider {
    /**
     * Builds a {@link RouteLocator} service.
     * @param deploymentName the name of the web deployment
     * @return a service builder
     */
    Builder<RouteLocator> getRouteLocatorBuilder(String deploymentName);

    /**
     * Builds the server dependencies to be made available to every deployment.
     * @param route the injected route source
     * @return a service builder
     */
    Builder<?> getRouteLocatorConfigurationBuilder(Value<? extends Value<String>> route);
}
