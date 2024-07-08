/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.beanvalidation.logging;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.MessageLogger;

/**
 * @author Eduardo Martins
 */
@MessageLogger(projectCode = "WFLYBV", length = 4)
public interface BeanValidationLogger extends BasicLogger {

    BeanValidationLogger ROOT_LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), BeanValidationLogger.class, "org.wildfly.extension.beanvalidation");

}
