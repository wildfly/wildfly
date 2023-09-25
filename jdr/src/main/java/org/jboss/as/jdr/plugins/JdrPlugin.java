/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jdr.plugins;

import org.jboss.as.jdr.commands.JdrCommand;

import java.util.List;

public interface JdrPlugin {

    List<JdrCommand> getCommands() throws Exception;
    PluginId getPluginId();
}
