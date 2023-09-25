/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.broadcast;

import java.util.Arrays;
import java.util.List;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.jboss.ClassTableContributor;

/**
 * {@link ClassTableContributor} for a {@link CommandDispatcherBroadcastEndpoint}.
 * @author Paul Ferraro
 */
@MetaInfServices(ClassTableContributor.class)
public class CommandDispatcherBroadcastEndpointClassTableContributor implements ClassTableContributor {

    @Override
    public List<Class<?>> getKnownClasses() {
        return Arrays.asList(BroadcastCommand.class);
    }
}
