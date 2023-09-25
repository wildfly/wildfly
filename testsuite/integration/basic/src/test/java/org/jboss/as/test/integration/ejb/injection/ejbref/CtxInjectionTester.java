/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.injection.ejbref;


/**
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 */
//@Remote
public interface CtxInjectionTester {
    void checkInjection() throws FailedException;

    @SuppressWarnings("serial")
    class FailedException extends Exception {

    }
}
