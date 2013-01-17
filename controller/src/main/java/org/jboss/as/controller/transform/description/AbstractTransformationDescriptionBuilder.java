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

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.PathTransformation;
import org.jboss.as.controller.transform.ResourceTransformer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Emanuel Muckenhuber
 */
abstract class AbstractTransformationDescriptionBuilder implements TransformationDescriptionBuilder {

    protected final PathElement pathElement;

    protected PathTransformation pathTransformation;
    protected ResourceTransformer resourceTransformer;
    protected OperationTransformer operationTransformer;

    protected final Map<String, OperationTransformer> operationTransformers = new HashMap<String, OperationTransformer>();
    protected final List<AbstractTransformationDescriptionBuilder> children = new ArrayList<AbstractTransformationDescriptionBuilder>();

    protected AbstractTransformationDescriptionBuilder(PathElement pathElement, PathTransformation pathTransformation,
                                             ResourceTransformer resourceTransformer, OperationTransformer operationTransformer) {
        this.pathElement = pathElement;
        this.pathTransformation = pathTransformation;
        this.resourceTransformer = resourceTransformer;
        this.operationTransformer = operationTransformer;
    }

    public TransformationDescriptionBuilder setPathTransformation(PathTransformation pathTransformation) {
        this.pathTransformation = pathTransformation;
        return this;
    }

    public TransformationDescriptionBuilder setResourceTransformer(ResourceTransformer resourceTransformer) {
        this.resourceTransformer = resourceTransformer;
        return this;
    }

    public TransformationDescriptionBuilder setOperationTransformer(OperationTransformer operationTransformer) {
        this.operationTransformer = operationTransformer;
        return this;
    }

    public TransformationDescriptionBuilder addOperationTransformer(final String operationName, final OperationTransformer operationTransformer) {
        operationTransformers.put(operationName, operationTransformer);
        return this;
    }

}
