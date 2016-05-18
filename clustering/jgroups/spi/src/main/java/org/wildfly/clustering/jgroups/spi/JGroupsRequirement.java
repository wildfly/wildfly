/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.jgroups.spi;

import org.jboss.as.clustering.controller.DefaultableUnaryServiceNameFactoryProvider;
import org.jboss.as.clustering.controller.ServiceNameFactory;
import org.jboss.as.clustering.controller.UnaryRequirementServiceNameFactory;
import org.jboss.as.clustering.controller.UnaryServiceNameFactory;
import org.wildfly.clustering.service.DefaultableUnaryRequirement;
import org.wildfly.clustering.service.Requirement;

/**
 * @author Paul Ferraro
 */
public enum JGroupsRequirement implements DefaultableUnaryRequirement, DefaultableUnaryServiceNameFactoryProvider {
    CHANNEL("org.wildfly.clustering.jgroups.channel", JGroupsDefaultRequirement.CHANNEL),
    CHANNEL_CLUSTER("org.wildfly.clustering.jgroups.channel-cluster", JGroupsDefaultRequirement.CHANNEL_CLUSTER),
    CHANNEL_FACTORY("org.wildfly.clustering.jgroups.channel-factory", JGroupsDefaultRequirement.CHANNEL_FACTORY),
    CHANNEL_MODULE("org.wildfly.clustering.jgroups.channel-module", JGroupsDefaultRequirement.CHANNEL_MODULE),
    CHANNEL_SOURCE("org.wildfly.clustering.jgroups.channel-source", JGroupsDefaultRequirement.CHANNEL_SOURCE),
    ;
    private final String name;
    private final UnaryServiceNameFactory factory = new UnaryRequirementServiceNameFactory(this);
    private final JGroupsDefaultRequirement defaultRequirement;

    JGroupsRequirement(String name, JGroupsDefaultRequirement defaultRequirement) {
        this.name = name;
        this.defaultRequirement = defaultRequirement;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Requirement getDefaultRequirement() {
        return this.defaultRequirement;
    }

    @Override
    public UnaryServiceNameFactory getServiceNameFactory() {
        return this.factory;
    }

    @Override
    public ServiceNameFactory getDefaultServiceNameFactory() {
        return this.defaultRequirement;
    }
}
