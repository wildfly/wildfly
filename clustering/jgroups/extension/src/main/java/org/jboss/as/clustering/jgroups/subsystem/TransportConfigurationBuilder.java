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

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import org.jboss.as.network.SocketBinding;
import org.jboss.as.threads.ThreadsServices;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.threads.JBossExecutors;
import org.wildfly.clustering.jgroups.spi.TransportConfiguration;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;

/**
 * @author Paul Ferraro
 */
public class TransportConfigurationBuilder extends AbstractProtocolConfigurationBuilder<TransportConfiguration> implements TransportConfiguration {

    private ValueDependency<SocketBinding> diagnosticsSocketBinding;
    private ValueDependency<ThreadFactory> threadFactory;
    private ValueDependency<ExecutorService> defaultExecutor = null;
    private ValueDependency<ExecutorService> oobExecutor = null;
    private ValueDependency<ScheduledExecutorService> timerExecutor = null;
    private boolean shared = TransportResourceDefinition.SHARED.getDefaultValue().asBoolean();
    private Topology topology = null;

    public TransportConfigurationBuilder(String stackName, String name) {
        super(stackName, name);
    }

    @Override
    public ServiceBuilder<TransportConfiguration> build(ServiceTarget target) {
        ServiceBuilder<TransportConfiguration> builder = super.build(target);
        if (this.diagnosticsSocketBinding != null) {
            this.diagnosticsSocketBinding.register(builder);
        }
        if (this.threadFactory != null) {
            this.threadFactory.register(builder);
        }
        if (this.defaultExecutor != null) {
            this.defaultExecutor.register(builder);
        }
        if (this.oobExecutor != null) {
            this.oobExecutor.register(builder);
        }
        if (this.timerExecutor != null) {
            this.timerExecutor.register(builder);
        }
        return builder;
    }

    @Override
    public TransportConfiguration getValue() {
        return this;
    }

    @Override
    public TransportConfigurationBuilder setModule(ModuleIdentifier module) {
        super.setModule(module);
        return this;
    }

    @Override
    public TransportConfigurationBuilder setSocketBinding(String socketBindingName) {
        super.setSocketBinding(socketBindingName);
        return this;
    }

    @Override
    public TransportConfigurationBuilder addProperty(String name, String value) {
        super.addProperty(name, value);
        return this;
    }

    public TransportConfigurationBuilder setDiagnosticsSocket(String socketBindingName) {
        if (socketBindingName != null) {
            this.diagnosticsSocketBinding = new InjectedValueDependency<>(SocketBinding.JBOSS_BINDING_NAME.append(socketBindingName), SocketBinding.class);
        }
        return this;
    }

    public TransportConfigurationBuilder setDefaultExecutor(String executorName) {
        if (executorName != null) {
            this.defaultExecutor = new InjectedValueDependency<>(ThreadsServices.executorName(executorName), ExecutorService.class);
        }
        return this;
    }

    public TransportConfigurationBuilder setOOBExecutor(String executorName) {
        if (executorName != null) {
            this.oobExecutor = new InjectedValueDependency<>(ThreadsServices.executorName(executorName), ExecutorService.class);
        }
        return this;
    }

    public TransportConfigurationBuilder setTimerExecutor(String executorName) {
        if (executorName != null) {
            this.timerExecutor = new InjectedValueDependency<>(ThreadsServices.executorName(executorName), ScheduledExecutorService.class);
        }
        return this;
    }

    public TransportConfigurationBuilder setThreadFactory(String threadFactoryName) {
        if (threadFactoryName != null) {
            this.threadFactory = new InjectedValueDependency<>(ThreadsServices.threadFactoryName(threadFactoryName), ThreadFactory.class);
        }
        return this;
    }

    public TransportConfigurationBuilder setShared(boolean shared) {
        this.shared = shared;
        return this;
    }

    public TransportConfigurationBuilder setTopology(final String site, final String rack, final String machine) {
        if ((site != null) || (rack != null) || (machine != null)) {
            this.topology = new Topology() {
                @Override
                public String getMachine() {
                    return machine;
                }

                @Override
                public String getRack() {
                    return rack;
                }

                @Override
                public String getSite() {
                    return site;
                }
            };
        }
        return this;
    }

    @Override
    public boolean isShared() {
        return this.shared;
    }

    @Override
    public SocketBinding getDiagnosticsSocketBinding() {
        return (this.diagnosticsSocketBinding != null) ? this.diagnosticsSocketBinding.getValue() : null;
    }

    @Override
    public ExecutorService getDefaultExecutor() {
        ExecutorService executor = (this.defaultExecutor != null) ? this.defaultExecutor.getValue() : null;
        return (executor != null) ? JBossExecutors.protectedExecutorService(executor) : null;
    }

    @Override
    public ExecutorService getOOBExecutor() {
        ExecutorService executor = (this.defaultExecutor != null) ? this.oobExecutor.getValue() : null;
        return (executor != null) ? JBossExecutors.protectedExecutorService(executor) : null;
    }

    @Override
    public ScheduledExecutorService getTimerExecutor() {
        ScheduledExecutorService executor = (this.timerExecutor != null) ? this.timerExecutor.getValue() : null;
        return (executor != null) ? JBossExecutors.protectedScheduledExecutorService(executor) : null;
    }

    @Override
    public ThreadFactory getThreadFactory() {
        return (this.threadFactory != null) ? this.threadFactory.getValue() : null;
    }

    @Override
    public Topology getTopology() {
        return this.topology;
    }
}
