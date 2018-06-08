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

package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.subsystem.CacheResourceDefinition.Capability.CACHE;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.infinispan.Cache;
import org.jboss.as.clustering.infinispan.InfinispanXAResourceRecovery;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.tm.XAResourceRecovery;
import org.jboss.tm.XAResourceRecoveryRegistry;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.SimpleServiceNameProvider;

/**
 * Builder for a {@link XAResourceRecovery} registration.
 * @author Paul Ferraro
 */
public class XAResourceRecoveryServiceConfigurator extends SimpleServiceNameProvider implements ServiceConfigurator, Supplier<XAResourceRecovery>, Consumer<XAResourceRecovery> {

    private volatile Supplier<Cache<?, ?>> cache;
    private volatile Supplier<XAResourceRecoveryRegistry> registry;

    /**
     * Constructs a new {@link XAResourceRecovery} builder.
     */
    public XAResourceRecoveryServiceConfigurator(PathAddress cacheAddress) {
        super(CACHE.getServiceName(cacheAddress).append("recovery"));
    }

    @Override
    public XAResourceRecovery get() {
        Cache<?, ?> cache = this.cache.get();
        XAResourceRecovery recovery = new InfinispanXAResourceRecovery(cache);
        if (cache.getCacheConfiguration().transaction().recovery().enabled()) {
            this.registry.get().addXAResourceRecovery(recovery);
        }
        return recovery;
    }

    @Override
    public void accept(XAResourceRecovery recovery) {
        if (this.cache.get().getCacheConfiguration().transaction().recovery().enabled()) {
            this.registry.get().removeXAResourceRecovery(recovery);
        }
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        this.cache = builder.requires(this.getServiceName().getParent());
        this.registry = builder.requires(TxnServices.JBOSS_TXN_ARJUNA_RECOVERY_MANAGER);
        Consumer<XAResourceRecovery> recovery = builder.provides(this.getServiceName());
        Service service = new FunctionalService<>(recovery, Function.identity(), this, this);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.PASSIVE);
    }
}
