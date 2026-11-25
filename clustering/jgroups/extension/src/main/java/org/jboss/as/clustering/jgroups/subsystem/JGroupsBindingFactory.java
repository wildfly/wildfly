/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.function.Function;

import org.jboss.as.clustering.naming.JndiNameFactory;
import org.jboss.as.naming.deployment.ContextNames;

public enum JGroupsBindingFactory implements Function<String, ContextNames.BindInfo> {
    CHANNEL("channel"),
    CHANNEL_FACTORY("factory"),
    ;
    private final String name;

    JGroupsBindingFactory(String name) {
        this.name = name;
    }

    @Override
    public ContextNames.BindInfo apply(String value) {
        return ContextNames.bindInfoFor(JndiNameFactory.createJndiName(JndiNameFactory.DEFAULT_JNDI_NAMESPACE, JGroupsSubsystemResourceDefinitionRegistrar.REGISTRATION.getName(), this.name, value).getAbsoluteName());
    }
}
