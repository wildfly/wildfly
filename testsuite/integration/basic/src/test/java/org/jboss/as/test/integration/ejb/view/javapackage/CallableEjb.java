/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.view.javapackage;

import java.util.concurrent.Callable;

import jakarta.ejb.Stateless;

/**
 * @author Stuart Douglas
 */
@Stateless
public class CallableEjb implements Callable<String> {

    public static final String MESSAGE = "Callable EJB";
    @Override
    public String call() throws Exception {
        return MESSAGE;
    }
}
