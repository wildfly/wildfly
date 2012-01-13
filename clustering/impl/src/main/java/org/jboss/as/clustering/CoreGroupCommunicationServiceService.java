/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering;

import org.jboss.as.clustering.jgroups.subsystem.ChannelService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jgroups.Channel;

/**
 * @author Paul Ferraro
 *
 */
public class CoreGroupCommunicationServiceService implements Service<CoreGroupCommunicationService> {

    public static ServiceName getServiceName(String name) {
        return ServiceName.JBOSS.append("cluster").append(name);
    }

    private final short scope;
    private final InjectedValue<Channel> channel = new InjectedValue<Channel>();

    private volatile CoreGroupCommunicationService service;

    public CoreGroupCommunicationServiceService(short scope) {
        this.scope = scope;
    }

    public ServiceBuilder<CoreGroupCommunicationService> build(ServiceTarget target, String name) {
        return target.addService(getServiceName(name), this)
            .addDependency(ChannelService.getServiceName(name), Channel.class, this.channel)
        ;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.value.Value#getValue()
     */
    @Override
    public CoreGroupCommunicationService getValue() throws IllegalStateException, IllegalArgumentException {
        return this.service;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.service.Service#start(org.jboss.msc.service.StartContext)
     */
    @Override
    public void start(StartContext context) throws StartException {
        this.service = new CoreGroupCommunicationService();
        this.service.setChannel(this.channel.getValue());
        this.service.setScopeId(this.scope);

        try {
            this.service.start();
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.service.Service#stop(org.jboss.msc.service.StopContext)
     */
    @Override
    public void stop(StopContext context) {
        this.service.stop();
    }
}
