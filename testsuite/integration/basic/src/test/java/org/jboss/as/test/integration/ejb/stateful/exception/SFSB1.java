/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.stateful.exception;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Remote;
import jakarta.ejb.Remove;
import jakarta.ejb.Stateful;

/**
 * stateful session bean
 */
@Stateful
@Remote
@LocalBean
public class SFSB1 implements SFSB1Interface {

    public static final String MESSAGE = "Expected Exception";

    @EJB
    private DestroyMarkerBean marker;


    @PostConstruct
    public void postConstruct() {
        marker.set(false);
    }

    @PreDestroy
    public void preDestroy() {
        marker.set(true);
    }

    public void systemException() {
        throw new RuntimeException(MESSAGE);
    }

    public void ejbException() {
        throw new EJBException(MESSAGE);
    }

    public void userException() throws TestException {
        throw new TestException(MESSAGE);
    }

    @Remove
    public void remove() {
        // just removed
    }
}
