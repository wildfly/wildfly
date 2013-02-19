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

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReadResourceNameOperationStepHandler;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for a thread factory resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ThreadFactoryResourceDefinition extends SimpleResourceDefinition {

    public ThreadFactoryResourceDefinition() {
        this(CommonAttributes.THREAD_FACTORY);
    }

    public ThreadFactoryResourceDefinition(String type) {
        super(PathElement.pathElement(type),
                new StandardResourceDescriptionResolver(CommonAttributes.THREAD_FACTORY, ThreadsExtension.RESOURCE_NAME,
                ThreadsExtension.class.getClassLoader(), true, false),
                ThreadFactoryAdd.INSTANCE,
                new ServiceRemoveStepHandler(ThreadsServices.FACTORY, ThreadFactoryAdd.INSTANCE));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadOnlyAttribute(PoolAttributeDefinitions.NAME, ReadResourceNameOperationStepHandler.INSTANCE);
        ThreadFactoryWriteAttributeHandler.INSTANCE.registerAttributes(resourceRegistration);
    }

    public static void registerTransformers1_0(ResourceTransformationDescriptionBuilder parent) {
        registerTransformers1_0(parent, CommonAttributes.THREAD_FACTORY);
    }

    public static void registerTransformers1_0(ResourceTransformationDescriptionBuilder parent, String type) {
        parent.addChildResource(PathElement.pathElement(type))
        .getAttributeBuilder()
            .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, PoolAttributeDefinitions.GROUP_NAME, PoolAttributeDefinitions.THREAD_NAME_PATTERN);
    }
}
