/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.weld.deployment.processors;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.weld.bootstrap.api.Environment;
import org.jboss.weld.bootstrap.api.Environments;
import org.jboss.weld.bootstrap.api.Service;
import org.jboss.weld.injection.spi.ResourceInjectionServices;
import org.jboss.weld.resources.spi.ResourceLoader;
import org.jboss.weld.security.spi.SecurityServices;

/**
 * Since not all Weld subsystem submodules must be always present (e.g. when using WildFly Swarm) we cannot use {@link Environments#EE_INJECT}.
 *
 * @author Martin Kouba
 */
class WildFlyWeldEnvironment implements Environment {

    static final WildFlyWeldEnvironment INSTANCE = new WildFlyWeldEnvironment();

    private final Set<Class<? extends Service>> requiredDeploymentServices;

    private final Set<Class<? extends Service>> requiredBeanDeploymentArchiveServices;

    private WildFlyWeldEnvironment() {
        this.requiredDeploymentServices = Collections.singleton(SecurityServices.class);
        Set<Class<? extends Service>> beanDeploymentArchiveServices = new HashSet<>();
        beanDeploymentArchiveServices.add(ResourceLoader.class);
        beanDeploymentArchiveServices.add(ResourceInjectionServices.class);
        this.requiredBeanDeploymentArchiveServices = Collections.unmodifiableSet(beanDeploymentArchiveServices);
    }

    @Override
    public Set<Class<? extends Service>> getRequiredDeploymentServices() {
        return requiredDeploymentServices;
    }

    @Override
    public Set<Class<? extends Service>> getRequiredBeanDeploymentArchiveServices() {
        return requiredBeanDeploymentArchiveServices;
    }

}
