/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server;

import java.util.function.Function;

import org.jboss.as.clustering.naming.JndiNameFactory;
import org.jboss.as.naming.deployment.JndiName;

/**
 * @author Paul Ferraro
 */
public enum GroupJndiNameFactory implements Function<String, JndiName> {
    COMMAND_DISPATCHER_FACTORY("dispatcher"),
    GROUP("group"),
    ;
    private final String component;

    GroupJndiNameFactory(String component) {
        this.component = component;
    }

    @Override
    public JndiName apply(String group) {
        return JndiNameFactory.createJndiName(JndiNameFactory.DEFAULT_JNDI_NAMESPACE, "clustering", this.component, group);
    }
}
