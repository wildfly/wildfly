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

import java.util.regex.Pattern;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Resolves {@link ModelType#EXPRESSION} expressions in a {@link ModelNode}.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public interface ExpressionResolver {

    /**
     * Resolves any expressions in the passed in ModelNode.
     * Expressions may either represent system properties or vaulted date. For vaulted data the format is
     * ${VAULT::vault_block::attribute_name::sharedKey}
     *
     * @param node the ModelNode containing expressions.
     * @return a copy of the node with expressions resolved
     *
     * @throws OperationFailedException if there is a value of type {@link ModelType#EXPRESSION} in the node tree and
     *            there is no system property or environment variable that matches the expression, or if a security
     *            manager exists and its {@link SecurityManager#checkPermission checkPermission} method doesn't allow
     *            access to the relevant system property or environment variable
     */
    ModelNode resolveExpressions(ModelNode node) throws OperationFailedException;

    /**
     * An {@code ExpressionResolver} suitable for test cases that simply calls {@link ModelNode#resolve()}.
     * Should not be used for production code as it does not support resolution from a security vault.
     */
    ExpressionResolver TEST_RESOLVER = new ExpressionResolverImpl();

    /**
     * Default {@code ExpressionResolver} that simply calls {@link ModelNode#resolve()}.
     * Should not be used for production code as it does not support resolution from a security vault.
     *
     * @deprecated use {@link #TEST_RESOLVER} for test cases
     */
    @Deprecated
    ExpressionResolver DEFAULT = TEST_RESOLVER;

    /**
     * An expression resolver that throws an {@code OperationFailedException} if any expressions are found.
     * Intended for use with APIs where an {@code ExpressionResolver} is required but the caller requires
     * that all expression have already been resolved.
     */
    ExpressionResolver REJECTING = new ExpressionResolverImpl() {
        private final Pattern EXPRESSION_PATTERN = Pattern.compile(".*\\$\\{.*\\}.*");
        @Override
        protected void resolvePluggableExpression(ModelNode node) throws OperationFailedException {
            String expression = node.asString();
            if (EXPRESSION_PATTERN.matcher(expression).matches()) {
                throw ControllerMessages.MESSAGES.illegalUnresolvedModel(expression);
            }
            // It wasn't an expression any way; convert the node to type STRING
            node.set(expression);
        }
    };
}
