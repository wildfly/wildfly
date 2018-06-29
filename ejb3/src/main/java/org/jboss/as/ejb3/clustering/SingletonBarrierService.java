/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.clustering;

import static org.jboss.as.ejb3.subsystem.EJB3SubsystemRootResourceDefinition.CLUSTERED_SINGLETON_CAPABILITY;

import java.util.function.Supplier;

import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.clustering.singleton.service.SingletonPolicy;

/**
 * Service that installs a singleton service on service start using a singleton policy.
 *
 * @author Flavia Rainone
 */
public class SingletonBarrierService implements Service {
    public static final ServiceName SERVICE_NAME = CLUSTERED_SINGLETON_CAPABILITY.getCapabilityServiceName().append("barrier");

    private final Supplier<SingletonPolicy> policy;

    public SingletonBarrierService(Supplier<SingletonPolicy> policy) {
        this.policy = policy;
    }

    @Override
    public void start(StartContext context) {
        this.policy.get().createSingletonServiceConfigurator(CLUSTERED_SINGLETON_CAPABILITY.getCapabilityServiceName())
                .build(context.getChildTarget())
                .install();
    }

    @Override
    public void stop(StopContext context) {
    }
}