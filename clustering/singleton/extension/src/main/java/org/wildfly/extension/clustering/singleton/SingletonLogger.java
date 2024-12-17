/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import static org.jboss.logging.Logger.Level.INFO;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.wildfly.clustering.singleton.service.ServiceTargetFactory;

/**
 * @author Paul Ferraro
 */
@MessageLogger(projectCode = "WFLYCLSNG", length = 4)
public interface SingletonLogger extends BasicLogger {
    String ROOT_LOGGER_CATEGORY = "org.wildfly.extension.clustering.singleton";

    SingletonLogger ROOT_LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), SingletonLogger.class, ROOT_LOGGER_CATEGORY);

    @LogMessage(level = INFO)
    @Message(id = 1, value = "Singleton deployment detected. Deployment will reset using %s policy.")
    void singletonDeploymentDetected(ServiceTargetFactory policy);
}
