/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.xts.wsat.service;

import jakarta.jws.WebMethod;

import org.jboss.as.test.xts.base.TestApplicationException;
import org.jboss.as.test.xts.util.ServiceCommand;

/**
 * WS-AT simple web service interface
 */
public interface AT {

    @WebMethod
    void invoke(ServiceCommand... serviceCommands) throws TestApplicationException;

}
