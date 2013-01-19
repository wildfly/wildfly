/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.controller;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 * Basic {@link ExpressionResolver} implementation.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ExpressionResolverImpl implements ExpressionResolver {

    protected ExpressionResolverImpl() {
    }

    @Override
    public final ModelNode resolveExpressions(final ModelNode node) throws OperationFailedException {
        ModelNode resolved = resolveExpressionsRecursively(node);
        try {
            return resolved.resolve();
        } catch (SecurityException e) {
            throw new OperationFailedException(new ModelNode().set(ControllerMessages.MESSAGES.noPermissionToResolveExpression(resolved, e)));
        } catch (IllegalStateException e) {
            throw new OperationFailedException(new ModelNode().set(ControllerMessages.MESSAGES.cannotResolveExpression(resolved, e)));
        }
    }

    private ModelNode resolveExpressionsRecursively(final ModelNode node) throws OperationFailedException {
        if (!node.isDefined()) {
            return node;
        }

        ModelNode resolved;
        if (node.getType() == ModelType.EXPRESSION) {
            resolved = node.clone();
            resolvePluggableExpression(resolved);
        } else if (node.getType() == ModelType.OBJECT) {
            resolved = node.clone();
            for (Property prop : resolved.asPropertyList()) {
                resolved.get(prop.getName()).set(resolveExpressionsRecursively(prop.getValue()));
            }
        } else if (node.getType() == ModelType.LIST) {
            resolved = node.clone();
            ModelNode list = new ModelNode();
            for (ModelNode current : resolved.asList()) {
                list.add(resolveExpressionsRecursively(current));
            }
            resolved = list;
        } else if (node.getType() == ModelType.PROPERTY) {
            resolved = node.clone();
            resolved.set(resolved.asProperty().getName(), resolveExpressionsRecursively(resolved.asProperty().getValue()));
        } else {
            resolved = node;
        }

        return resolved;
    }

    protected void resolvePluggableExpression(ModelNode node) throws OperationFailedException {
    }

}
