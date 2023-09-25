/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.tx;

/**
 * Class that stores details about application exceptions
 *
 * @author Stuart Douglas
 */
public class ApplicationExceptionDetails {

    private final String exceptionClass;
    private final boolean inherited;
    private final boolean rollback;


    public ApplicationExceptionDetails(final String exceptionClass, final boolean inherited, final boolean rollback) {
        this.exceptionClass = exceptionClass;
        this.inherited = inherited;
        this.rollback = rollback;
    }

    public String getExceptionClass() {
        return exceptionClass;
    }

    public boolean isInherited() {
        return inherited;
    }

    public boolean isRollback() {
        return rollback;
    }
}
