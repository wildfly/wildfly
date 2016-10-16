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

package org.wildfly.extension.undertow.security.sso;

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

import io.undertow.security.impl.SingleSignOnManager;

/**
 * Builds a distrubutable {@link SingleSignOnManagerFactory} service.
 * @author Paul Ferraro
 */
public interface DistributableSingleSignOnManagerBuilder {

    Optional<DistributableSingleSignOnManagerBuilder> INSTANCE = StreamSupport.stream(ServiceLoader.load(DistributableSingleSignOnManagerBuilder.class, DistributableSingleSignOnManagerBuilder.class.getClassLoader()).spliterator(), false).findFirst();

    /**
     * Builds a SingleSignOnManagerFactory service for a host.
     * @param target the service target
     * @param name the service name
     * @param hostServiceName the service name of the host
     * @return a service builder
     */
    ServiceBuilder<SingleSignOnManager> build(ServiceTarget target, ServiceName name, CapabilityServiceSupport support, String serverName, String hostName);
}
