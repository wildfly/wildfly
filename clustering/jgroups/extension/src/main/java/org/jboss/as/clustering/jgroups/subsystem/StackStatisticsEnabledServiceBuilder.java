/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat Middleware LLC, and individual contributors
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

import static org.jboss.as.clustering.jgroups.subsystem.ChannelResourceDefinition.Attribute.STATISTICS_ENABLED;

import org.jboss.as.clustering.controller.ResourceServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.ImmediateValue;
import org.wildfly.clustering.service.Builder;

/**
 * Builds a service providing the configuration of the statistics-enabled for a given jgroups stack.
 *
 * @author Radoslav Husar
 */
public class StackStatisticsEnabledServiceBuilder implements ResourceServiceBuilder<Boolean> {

    private static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append(JGroupsExtension.SUBSYSTEM_NAME, ModelDescriptionConstants.STATISTICS_ENABLED);

    private final String stackName;
    boolean statisticsEnabled;

    StackStatisticsEnabledServiceBuilder(String stackName) {
        this.stackName = stackName;
    }

    @Override
    public ServiceName getServiceName() {
        return SERVICE_NAME.append(stackName);
    }

    @Override
    public Builder<Boolean> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.statisticsEnabled = STATISTICS_ENABLED.resolveModelAttribute(context, model).asBoolean();
        return this;
    }

    @Override
    public ServiceBuilder<Boolean> build(ServiceTarget target) {
        return target.addService(this.getServiceName(), new ValueService<>(new ImmediateValue<>(this.statisticsEnabled)));
    }
}
