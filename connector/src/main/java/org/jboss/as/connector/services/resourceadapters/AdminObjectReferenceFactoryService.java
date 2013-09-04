/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.connector.services.resourceadapters;

import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ValueManagedReference;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.InjectedValue;

/**
 * Service responsible for exposing a {@link ManagedReferenceFactory} for an admin object
 * @author @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 */
public class AdminObjectReferenceFactoryService implements Service<ManagedReferenceFactory>, ManagedReferenceFactory {
    public static final ServiceName SERVICE_NAME_BASE =
        ServiceName.JBOSS.append("connector").append("admin-object").append("reference-factory");

    private final InjectedValue<Object> adminObjectValue = new InjectedValue<Object>();
    private ManagedReference reference;

    public synchronized void start(StartContext startContext) throws StartException {
        reference = new ValueManagedReference(new ImmediateValue<Object>(adminObjectValue.getValue()));
    }

    public synchronized void stop(StopContext stopContext) {
        reference = null;
    }

    public synchronized ManagedReferenceFactory getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public synchronized ManagedReference getReference() {
        return reference;
    }

    public Injector<Object> getAdminObjectInjector() {
        return adminObjectValue;
    }
}
