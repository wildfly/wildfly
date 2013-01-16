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
import org.jboss.as.controller.transform.TransformersSubRegistration;

import java.util.Collections;
import java.util.Map;

/**
 * @author Emanuel Muckenhuber
 */
public class ExplicitTransformationDescriptionBuilder extends TransformationDescriptionBuilder {

    private ResourceTransformer resourceTransformer = ResourceTransformer.DEFAULT;
    private OperationTransformer operationTransformer = OperationTransformer.DEFAULT;
    private PathTransformation pathTransformation = PathTransformation.DEFAULT;

    private final Map<String, OperationTransformer> operationTransformers = Collections.emptyMap(); // TODO

    public ExplicitTransformationDescriptionBuilder(PathElement pathElement) {
        super(pathElement);
    }

    public ExplicitTransformationDescriptionBuilder setResourceTransformer(ResourceTransformer resourceTransformer) {
        this.resourceTransformer = resourceTransformer;
        return this;
    }

    public ExplicitTransformationDescriptionBuilder setOperationTransformer(OperationTransformer operationTransformer) {
        this.operationTransformer = operationTransformer;
        return this;
    }

    public ExplicitTransformationDescriptionBuilder redirectTo(PathElement element) {
        pathTransformation = new PathTransformation.BasicPathTransformation(element);
        return this;
    }

    @Override
    public TransformationDescription build() {
        return new AbstractDescription(pathElement, pathTransformation) {

            @Override
            public void register(TransformersSubRegistration parent) {
                parent.registerSubResource(pathElement, resourceTransformer, operationTransformer);
            }

        };
    }

}
