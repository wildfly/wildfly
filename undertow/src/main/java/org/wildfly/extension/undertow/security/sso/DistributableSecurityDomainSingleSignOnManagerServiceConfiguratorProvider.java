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

package org.wildfly.extension.undertow.security.sso;

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.msc.service.ServiceName;
import org.wildfly.security.http.util.sso.SingleSignOnManager;

import io.undertow.server.session.SessionIdGenerator;

/**
 * @author Paul Ferraro
 */
public interface DistributableSecurityDomainSingleSignOnManagerServiceConfiguratorProvider {

    Optional<DistributableSecurityDomainSingleSignOnManagerServiceConfiguratorProvider> INSTANCE = StreamSupport.stream(ServiceLoader.load(DistributableSecurityDomainSingleSignOnManagerServiceConfiguratorProvider.class, DistributableSecurityDomainSingleSignOnManagerServiceConfiguratorProvider.class.getClassLoader()).spliterator(), false).findFirst();

    /**
     * Returns a builder of a SingleSignOnManagerFactory service for a security domain.
     * @param name the service name
     * @param securityDomainName the name of the target security domain
     * @param generator a generator of unique identifiers
     * @return builder for a service providing a {@link SingleSignOnManager}
     */
    CapabilityServiceConfigurator getServiceConfigurator(ServiceName name, String securityDomainName, SessionIdGenerator generator);
}
