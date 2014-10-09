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
package org.wildfly.clustering.server.provider;

import java.util.Arrays;
import java.util.Collection;

import org.jboss.as.clustering.naming.BinderServiceBuilder;
import org.jboss.as.clustering.naming.JndiNameFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.provider.ServiceProviderRegistrationFactory;
import org.wildfly.clustering.server.CacheServiceBuilder;
import org.wildfly.clustering.spi.CacheServiceInstaller;
import org.wildfly.clustering.spi.CacheServiceNames;
import org.wildfly.clustering.spi.GroupServiceNameFactory;

/**
 * @author Paul Ferraro
 */
public class ServiceProviderRegistrationFactoryServiceInstaller implements CacheServiceInstaller {
    private final Logger logger = Logger.getLogger(this.getClass());

    private final CacheServiceBuilder<ServiceProviderRegistrationFactory> builder;

    protected ServiceProviderRegistrationFactoryServiceInstaller(CacheServiceBuilder<ServiceProviderRegistrationFactory> builder) {
        this.builder = builder;
    }

    private static ContextNames.BindInfo createBinding(String container, String cache) {
        return ContextNames.bindInfoFor(JndiNameFactory.createJndiName(JndiNameFactory.DEFAULT_JNDI_NAMESPACE, GroupServiceNameFactory.BASE_NAME, CacheServiceNames.SERVICE_PROVIDER_REGISTRATION.toString(), container, cache).getAbsoluteName());
    }

    @Override
    public Collection<ServiceName> getServiceNames(String container, String cache) {
        return Arrays.asList(CacheServiceNames.SERVICE_PROVIDER_REGISTRATION.getServiceName(container, cache), createBinding(container, cache).getBinderServiceName());
    }

    @Override
    public void install(ServiceTarget target, String container, String cache) {
        ServiceName name = CacheServiceNames.SERVICE_PROVIDER_REGISTRATION.getServiceName(container, cache);
        ContextNames.BindInfo bindInfo = createBinding(container, cache);

        this.logger.debugf("Installing %s service, bound to ", name.getCanonicalName(), bindInfo.getAbsoluteJndiName());

        this.builder.build(target, name, container, cache).setInitialMode(ServiceController.Mode.ON_DEMAND).install();

        new BinderServiceBuilder(target).build(bindInfo, name, ServiceProviderRegistrationFactory.class).install();
    }
}
