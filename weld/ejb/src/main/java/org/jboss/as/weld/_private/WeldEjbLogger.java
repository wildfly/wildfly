/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.weld._private;

import java.lang.invoke.MethodHandles;

import jakarta.ejb.NoSuchEJBException;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "WFLYWELDEJB", length = 4)
public interface WeldEjbLogger extends BasicLogger {

    WeldEjbLogger ROOT_LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), WeldEjbLogger.class, "org.jboss.as.weld.ejb");

    @Message(id = 1, value = "EJB has been removed: %s")
    NoSuchEJBException ejbHashBeenRemoved(Object ejbComponent);

}
