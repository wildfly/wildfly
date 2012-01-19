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

package org.jboss.as.remoting.management;

import org.jboss.as.remoting.RemotingServices;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.remoting3.Registration;

import java.util.ArrayList;

/**
 * A basic registry for opened management channels. Other services like the {@code ServerInventory}
 * can add a dependency in order to prevent that connections are closed too early in the shutdown process.
 *
 * @author Emanuel Muckenhuber
 */
public class ManagementChannelRegistryService implements Service<ManagementChannelRegistryService> {

    /** The service name. */
    public static final ServiceName SERVICE_NAME = RemotingServices.REMOTING_BASE.append("management", "channel", "registry");
    private final ArrayList<Registration> registrations = new ArrayList<Registration>();

    public static void addService(final ServiceTarget serviceTarget) {
        serviceTarget.addService(SERVICE_NAME, new ManagementChannelRegistryService())
                // Make sure the endpoint service does not close all connections
                .addDependency(ManagementRemotingServices.MANAGEMENT_ENDPOINT)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
    }

    protected ManagementChannelRegistryService() {
        //
    }

    @Override
    public synchronized void start(StartContext context) throws StartException {
        // nothing
    }

    @Override
    public synchronized void stop(final StopContext context) {
        final ArrayList<Registration> registrations = this.registrations;
        for(final Registration registration : registrations) {
            registration.close();
        }
        registrations.clear();
    }

    public synchronized void register(final Registration registration) {
        registrations.add(registration);
    }

    @Override
    public ManagementChannelRegistryService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

}
