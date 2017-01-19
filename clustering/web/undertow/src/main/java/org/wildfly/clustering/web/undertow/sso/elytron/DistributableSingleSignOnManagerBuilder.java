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

import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.SimpleBuilder;
import org.wildfly.clustering.web.sso.SSOManager;
import org.wildfly.clustering.web.sso.SSOManagerFactory;
import org.wildfly.clustering.web.sso.SSOManagerFactoryBuilderProvider;
import org.wildfly.clustering.web.undertow.sso.SSOManagerBuilder;
import org.wildfly.extension.undertow.ApplicationSecurityDomainDefinition;
import org.wildfly.extension.undertow.security.sso.DistributableApplicationSecurityDomainSingleSignOnManagerBuilder;
import org.wildfly.security.http.util.sso.SingleSignOnManager;

import io.undertow.server.session.SessionIdGenerator;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(DistributableApplicationSecurityDomainSingleSignOnManagerBuilder.class)
public class DistributableSingleSignOnManagerBuilder implements DistributableApplicationSecurityDomainSingleSignOnManagerBuilder, Value<SingleSignOnManager> {

    private static final SSOManagerFactoryBuilderProvider<Batch> PROVIDER = StreamSupport.stream(ServiceLoader.load(SSOManagerFactoryBuilderProvider.class, SSOManagerFactoryBuilderProvider.class.getClassLoader()).spliterator(), false).findFirst().get();

    @SuppressWarnings("rawtypes")
    private final InjectedValue<SSOManager> manager = new InjectedValue<>();

    @Override
    public ServiceBuilder<SingleSignOnManager> build(ServiceTarget target, ServiceName name, CapabilityServiceSupport support, String securityDomainName, SessionIdGenerator generator) {
        ServiceName securityDomainServiceName = support.getCapabilityServiceName(ApplicationSecurityDomainDefinition.APPLICATION_SECURITY_DOMAIN_CAPABILITY, securityDomainName);

        Builder<SSOManagerFactory<ElytronAuthentication, String, Map.Entry<String, URI>, Batch>> factoryBuilder = PROVIDER.<ElytronAuthentication, String, Map.Entry<String, URI>>getBuilder(securityDomainName).configure(support);
        Builder<SessionIdGenerator> generatorBuilder = new SimpleBuilder<>(securityDomainServiceName.append("generator"), generator);
        Builder<SSOManager<ElytronAuthentication, String, Map.Entry<String, URI>, LocalSSOContext, Batch>> managerBuilder = new SSOManagerBuilder<>(factoryBuilder.getServiceName(), generatorBuilder.getServiceName(), new LocalSSOContextFactory());

        Arrays.asList(factoryBuilder, generatorBuilder, managerBuilder).forEach(builder -> builder.build(target).setInitialMode(ServiceController.Mode.ON_DEMAND).install());

        return target.addService(name, new ValueService<>(this))
                .addDependency(managerBuilder.getServiceName(), SSOManager.class, this.manager)
                .setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public SingleSignOnManager getValue() {
        return new DistributableSingleSignOnManager(this.manager.getValue());
    }
}
