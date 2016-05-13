/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.singleton.service;

import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.singleton.SingletonDefaultRequirement;
import org.wildfly.clustering.singleton.SingletonPolicy;

/**
 * @author Paul Ferraro
 */
public class MyServicePolicyActivator implements ServiceActivator {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("test", "myservice", "default-policy");

    @Override
    public void activate(ServiceActivatorContext context) throws ServiceRegistryException {
        try {
            SingletonPolicy policy = (SingletonPolicy) context.getServiceRegistry().getRequiredService(ServiceName.parse(SingletonDefaultRequirement.SINGLETON_POLICY.getName())).awaitValue();
            InjectedValue<ServerEnvironment> env = new InjectedValue<>();
            MyService service = new MyService(env);
            policy.createSingletonServiceBuilder(SERVICE_NAME, service).build(context.getServiceTarget())
                    .addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, env)
                    .install();
        } catch (InterruptedException e) {
            throw new ServiceRegistryException(e);
        }
    }
}
