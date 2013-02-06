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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.PathAddressTransformer;
import org.jboss.as.controller.transform.ResourceTransformer;

/**
 * @author Emanuel Muckenhuber
 */
abstract class AbstractTransformationDescriptionBuilder implements TransformationDescriptionBuilder {

    protected final PathElement pathElement;

    protected PathAddressTransformer pathAddressTransformer;
    protected ResourceTransformer resourceTransformer;
    protected OperationTransformer operationTransformer;

    protected final Map<String, OperationTransformationEntry> operationTransformers = new HashMap<String, OperationTransformationEntry>();
    protected final List<TransformationDescriptionBuilder> children = new ArrayList<TransformationDescriptionBuilder>();

    protected AbstractTransformationDescriptionBuilder(PathElement pathElement, PathAddressTransformer pathAddressTransformer,
                                             ResourceTransformer resourceTransformer, OperationTransformer operationTransformer) {
        this.pathElement = pathElement;
        this.pathAddressTransformer = pathAddressTransformer;
        this.resourceTransformer = resourceTransformer;
        this.operationTransformer = operationTransformer;
    }

    public TransformationDescriptionBuilder setResourceTransformer(ResourceTransformer resourceTransformer) {
        this.resourceTransformer = resourceTransformer;
        return this;
    }

    void addOperationTransformerEntry(String operationName, OperationTransformationEntry transformer) {
        operationTransformers.put(operationName, transformer);
    }

    abstract static class OperationTransformationEntry {

        /**
         * Get the operation transformer.
         *
         * @param resourceRegistry the attribute transformation description for the resource
         * @return the operation transformer
         */
        abstract OperationTransformer getOperationTransformer(AttributeTransformationDescriptionBuilderImpl.AttributeTransformationDescriptionBuilderRegistry resourceRegistry);

    }

}
