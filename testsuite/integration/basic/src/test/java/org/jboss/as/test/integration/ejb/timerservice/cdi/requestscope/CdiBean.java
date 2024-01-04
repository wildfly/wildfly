/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.cdi.requestscope;

import jakarta.enterprise.context.RequestScoped;

/**
 * @author Stuart Douglas
 */
@RequestScoped
public class CdiBean {

    public static final String MESSAGE = "Hello";

    public String getMessage() {
        return MESSAGE;
    }

}
