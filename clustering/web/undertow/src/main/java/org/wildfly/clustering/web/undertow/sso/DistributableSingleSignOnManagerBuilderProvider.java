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

package org.wildfly.clustering.web.undertow.sso;

import java.util.ServiceLoader;

import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.as.clustering.controller.SimpleCapabilityServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.web.sso.SSOManagerFactoryBuilderProvider;
import org.wildfly.extension.undertow.security.sso.DistributableHostSingleSignOnManagerBuilderProvider;

import io.undertow.security.impl.InMemorySingleSignOnManager;
import io.undertow.security.impl.SingleSignOnManager;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(DistributableHostSingleSignOnManagerBuilderProvider.class)
public class DistributableSingleSignOnManagerBuilderProvider implements DistributableHostSingleSignOnManagerBuilderProvider {

    private static final SSOManagerFactoryBuilderProvider<Batch> PROVIDER = loadProvider();

    private static SSOManagerFactoryBuilderProvider<Batch> loadProvider() {
        for (SSOManagerFactoryBuilderProvider<Batch> provider : ServiceLoader.load(SSOManagerFactoryBuilderProvider.class, SSOManagerFactoryBuilderProvider.class.getClassLoader())) {
            return provider;
        }
        return null;
    }

    @Override
    public CapabilityServiceBuilder<SingleSignOnManager> getBuilder(ServiceName name, String serverName, String hostName) {
        return (PROVIDER != null) ? new DistributableSingleSignOnManagerBuilder(name, serverName, hostName, PROVIDER) : new SimpleCapabilityServiceBuilder<>(name, new InMemorySingleSignOnManager());
    }
}
