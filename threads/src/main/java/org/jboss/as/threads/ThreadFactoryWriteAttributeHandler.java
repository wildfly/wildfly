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
package org.jboss.as.threads;


import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;


/**
 *
 * @author Alexey Loubyansky
 */
public class ThreadFactoryWriteAttributeHandler extends ThreadsWriteAttributeOperationHandler {

    public static final ThreadFactoryWriteAttributeHandler INSTANCE = new ThreadFactoryWriteAttributeHandler();

    private ThreadFactoryWriteAttributeHandler() {
        super(ThreadFactoryAdd.ATTRIBUTES, ThreadFactoryAdd.RW_ATTRIBUTES);
    }

    protected void applyOperation(ModelNode operation, String attributeName, ServiceController<?> service) {

        final ThreadFactoryService tf = (ThreadFactoryService) service.getService();
        try {
            if (CommonAttributes.GROUP_NAME.equals(attributeName)) {
                final ModelNode value = PoolAttributeDefinitions.GROUP_NAME.validateResolvedOperation(operation);
                tf.setThreadGroupName(value.isDefined() ? value.asString() : null);
            } else if(CommonAttributes.PRIORITY.equals(attributeName)) {
                final ModelNode value = PoolAttributeDefinitions.PRIORITY.validateResolvedOperation(operation);
                tf.setPriority(value.isDefined() ? value.asInt() : -1);
            } else if(CommonAttributes.THREAD_NAME_PATTERN.equals(attributeName)) {
                final ModelNode value = PoolAttributeDefinitions.THREAD_NAME_PATTERN.validateResolvedOperation(operation);
                tf.setNamePattern(value.isDefined() ? value.asString() : null);
            } else {
                throw new IllegalArgumentException("Unexpected attribute '" + attributeName + "'");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
