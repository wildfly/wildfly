/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.ejb.infinispan.timer;

import java.security.PrivilegedAction;
import java.util.function.Function;

import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassResolver;
import org.jboss.modules.Module;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.ejb.timer.LegacyTimerManagementProviderFactory;
import org.wildfly.clustering.ejb.timer.TimerManagementProvider;
import org.wildfly.clustering.marshalling.jboss.DynamicExternalizerObjectTable;
import org.wildfly.clustering.marshalling.jboss.JBossByteBufferMarshaller;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshallingConfigurationRepository;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(LegacyTimerManagementProviderFactory.class)
public class InfinispanLegacyTimerManagementProviderFactory implements LegacyTimerManagementProviderFactory, Function<Module, ByteBufferMarshaller>, InfinispanTimerManagementConfiguration, PrivilegedAction<Boolean> {

    private static final String DISTRIBUTED_TIMER_SERVICE_ENABLED = "jboss.ejb.timer-service.distributed.enabled";

    @Override
    public Boolean run() {
        return Boolean.getBoolean(DISTRIBUTED_TIMER_SERVICE_ENABLED);
    }

    @Override
    public TimerManagementProvider createTimerManagementProvider() {
        return WildFlySecurityManager.doUnchecked(this) ? new InfinispanTimerManagementProvider(this) : null;
    }

    @Override
    public ByteBufferMarshaller apply(Module module) {
        MarshallingConfiguration config = new MarshallingConfiguration();
        config.setClassResolver(ModularClassResolver.getInstance(module.getModuleLoader()));
        config.setObjectTable(new DynamicExternalizerObjectTable(module.getClassLoader()));
        return new JBossByteBufferMarshaller(new SimpleMarshallingConfigurationRepository(config), module.getClassLoader());
    }

    @Override
    public Function<Module, ByteBufferMarshaller> getMarshallerFactory() {
        return this;
    }

    @Override
    public String getContainerName() {
        return "ejb";
    }

    @Override
    public String getCacheName() {
        return null;
    }

    @Override
    public Integer getMaxActiveTimers() {
        return null;
    }
}
