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

package org.jboss.as.jmx;

import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * A HORRIBLE HORRIBLE HACK. Supports tunneling of JMX over management.
 *
 * @author Jason T. Greene
 */
public class GetMBeanInfoRaw extends AbstractRuntimeOnlyHandler {
    private static final String MBEAN_NAME = "mbean-name";

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
        ServiceController<?> service = context.getServiceRegistry(false).getRequiredService(MBeanServerService.SERVICE_NAME);
        MBeanServer server = (MBeanServer) service.getValue();

        String name = operation.require(MBEAN_NAME).asString();


        ObjectName beanName;
        MBeanInfo result;
        try {
            beanName = ObjectName.getInstance(name);

            result = server.getMBeanInfo(beanName);
        } catch (Exception e) {
            throw new OperationFailedException(e.getMessage(), e, new ModelNode().set(e.getMessage()));
        }

        context.getResult().set(InvokeMBeanRaw.getBytes(result));
        context.completeStep();
    }
}