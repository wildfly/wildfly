/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jdr.commands;

/**
 * Abstract class that should be subclassed by JDR Commands.
 *
 * The purpose of this class is to standardize the method by which the
 * JdrEnvironment is shared with Commands.
 */
public abstract class JdrCommand {
    JdrEnvironment env;

    public void setEnvironment(JdrEnvironment env) {
        this.env = env;
    }

    /**
     * executes the command
     * {@link org.jboss.as.jdr.plugins.JdrPlugin} implementations do not need to call this method.
     * @throws Exception
     */
    public abstract void execute() throws Exception;
}
