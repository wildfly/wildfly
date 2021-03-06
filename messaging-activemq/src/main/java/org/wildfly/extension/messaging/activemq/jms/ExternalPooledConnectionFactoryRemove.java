/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.messaging.activemq.jms;

import static org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.Common.ENTRIES;

import org.jboss.as.controller.OperationContext;
import org.wildfly.extension.messaging.activemq.MessagingServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

/**
 * Operation Handler to remove a Jakarta Messaging external pooled Connection Factory.
 * @author Emmanuel Hugonnet (c) 2018 Red Hat, inc.
 */
public class ExternalPooledConnectionFactoryRemove extends PooledConnectionFactoryRemove {

    public static final ExternalPooledConnectionFactoryRemove INSTANCE = new ExternalPooledConnectionFactoryRemove();

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) {
        ServiceName serviceName = MessagingServices.getActiveMQServiceName("");
        context.removeService(JMSServices.getPooledConnectionFactoryBaseServiceName(serviceName).append(context.getCurrentAddressValue()));
        removeJNDIAliases(context, model.require(ENTRIES.getName()).asList());
    }

    @Override
    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) {
        // TODO:  RE-ADD SERVICES
    }
}
