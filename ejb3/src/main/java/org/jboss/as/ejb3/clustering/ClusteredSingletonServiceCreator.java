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

import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.singleton.SingletonPolicy;

import static org.jboss.as.ejb3.subsystem.EJB3SubsystemRootResourceDefinition.CLUSTERED_SINGLETON_CAPABILITY;

/**
 * Service that creates clustered singleton service at runtime, using a singleton policy capability service for that.
 *
 * @author Flavia Rainone
 */
public class ClusteredSingletonServiceCreator extends AbstractService<Void> {

    private final InjectedValue<SingletonPolicy> singletonPolicy;

    public ClusteredSingletonServiceCreator() {
        this.singletonPolicy = new InjectedValue<>();
    }

    public InjectedValue<SingletonPolicy> getSingletonPolicy() {
        return singletonPolicy;
    }

    @Override public void start(StartContext context) throws StartException {
        final ServiceTarget target = context.getChildTarget();
        final SingletonPolicy singletonPolicyValue = singletonPolicy.getValue();
        singletonPolicyValue.createSingletonServiceBuilder(CLUSTERED_SINGLETON_CAPABILITY.getCapabilityServiceName(), new ClusteredSingletonService())
                .build(target)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install();
    }
}