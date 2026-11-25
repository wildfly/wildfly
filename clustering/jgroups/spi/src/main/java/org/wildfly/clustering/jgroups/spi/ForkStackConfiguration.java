/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.jgroups.spi;

import org.jboss.modules.Module;
import org.jgroups.JChannel;

/**
 * @author Paul Ferraro
 */
public interface ForkStackConfiguration extends StackConfiguration {
    JChannel getChannel();
    ChannelFactory getChannelFactory();
    Module getModule();
}
