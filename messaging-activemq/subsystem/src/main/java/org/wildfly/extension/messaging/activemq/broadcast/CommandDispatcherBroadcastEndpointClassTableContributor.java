/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.broadcast;

import java.util.List;

import org.jboss.marshalling.ClassTable;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.jboss.IdentityClassTable;

/**
 * {@link ClassTableContributor} for a {@link CommandDispatcherBroadcastEndpoint}.
 * @author Paul Ferraro
 */
@MetaInfServices(ClassTable.class)
public class CommandDispatcherBroadcastEndpointClassTableContributor extends IdentityClassTable {

    public CommandDispatcherBroadcastEndpointClassTableContributor() {
        super(List.of(BroadcastCommand.class));
    }
}
