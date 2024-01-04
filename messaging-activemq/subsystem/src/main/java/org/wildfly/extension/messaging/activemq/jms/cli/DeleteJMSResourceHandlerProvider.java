/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.jms.cli;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandHandler;
import org.jboss.as.cli.CommandHandlerProvider;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2015 Red Hat inc.
 */
public class DeleteJMSResourceHandlerProvider implements CommandHandlerProvider {
    @Override
    public CommandHandler createCommandHandler(CommandContext ctx) {
        return new DeleteJMSResourceHandler(ctx);
    }

    @Override
    public boolean isTabComplete() {
        return false;
    }

    @Override
    public String[] getNames() {
        return new String[] { "delete-jms-resource" };
    }
}
