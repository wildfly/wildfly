/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.clustering.controller.transform;

import java.util.Map;

import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.dmr.ModelNode;

/**
 * Operation transformer multiplexer for read-attribute, write-attribute, and undefine-attribute operations.
 * @author Paul Ferraro
 */
public class AttributeOperationTransformer implements OperationTransformer {

    private final Map<String, OperationTransformer> transformers;

    public AttributeOperationTransformer(Map<String, OperationTransformer> transformers) {
        this.transformers = transformers;
    }

    @Override
    public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) throws OperationFailedException {
        String name = Operations.getName(operation);
        OperationTransformer transformer = this.transformers.get(name);
        return (transformer != null) ? transformer.transformOperation(context, address, operation) : new TransformedOperation(operation, OperationResultTransformer.ORIGINAL_RESULT);
    }
}
