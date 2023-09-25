/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
        // Enterprise Beans 3.1 spec, section 14.1.1
        // application exception *must* be of type Exception
        if (!Exception.class.isAssignableFrom(exceptionClass)) {
            throw EjbLogger.ROOT_LOGGER.cannotBeApplicationExceptionBecauseNotAnExceptionType(exceptionClass);
        }
        // Enterprise Beans 3.1 spec, section 14.1.1:
        // application exception *cannot* be of type java.rmi.RemoteException
        if (RemoteException.class.isAssignableFrom(exceptionClass)) {
            throw EjbLogger.ROOT_LOGGER.rmiRemoteExceptionCannotBeApplicationException(exceptionClass);
        }
        // add it to our map
        this.applicationExceptions.put(exceptionClass, applicationException);

    }


}
