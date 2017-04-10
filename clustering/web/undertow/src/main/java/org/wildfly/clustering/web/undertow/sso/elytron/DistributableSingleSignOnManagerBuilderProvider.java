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

package org.wildfly.clustering.web.undertow.sso.elytron;

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.StreamSupport;

import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.as.clustering.controller.SimpleCapabilityServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.web.sso.SSOManagerFactoryBuilderProvider;
import org.wildfly.extension.undertow.security.sso.DistributableSecurityDomainSingleSignOnManagerBuilderProvider;
import org.wildfly.security.http.util.sso.DefaultSingleSignOnManager;
import org.wildfly.security.http.util.sso.SingleSignOnManager;

import io.undertow.server.session.SessionIdGenerator;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(DistributableSecurityDomainSingleSignOnManagerBuilderProvider.class)
public class DistributableSingleSignOnManagerBuilderProvider implements DistributableSecurityDomainSingleSignOnManagerBuilderProvider {

    @SuppressWarnings("rawtypes")
    private static final Optional<SSOManagerFactoryBuilderProvider> PROVIDER = StreamSupport.stream(ServiceLoader.load(SSOManagerFactoryBuilderProvider.class, SSOManagerFactoryBuilderProvider.class.getClassLoader()).spliterator(), false).findFirst();

    @Override
    public CapabilityServiceBuilder<SingleSignOnManager> getBuilder(ServiceName name, String securityDomainName, SessionIdGenerator generator) {
        @SuppressWarnings("unchecked")
        Optional<CapabilityServiceBuilder<SingleSignOnManager>> builder = PROVIDER.map(provider -> new DistributableSingleSignOnManagerBuilder(name, securityDomainName, generator, provider));
        return builder.orElse(new SimpleCapabilityServiceBuilder<>(name, new DefaultSingleSignOnManager(new ConcurrentHashMap<>(), generator::createSessionId)));
   }
}
