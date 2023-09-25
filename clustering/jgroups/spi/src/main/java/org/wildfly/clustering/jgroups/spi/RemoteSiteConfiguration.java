/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.jgroups.spi;

/**
 * Configuration of a channel to a remote site, used by the RELAY2 protocol.
 * @author Paul Ferraro
 */
public interface RemoteSiteConfiguration {
    String getName();

    ChannelFactory getChannelFactory();

    String getClusterName();
}
