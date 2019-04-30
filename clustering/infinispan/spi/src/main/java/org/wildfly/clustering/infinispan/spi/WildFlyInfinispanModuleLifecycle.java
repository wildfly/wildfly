/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.infinispan.spi;

import org.infinispan.lifecycle.ModuleLifecycle;
import org.infinispan.factories.annotations.InfinispanModule;

/**
 * @author Paul Ferraro
 */
@InfinispanModule(name = "wildfly", requiredModules = "core")
public class WildFlyInfinispanModuleLifecycle implements ModuleLifecycle {
/*
    @Override
    public void cacheManagerStarting(GlobalComponentRegistry globalRegistry, GlobalConfiguration configuration) {
        BasicComponentRegistry registry = globalRegistry.getComponent(BasicComponentRegistry.class);
        registry.registerComponent(componentType, instance, manageLifecycle).registerComponent(new LocalGlobalConfigurationManager(), GlobalConfigurationManager.class);
    }

    @Override
    public void cacheManagerStopping(GlobalComponentRegistry registry) {
        ModuleLifecycle.super.cacheManagerStopping(registry);
    }

    @Override
    public void cacheStarting(ComponentRegistry registry, Configuration configuration, String cacheName) {
        registry.getComponent(BasicComponentRegistry.class).replaceComponent(componentName, newInstance, manageLifecycle);
    }

    @Override
    public void cacheStopped(ComponentRegistry registry, String cacheName) {
    }
*/
}
