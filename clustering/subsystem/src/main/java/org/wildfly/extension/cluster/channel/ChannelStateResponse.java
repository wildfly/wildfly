/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.cluster.channel;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;

public class ChannelStateResponse implements ChannelState, Serializable {
    private static final long serialVersionUID = 3289260366299053750L;

    private final String clusterName;
    private final String view;
    private final String viewHistory;
    private final Map<RpcType, Integer> rpcs = new EnumMap<RpcType, Integer>(RpcType.class);

    ChannelStateResponse(String clusterName, String view, String viewHistory) {
        this.clusterName = clusterName;
        this.view = view;
        this.viewHistory = viewHistory;
    }

    @Override
    public String getClusterName() {
        return this.clusterName;
    }

    @Override
    public String getView() {
        return this.view;
    }

    @Override
    public String getViewHistory() {
        return this.viewHistory;
    }

    @Override
    public Map<RpcType, Integer> getRpcStatistics() {
        return this.rpcs;
    }
}