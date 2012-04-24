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

package org.jboss.as.controller;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Exposes the current {@link ControlledProcessState.State} and allows services to register a listener for changes
 * to it.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ControlledProcessStateService implements Service<ControlledProcessStateService> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("controlled-process-state");

    public static ServiceController<ControlledProcessStateService> addService(ServiceTarget target,
                                                                              ControlledProcessState processState) {
        ControlledProcessStateService service = processState.getService();
        return target.addService(SERVICE_NAME, service).install();
    }

    private ControlledProcessState.State processState;
    private final PropertyChangeSupport changeSupport;

    ControlledProcessStateService(ControlledProcessState.State initialState) {
        this.processState = initialState;
        changeSupport = new PropertyChangeSupport(this);
    }

    @Override
    public void start(StartContext context) throws StartException {
    }

    @Override
    public void stop(StopContext context) {
    }

    @Override
    public ControlledProcessStateService getValue() {
        return this;
    }

    /**
     * Returns the current process state.
     *
     * @return  the current state
     *
     * @deprecated This service should not be used outside the AS7 codebase itself, as it may be replaced in AS 7.2
     */
    @Deprecated
    public ControlledProcessState.State getCurrentState() {
        return processState;
    }

    /**
     * Registers a listener for changes to the process state.
     *
     * @param listener the listener
     *
     * @deprecated This service should not be used outside the AS7 codebase itself, as it may be replaced in AS 7.2
     */
    @Deprecated
    public void addPropertyChangeListener(
            PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Removes a previously {@link #addPropertyChangeListener(PropertyChangeListener) registered listener}.
     *
     * @param listener the listener
     *
     * @deprecated This service should not be used outside the AS7 codebase itself, as it may be replaced in AS 7.2
     */
    @Deprecated
    public void removePropertyChangeListener(
            PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }

    synchronized void stateChanged(ControlledProcessState.State newState) {
        final ControlledProcessState.State oldState = processState;
        processState = newState;
        changeSupport.firePropertyChange("currentState", oldState, newState);
    }
}
