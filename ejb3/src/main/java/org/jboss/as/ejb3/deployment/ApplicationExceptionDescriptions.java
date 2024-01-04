/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.deployment;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.tx.ApplicationExceptionDetails;

/**
 * @author Stuart Douglas
 */
public class ApplicationExceptionDescriptions {

    private final Map<String, ApplicationExceptionDetails> applicationExceptions = new ConcurrentHashMap<String, ApplicationExceptionDetails>();


    public void addApplicationException(final String exceptionClassName, final boolean rollback, final boolean inherited) {
        if (exceptionClassName == null || exceptionClassName.isEmpty()) {
            throw EjbLogger.ROOT_LOGGER.stringParamCannotBeNullOrEmpty("Exception class name");
        }
        //TODO: Is this a good idea? ApplicationException's equals/hashCode
        //will not work the way that would be expected
        ApplicationExceptionDetails appException = new ApplicationExceptionDetails(exceptionClassName, inherited, rollback);
        // add it to the map
        this.applicationExceptions.put(exceptionClassName, appException);
    }


    public Map<String, ApplicationExceptionDetails> getApplicationExceptions() {
        return Collections.unmodifiableMap(this.applicationExceptions);
    }
}
