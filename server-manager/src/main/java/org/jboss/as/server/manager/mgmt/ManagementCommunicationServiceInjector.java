/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server.manager.mgmt;

import org.jboss.as.protocol.mgmt.ManagementOperationHandler;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.value.Value;

/**
 * Injector used to add a {@link org.jboss.as.protocol.mgmt.ManagementOperationHandler} to the
 * {@link org.jboss.as.server.manager.mgmt.ManagementCommunicationService}.
 *
 * @author John Bailey
 */
public class ManagementCommunicationServiceInjector implements Injector<ManagementCommunicationService> {
    private final Value<? extends ManagementOperationHandler> handlerValue;
    private ManagementCommunicationService communicationService;

    /**
     * Create an instance with a handler value.
     *
     * @param handlerValue The handler value
     */
    public ManagementCommunicationServiceInjector(Value<? extends ManagementOperationHandler> handlerValue) {
        this.handlerValue = handlerValue;
    }

    /**
     * Add the {@link org.jboss.as.protocol.mgmt.ManagementOperationHandler} to the injected
     * {@link org.jboss.as.server.manager.mgmt.ManagementCommunicationService}.
     *
     * @param value The ManagementCommunicationService
     * @throws InjectionException
     */
    public synchronized void inject(ManagementCommunicationService value) throws InjectionException {
        this.communicationService = value;
        communicationService.addHandler(handlerValue.getValue());
    }

    /**
     * Remove the {@link org.jboss.as.protocol.mgmt.ManagementOperationHandler} from the injected
     * {@link org.jboss.as.server.manager.mgmt.ManagementCommunicationService}.
     */
    public synchronized void uninject() {
        if(communicationService  != null) {
            communicationService.removeHandler(handlerValue.getValue());
        }

    }
}
