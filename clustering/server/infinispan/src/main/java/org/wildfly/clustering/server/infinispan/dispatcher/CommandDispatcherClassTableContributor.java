/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.server.infinispan.dispatcher;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.marshalling.jboss.ClassTableContributor;

/**
 * ClassTable contributor for the marshaller of a {@link org.wildfly.clustering.dispatcher.CommandDispatcher}.
 * @author Paul Ferraro
 */
@MetaInfServices(ClassTableContributor.class)
public class CommandDispatcherClassTableContributor implements ClassTableContributor {

    @Override
    public List<Class<?>> getKnownClasses() {
        return Arrays.<Class<?>>asList(Command.class, NoSuchService.class, ExecutionException.class);
    }
}
