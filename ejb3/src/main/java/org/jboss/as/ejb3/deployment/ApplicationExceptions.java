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

package org.jboss.as.ejb3.deployment;

import org.jboss.as.ejb3.logging.EjbLogger;

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * User: jpai
 */
public class ApplicationExceptions {

    private final Map<Class<?>, org.jboss.as.ejb3.tx.ApplicationExceptionDetails> applicationExceptions = new HashMap<Class<?>, org.jboss.as.ejb3.tx.ApplicationExceptionDetails>();


    public ApplicationExceptions() {


    }

    public org.jboss.as.ejb3.tx.ApplicationExceptionDetails getApplicationException(Class<?> exceptionClass) {
        return this.applicationExceptions.get(exceptionClass);
    }

    public Map<Class<?>, org.jboss.as.ejb3.tx.ApplicationExceptionDetails> getApplicationExceptions() {
        return Collections.unmodifiableMap(this.applicationExceptions);
    }

    public void addApplicationException(Class<?> exceptionClass, org.jboss.as.ejb3.tx.ApplicationExceptionDetails applicationException) {
        if (exceptionClass == null) {
            throw EjbLogger.ROOT_LOGGER.paramCannotBeNull("Exception class");
        }
        if (applicationException == null) {
            throw EjbLogger.ROOT_LOGGER.paramCannotBeNull("ApplicationException");
        }
        // EJB 3.1 spec, section 14.1.1
        // application exception *must* be of type Exception
        if (!Exception.class.isAssignableFrom(exceptionClass)) {
            throw EjbLogger.ROOT_LOGGER.cannotBeApplicationExceptionBecauseNotAnExceptionType(exceptionClass);
        }
        // EJB 3.1 spec, section 14.1.1:
        // application exception *cannot* be of type java.rmi.RemoteException
        if (RemoteException.class.isAssignableFrom(exceptionClass)) {
            throw EjbLogger.ROOT_LOGGER.rmiRemoteExceptionCannotBeApplicationException(exceptionClass);
        }
        // add it to our map
        this.applicationExceptions.put(exceptionClass, applicationException);

    }


}
