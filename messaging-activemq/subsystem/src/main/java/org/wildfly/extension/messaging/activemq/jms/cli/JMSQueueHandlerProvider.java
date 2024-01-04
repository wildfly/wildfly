/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.jms.cli;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandHandler;
import org.jboss.as.cli.CommandHandlerProvider;
import org.jboss.as.cli.handlers.GenericTypeOperationHandler;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2015 Red Hat inc.
 */
public class JMSQueueHandlerProvider implements CommandHandlerProvider {
    @Override
    public CommandHandler createCommandHandler(CommandContext ctx) {
        return new GenericTypeOperationHandler(ctx,  "/subsystem=messaging-activemq/server=default/jms-queue", "queue-address");
    }

    @Override
    public boolean isTabComplete() {
        return true;
    }

    @Override
    public String[] getNames() {
        return new String[] { "jms-queue" };
    }
}
