/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.xts.wsba.coordinatorcompletion.service;

import jakarta.jws.WebMethod;

import org.jboss.as.test.xts.base.TestApplicationException;
import org.jboss.as.test.xts.util.ServiceCommand;

public interface BACoordinatorCompletion {

    @WebMethod
    void saveData(ServiceCommand... serviceCommands) throws TestApplicationException;
}
