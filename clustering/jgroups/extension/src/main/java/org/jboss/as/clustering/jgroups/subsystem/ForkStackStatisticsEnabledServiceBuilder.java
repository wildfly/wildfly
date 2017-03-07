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

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.service.Builder;

/**
 * Builds a service providing the configuration of the statistics-enabled for a given FORK jgroups stack.
 *
 * @author Radoslav Husar
 */
public class ForkStackStatisticsEnabledServiceBuilder extends StackStatisticsEnabledServiceBuilder {

    ForkStackStatisticsEnabledServiceBuilder(String stackName) {
        super(stackName);
    }

    @Override
    public Builder<Boolean> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.statisticsEnabled = STATISTICS_ENABLED.resolveModelAttribute(context, model).asBoolean();
        return this;
    }
}
