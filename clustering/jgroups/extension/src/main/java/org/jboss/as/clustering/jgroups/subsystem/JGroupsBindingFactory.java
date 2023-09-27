/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.clustering.naming.JndiNameFactory;
import org.jboss.as.naming.deployment.ContextNames;

public final class JGroupsBindingFactory {

    public static ContextNames.BindInfo createChannelBinding(String channel) {
        return ContextNames.bindInfoFor(JndiNameFactory.createJndiName(JndiNameFactory.DEFAULT_JNDI_NAMESPACE, JGroupsExtension.SUBSYSTEM_NAME, "channel", channel).getAbsoluteName());
    }

    public static ContextNames.BindInfo createChannelFactoryBinding(String stack) {
        return ContextNames.bindInfoFor(JndiNameFactory.createJndiName(JndiNameFactory.DEFAULT_JNDI_NAMESPACE, JGroupsExtension.SUBSYSTEM_NAME, "factory", stack).getAbsoluteName());
    }

    private JGroupsBindingFactory() {
        // Hide
    }
}
