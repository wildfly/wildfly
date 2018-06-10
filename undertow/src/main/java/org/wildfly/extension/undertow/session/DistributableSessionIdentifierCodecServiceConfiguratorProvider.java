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
package org.wildfly.extension.undertow.session;

import java.util.Collection;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.web.session.SessionIdentifierCodec;
import org.jboss.msc.service.ServiceName;

/**
 * Builds a {@link SessionIdentifierCodec} service.
 * @author Paul Ferraro
 */
public interface DistributableSessionIdentifierCodecServiceConfiguratorProvider {

    Optional<DistributableSessionIdentifierCodecServiceConfiguratorProvider> INSTANCE = StreamSupport.stream(ServiceLoader.load(DistributableSessionIdentifierCodecServiceConfiguratorProvider.class, DistributableSessionIdentifierCodecServiceConfiguratorProvider.class.getClassLoader()).spliterator(), false).findFirst();

    /**
     * Builds a {@link SessionIdentifierCodec} service.
     * @param target a service target
     * @param name a service name
     * @param deploymentServiceName the service name of the deployment
     * @return a service builder
     */
    CapabilityServiceConfigurator getDeploymentServiceConfigurator(ServiceName name, String serverName, String deploymentName);

    /**
     * Builds cross-deployment dependencies needed for route handling
     * @param target the service target
     * @return a service builder
     */
    Collection<CapabilityServiceConfigurator> getServerServiceConfigurators(String serverName);
}
