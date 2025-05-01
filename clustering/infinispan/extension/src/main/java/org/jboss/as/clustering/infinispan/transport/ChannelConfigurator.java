/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.transport;

import java.util.List;

import javax.sql.DataSource;

import org.infinispan.remoting.transport.jgroups.JGroupsChannelConfigurator;
import org.jgroups.JChannel;
import org.jgroups.conf.ProtocolConfiguration;
import org.jgroups.util.SocketFactory;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;

/**
 * @author Paul Ferraro
 */
public class ChannelConfigurator implements JGroupsChannelConfigurator {

    private final ChannelFactory factory;
    private final String name;

    public ChannelConfigurator(ChannelFactory factory, String name) {
        this.factory = factory;
        this.name = name;
    }

    @Override
    public String getProtocolStackString() {
        return null;
    }

    @Override
    public List<ProtocolConfiguration> getProtocolStack() {
        return null;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public JChannel createChannel(String name) throws Exception {
        return this.factory.createChannel(this.name);
    }

    @Override
    public void setSocketFactory(SocketFactory socketFactory) {
        // Do nothing
    }

    @Override
    public void setDataSource(DataSource dataSource) {
    }
}
