/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.config.smallrye._private;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.INFO;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
@MessageLogger(projectCode = "WFLYCONF", length = 4)
public interface MicroProfileConfigLogger extends BasicLogger {

    /**
     * The root logger with a category of the package name.
     */
    MicroProfileConfigLogger ROOT_LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), MicroProfileConfigLogger.class,"org.wildfly.extension.microprofile.config.smallrye");

    /**
     * Logs an informational message indicating the naming subsystem is being activated.
     */
    @LogMessage(level = INFO)
    @Message(id = 1, value = "Activating MicroProfile Config Subsystem")
    void activatingSubsystem();

    @Message(id = 2, value = "Unable to load class %s from module %s")
    OperationFailedException unableToLoadClassFromModule(String className, String moduleName);

    @LogMessage(level = DEBUG)
    @Message(id = 3, value = "Use directory for MicroProfile Config Source: %s")
    void loadConfigSourceFromDir(String path);

    @LogMessage(level = DEBUG)
    @Message(id = 4, value = "Use class for MicroProfile Config Source: %s")
    void loadConfigSourceFromClass(Class clazz);

    // 5 and 6 will come from https://github.com/wildfly/wildfly/pull/15030

    // 7 and 8 are used downstream
    /*
    @Message(id = 7, value = "")
    OperationFailedException seeDownstream();

    @Message(id = 8, value = "")
    String seeDownstream();
    */

    @LogMessage(level = DEBUG)
    @Message(id = 9, value = "Use directory for MicroProfile Config Source Root: %s")
    void loadConfigSourceRootFromDir(String path);

    @LogMessage(level = INFO)
    @Message(id = 10, value = "The MicroProfile Config Source root directory '%s' contains the following directories which will be used as MicroProfile Config Sources: %s")
    void logDirectoriesUnderConfigSourceRoot(String name, List<String> directories);
}
