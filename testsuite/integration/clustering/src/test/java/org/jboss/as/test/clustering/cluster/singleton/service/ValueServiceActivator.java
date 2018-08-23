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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.ImmediateValue;
import org.wildfly.clustering.singleton.SingletonDefaultRequirement;
import org.wildfly.clustering.singleton.SingletonPolicy;

/**
 * @author Paul Ferraro
 */
@SuppressWarnings("deprecation")
public class ValueServiceActivator implements ServiceActivator {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("test", "service", "value");

    @Override
    public void activate(ServiceActivatorContext context) {
        ServiceController<?> controller = context.getServiceRegistry().getRequiredService(ServiceName.parse(SingletonDefaultRequirement.SINGLETON_POLICY.getName()));
        try {
            SingletonPolicy policy = (SingletonPolicy) controller.awaitValue(30, TimeUnit.SECONDS);
            policy.createSingletonServiceBuilder(SERVICE_NAME, new ValueService<>(new ImmediateValue<>(Boolean.TRUE)), new ValueService<>(new ImmediateValue<>(Boolean.FALSE))).build(context.getServiceTarget()).install();
        } catch (TimeoutException e) {
            controller.getServiceContainer().dumpServices();
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
