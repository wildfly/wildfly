/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.capedwarf.services;

import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jgroups.Address;
import org.jgroups.Channel;

/**
 * Master info service
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class MasterInfoService implements Service<Address> {

    public static final ServiceName NAME = ServiceName.JBOSS.append("capedwarf").append("master");

    private InjectedValue<Channel> channel = new InjectedValue<Channel>();
    private InjectedValue<ServiceBasedNamingStore> namingStoreValue = new InjectedValue<ServiceBasedNamingStore>();

    /**
     * Bind the entry into the injected context.
     *
     * @param context The start context
     * @throws StartException If the entity can not be bound
     */
    public synchronized void start(StartContext context) throws StartException {
        final ServiceBasedNamingStore namingStore = namingStoreValue.getValue();
        ServiceController<?> controller = context.getController();
        namingStore.add(controller.getName());
    }

    /**
     * Unbind the entry from the injected context.
     *
     * @param context The stop context
     */
    public synchronized void stop(StopContext context) {
        final ServiceBasedNamingStore namingStore = namingStoreValue.getValue();
        namingStore.remove(context.getController().getName());
    }

    public Address getValue() throws IllegalStateException, IllegalArgumentException {
        return channel.getValue().getAddress();
    }

    public InjectedValue<Channel> getChannel() {
        return channel;
    }

    public Injector<ServiceBasedNamingStore> getNamingStoreInjector() {
        return namingStoreValue;
    }
}
