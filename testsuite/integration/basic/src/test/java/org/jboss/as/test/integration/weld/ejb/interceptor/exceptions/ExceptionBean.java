/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.interceptor.exceptions;

import jakarta.ejb.Stateless;

/**
 * @author Stuart Douglas
 */
@Stateless
@ExceptionBinding
public class ExceptionBean {

    public void checked() throws SimpleApplicationException {
        throw new SimpleApplicationException();
    }

    public void unchecked() {
        throw new UncheckedException();
    }
}
