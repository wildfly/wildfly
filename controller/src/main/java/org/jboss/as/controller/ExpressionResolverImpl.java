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
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ExpressionResolverImpl implements ExpressionResolver {

    protected ExpressionResolverImpl() {
    }

    @Override
    public final ModelNode resolveExpressions(final ModelNode node) {
        ModelNode resolved = resolveExpressionsRecursively(node);
        return resolved.resolve();
    }

    private ModelNode resolveExpressionsRecursively(final ModelNode node) {
        if (!node.isDefined()) {
            return node;
        }

        ModelNode resolved = node.clone();
        if (resolved.getType() == ModelType.EXPRESSION) {
            resolvePluggableExpression(resolved);
        } else if (resolved.getType() == ModelType.OBJECT) {
            for (Property prop : resolved.asPropertyList()) {
                resolved.get(prop.getName()).set(resolveExpressionsRecursively(prop.getValue()));
            }
        } else if (resolved.getType() == ModelType.LIST) {
            ModelNode list = new ModelNode();
            for (ModelNode current : resolved.asList()) {
                list.add(resolveExpressionsRecursively(current));
            }
            resolved = list;
        } else if (resolved.getType() == ModelType.PROPERTY) {
            resolved.set(resolved.asProperty().getName(), resolveExpressionsRecursively(resolved.asProperty().getValue()));

        }
        return resolved;
    }

    protected void resolvePluggableExpression(ModelNode node) {
    }

}
