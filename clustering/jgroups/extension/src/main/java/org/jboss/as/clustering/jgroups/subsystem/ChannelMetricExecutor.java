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
package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.clustering.controller.Metric;
import org.jboss.as.clustering.controller.MetricExecutor;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.jgroups.JChannel;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;
import org.wildfly.clustering.service.PassiveServiceSupplier;

/**
 * Handler for reading run-time only attributes from an underlying channel service.
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 * @author Paul Ferraro
 */
public class ChannelMetricExecutor implements MetricExecutor<JChannel> {

    @Override
    public ModelNode execute(OperationContext context, Metric<JChannel> metric) throws OperationFailedException {
        String channelName = context.getCurrentAddressValue();
        ServiceName serviceName = JGroupsRequirement.CHANNEL.getServiceName(context, channelName);
        JChannel channel = new PassiveServiceSupplier<JChannel>(context.getServiceRegistry(true), serviceName).get();

        return (channel != null) ? metric.execute(channel) : null;
    }
}
