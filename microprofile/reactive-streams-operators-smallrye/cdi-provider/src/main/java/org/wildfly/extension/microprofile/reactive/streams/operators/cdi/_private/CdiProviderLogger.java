/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.reactive.streams.operators.cdi._private;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Log messages for WildFly Reactive Streams Operators (SmallRye) Extension.
 *
 * @author <a href="kkhan@redhat.com">Kabir Khan</a>
 */
@MessageLogger(projectCode = "WFLYRXSTOPSCDI", length = 4)
public interface CdiProviderLogger extends BasicLogger {

    CdiProviderLogger LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), CdiProviderLogger.class, "org.wildfly.extension.microprofile.reactive.streams.operators.cdi");

    @Message(id = 1, value = "No implementation of the %s found in the classpath")
    IllegalStateException noImplementationFound(String className);

}
