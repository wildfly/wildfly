/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.ejb3.remote;

import org.jboss.ejb.client.EJBClientConfiguration;
import org.xnio.OptionMap;

import javax.security.auth.callback.CallbackHandler;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Jaikiran Pai
 */
public class JBossEJBClientXmlConfiguration implements EJBClientConfiguration {

    private final Map<String, ClusterConfiguration> clusterConfigs = new HashMap<String, ClusterConfiguration>();

    @Override
    public String getEndpointName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public OptionMap getEndpointCreationOptions() {
        return OptionMap.EMPTY;
    }

    @Override
    public OptionMap getRemoteConnectionProviderCreationOptions() {
        return OptionMap.EMPTY;
    }

    @Override
    public CallbackHandler getCallbackHandler() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Iterator<RemotingConnectionConfiguration> getConnectionConfigurations() {
        return Collections.EMPTY_SET.iterator();
    }

    @Override
    public Iterator<ClusterConfiguration> getClusterConfigurations() {
        return this.clusterConfigs.values().iterator();
    }

    @Override
    public ClusterConfiguration getClusterConfiguration(String nodeName) {
        return this.clusterConfigs.get(nodeName);
    }

    public void addClusterConfiguration(final EJBClientClusterConfig clusterConfig) {
        this.clusterConfigs.put(clusterConfig.getClusterName(), clusterConfig);
    }

}
