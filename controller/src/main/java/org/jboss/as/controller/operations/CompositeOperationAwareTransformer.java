/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.operations;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.jboss.as.controller.OperationContext;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;

/**
 * @author Stuart Douglas
 */
public class CompositeOperationAwareTransformer implements DomainOperationTransformer {

    private final ModelNode newOperation;

    public CompositeOperationAwareTransformer(final ModelNode newOperation) {
        this.newOperation = newOperation;
    }

    @Override
    public ModelNode transform(final OperationContext context, final ModelNode operation) {
        if (operation.get(OP).asString().equals(COMPOSITE)) {
            ModelNode ret = operation.clone();
            final List<ModelNode> list = new ArrayList<ModelNode>(ret.get(STEPS).asList());
            ListIterator<ModelNode> it = list.listIterator();
            while (it.hasNext()) {
                final ModelNode subOperation = it.next();
                transform(context, subOperation);
            }
            ret.get(STEPS).set(list);
            return ret;
        } else {
            if (matches(operation, newOperation)) {
                return newOperation;
            } else {
                return operation;
            }
        }
    }

    protected boolean matches(final ModelNode operation, final ModelNode newOperation) {
        return operation.get(OP).equals(newOperation.get(OP)) &&
                operation.get(ADDRESS).equals(newOperation.get(ADDRESS));
    }
}
