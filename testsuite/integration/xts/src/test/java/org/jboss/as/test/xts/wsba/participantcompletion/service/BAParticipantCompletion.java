/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.xts.wsba.participantcompletion.service;

import org.jboss.as.test.xts.base.TestApplicationException;
import org.jboss.as.test.xts.util.ServiceCommand;

import jakarta.ejb.Remote;
import jakarta.jws.WebMethod;

@Remote
public interface BAParticipantCompletion {

    @WebMethod
    void saveData(ServiceCommand... serviceCommands) throws TestApplicationException;

}
