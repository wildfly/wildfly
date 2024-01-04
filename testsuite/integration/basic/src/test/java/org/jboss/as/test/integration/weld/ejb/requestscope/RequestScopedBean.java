/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.weld.ejb.requestscope;

import jakarta.enterprise.context.RequestScoped;

/**
 * @author Stuart Douglas
 */
@RequestScoped
public class RequestScopedBean {

    public static final String MESSAGE = "Request Scoped";

    public String getMessage() {
        return MESSAGE;
    }
}
