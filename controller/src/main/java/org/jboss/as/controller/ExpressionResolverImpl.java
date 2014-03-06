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
        return resolveExpressionsRecursively(node);
    }

    /**
     * Examine the given model node, resolving any expressions found within, including within child nodes.
     *
     * @param node the node
     * @return a node with all expressions resolved
     * @throws OperationFailedException if an expression cannot be resolved
     */
    private ModelNode resolveExpressionsRecursively(final ModelNode node) throws OperationFailedException {
        if (!node.isDefined()) {
            return node;
        }

        ModelType type = node.getType();
        ModelNode resolved;
        if (type == ModelType.EXPRESSION) {
            resolved = resolveExpressionType(node, false);
        } else if (type == ModelType.OBJECT) {
            resolved = node.clone();
            for (Property prop : resolved.asPropertyList()) {
                resolved.get(prop.getName()).set(resolveExpressionsRecursively(prop.getValue()));
            }
        } else if (type == ModelType.LIST) {
            resolved = node.clone();
            ModelNode list = new ModelNode();
            for (ModelNode current : resolved.asList()) {
                list.add(resolveExpressionsRecursively(current));
            }
            resolved = list;
        } else if (type == ModelType.PROPERTY) {
            resolved = node.clone();
            resolved.set(resolved.asProperty().getName(), resolveExpressionsRecursively(resolved.asProperty().getValue()));
        } else {
            resolved = node;
        }

        return resolved;
    }

    /**
     * Attempt to resolve the expression {@link org.jboss.dmr.ModelNode#asString() encapsulated in the given node},
     * setting the value of {@code node} to the resolved string if successful, or leaving {@code node} unaltered
     * if the expression is not of a form resolvable by this method. When this method returns, the type of {@code node}
     * should either be {@link ModelType#STRING} if this method was able to resolve, or {@link ModelType#EXPRESSION} if
     * not.
     * <p>
     * The default implementation does nothing.
     * </p>
     *
     * @param node a node of type {@link ModelType#EXPRESSION}
     *
     * @throws OperationFailedException if the expression in {@code node} is of a form that should be resolvable by this
     *                                  method but some resolution failure occurs
     */
    protected void resolvePluggableExpression(ModelNode node) throws OperationFailedException {
    }

    /**
     * Attempt to resolve the expression {@link org.jboss.dmr.ModelNode#asString() encapsulated in the given node}.
     *
     * @param expressionType a node of {@link ModelType#EXPRESSION}
     * @param ignoreDMRResolutionFailure {@code false} if {@link org.jboss.dmr.ModelNode#resolve() basic DMR resolution}
     *                            failures should be ignored, and {@code new ModelNode(expressionType.asString())} returned
     *
     * @return a node of {@link ModelType#STRING} where the encapsulated string is the resolved expression
     *
     * @throws OperationFailedException if the expression cannot be resolved
     */
    private ModelNode resolveExpressionType(final ModelNode expressionType, final boolean ignoreDMRResolutionFailure) throws OperationFailedException {

        ModelNode resolved = expressionType.clone();

        // Try plug-in resolution; i.e. vault
        resolvePluggableExpression(resolved);

        if (resolved.getType() == ModelType.EXPRESSION ) {
            // resolvePluggableExpression did nothing. Try standard resolution
            String unresolvedString = expressionType.asString();
            resolved = resolveStandardExpression(resolved, ignoreDMRResolutionFailure);
            String resolvedString = resolved.asString();
            if (!unresolvedString.equals(resolvedString)) {
                // resolveStandardExpression made progress; keep resolving
                resolved = convertAndResolve(resolvedString);
            } // else there is nothing more we can do with this string
        } else {
            // resolvePluggableExpression made progress; keep resolving
            resolved = convertAndResolve(resolved.asString());
        }

        return resolved;
    }

    private ModelNode convertAndResolve(String possibleExpression) throws OperationFailedException {
        if (EXPRESSION_PATTERN.matcher(possibleExpression).matches()) {
            // Keep resolving, but don't fail on unresolvable strings
            ModelNode expression = new ModelNode();
            expression.setExpression(possibleExpression);
            return resolveExpressionType(expression, true);
        }
        return new ModelNode(possibleExpression);
    }

    /**
     * Perform a standard {@link org.jboss.dmr.ModelNode#resolve()} on the given {@code unresolved} node.
     * @param unresolved  the unresolved node, which should be of type {@link ModelType#EXPRESSION}
     * @param ignoreUnresolvable {@code true} if resolution failures due to unknown properties should be ignored,
     *                                       and {@code new ModelNode(unresolved.asString())} returned
     *
     * @return a node of type {@link ModelType#STRING}
     *
     * @throws OperationFailedException if {@code ignoreFailures} is {@code false} and the expression cannot be resolved
     */
    private static ModelNode resolveStandardExpression(final ModelNode unresolved, final boolean ignoreUnresolvable) throws OperationFailedException {
        try {
            return unresolved.resolve();
        } catch (SecurityException e) {
            // A security exception should propagate no matter what the value of ignoreUnresolvable is. The first call to
            // this method for any expression will have ignoreUnresolvable set to 'false' which means a basic test of
            // ability to read system properties will have already passed. So a failure with ignoreUnresolvable set to
            // true means a specific property caused the failure, and that should not be ignored
            throw new OperationFailedException(ControllerMessages.MESSAGES.noPermissionToResolveExpression(unresolved, e));
        } catch (IllegalStateException e) {
            if (ignoreUnresolvable) {
                return new ModelNode(unresolved.asString());
            }
            throw new OperationFailedException(ControllerMessages.MESSAGES.cannotResolveExpression(unresolved, e));
        }

    }

}
