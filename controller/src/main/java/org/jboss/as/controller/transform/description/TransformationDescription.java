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

package org.jboss.as.controller.transform.description;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.ModelVersionRange;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.TransformersSubRegistration;
import org.jboss.dmr.ModelNode;

import java.util.Iterator;
import java.util.List;

/**
 * The final tranformation description.
 *
 * @author Emanuel Muckenhuber
 */
public interface TransformationDescription {

    /**
     * Get the path transformation for this level.
     *
     * @return the path transformation
     */
    PathTransformation getPathTransformation();

    /**
     * Register this transformation description to the subsystem registration.
     *
     * @param subsytem the subsystem
     * @param versions the versions
     */
    void register(SubsystemRegistration subsytem, ModelVersion... versions);

    /**
     * Register this transformation description to the subsystem registration.
     *
     * @param subsytem the subsystem
     * @param range the version range
     */
    void register(SubsystemRegistration subsytem, ModelVersionRange range);

    /**
     * Register this transformation description.
     *
     * @param registration the transformation description
     */
    void register(TransformersSubRegistration parent);


    // TODO registerAsSubsystem...
}
