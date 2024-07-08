/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.system.logging;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "WFLYSYSJMX", length = 4)
public interface ServiceMBeanLogger extends BasicLogger {

    ServiceMBeanLogger ROOT_LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), ServiceMBeanLogger.class, "org.jboss.system");

    @Message(id = 1, value = "Null method name")
    IllegalArgumentException nullMethodName();

    @Message(id = 2, value = "Unknown lifecyle method %s")
    IllegalArgumentException unknownLifecycleMethod(String methodName);

    @Message(id = 3, value = "Error in destroy %s")
    String errorInDestroy(String description);

    @Message(id = 4, value = "Error in stop %s")
    String errorInStop(String description);

    @Message(id = 5, value = "Initialization failed %s")
    String initializationFailed(String description);

    @Message(id = 6, value = "Starting failed %s")
    String startingFailed(String description);

    @Message(id = 7, value = "Stopping failed %s")
    String stoppingFailed(String description);

    @Message(id = 8, value = "Destroying failed %s")
    String destroyingFailed(String description);

    @Message(id = 9, value = "Initialization failed during postRegister")
    String postRegisterInitializationFailed();
}
