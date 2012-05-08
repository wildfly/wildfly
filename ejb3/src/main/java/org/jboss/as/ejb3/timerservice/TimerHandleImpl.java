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
package org.jboss.as.ejb3.timerservice;

import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.ejb.EJBException;
import javax.ejb.Timer;
import javax.ejb.TimerHandle;

import org.jboss.as.ejb3.timerservice.spi.TimedObjectInvoker;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;

import static org.jboss.as.ejb3.EjbMessages.MESSAGES;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class TimerHandleImpl implements TimerHandle {
    private static final long serialVersionUID = 1L;

    /**
     * Id of the target {@link TimedObjectInvoker}
     */
    private final String timedObjectId;

    /**
     * The service name of the timer service
     */
    private final String serviceName;

    /**
     * Each {@link TimedObjectInvoker} can have multiple timer instances.
     * This id corresponds to one such <i>instance</i>
     */
    private final String id;

    /**
     * The {@link TimerServiceImpl} to which this timer handle belongs to
     */
    private transient TimerServiceImpl service;

    /**
     * Creates a {@link TimerHandleImpl}
     *
     * @param id            The id of the timer instance
     * @param timedObjectId The id of the target {@link TimedObjectInvoker}
     * @param service       The timer service to which this timer handle belongs to
     * @throws IllegalArgumentException If either of the passed parameters is null
     */
    public TimerHandleImpl(final String id, final String timedObjectId, final TimerServiceImpl service) throws IllegalArgumentException {
        if (id == null) {
            throw MESSAGES.idIsNull();
        }
        if (timedObjectId == null) {
            throw MESSAGES.timedObjectNull();
        }
        if (service == null) {
            throw MESSAGES.timerServiceIsNull();
        }

        this.timedObjectId = timedObjectId;
        this.id = id;
        this.service = service;
        this.serviceName = service.getServiceName().getCanonicalName();
    }

    /**
     * Returns the {@link javax.ejb.Timer} corresponding to this timer handle
     * <p/>
     * {@inheritDoc}
     */
    public Timer getTimer() throws IllegalStateException, EJBException {
        if (service == null) {
            // get hold of the timer service through the use of timed object id
            service = (TimerServiceImpl) currentServiceContainer().getRequiredService(ServiceName.parse(serviceName)).getValue();
            if (service == null) {
                throw MESSAGES.timerServiceWithIdNotRegistered(timedObjectId);
            }
        }
        final TimerImpl timer = this.service.getTimer(this);
        if (timer != null && timer.isActive() == false) {
            throw MESSAGES.timerHandleIsNotActive(this);
        }
        return timer;
    }

    public String getId() {
        return this.id;
    }

    public String getTimedObjectId() {
        return this.timedObjectId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof TimerHandleImpl == false) {
            return false;
        }
        TimerHandleImpl other = (TimerHandleImpl) obj;
        if (this == other) {
            return true;
        }
        if (this.id.equals(other.id) && this.timedObjectId.equals(other.timedObjectId)) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }


    private static ServiceContainer currentServiceContainer() {
        return AccessController.doPrivileged(new PrivilegedAction<ServiceContainer>() {
            @Override
            public ServiceContainer run() {
                return CurrentServiceContainer.getServiceContainer();
            }
        });
    }

}
