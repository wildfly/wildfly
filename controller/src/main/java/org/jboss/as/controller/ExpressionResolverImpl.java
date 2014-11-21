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

import java.util.Stack;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.dmr.ValueExpression;

/**
 * Basic {@link ExpressionResolver} implementation.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ExpressionResolverImpl implements ExpressionResolver {

    private static final int INITIAL = 0;
    private static final int GOT_DOLLAR = 1;
    private static final int GOT_OPEN_BRACE = 2;

    // This is always false in EAP6 but is kept to make backporting from WildFly easier
    private final boolean lenient = false;

    /**
     * Creates a new {@code ExpressionResolverImpl} configured to throw an OFE
     * when it encounters an unresolvable expression.
     */
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
            resolved = resolveExpressionStringRecursively(node.asExpression().getExpressionString(), lenient, true);
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
     * Attempt to resolve the given expression string, recursing if resolution of one string produces
     * another expression.
     *
     * @param expressionString the expression string from a node of {@link ModelType#EXPRESSION}
     * @param ignoreDMRResolutionFailure {@code false} if {@link org.jboss.dmr.ModelNode#resolve() basic DMR resolution}
     *                            failures should be ignored, and {@code new ModelNode(expressionType.asString())} returned
     * @param initial {@code true} if this call originated outside this method; {@code false} if it is a recursive call
     *
     * @return a node of {@link ModelType#STRING} where the encapsulated string is the resolved expression, or a node
     *         of {@link ModelType#EXPRESSION} if {@code ignoreDMRResolutionFailure} and {@code initial} are
     *         {@code true} and the string could not be resolved.
     *
     * @throws OperationFailedException if the expression cannot be resolved
     */
    private ModelNode resolveExpressionStringRecursively(final String expressionString, final boolean ignoreDMRResolutionFailure,
                                                         final boolean initial) throws OperationFailedException {
        ParseAndResolveResult resolved = parseAndResolve(expressionString, ignoreDMRResolutionFailure);
        if (resolved.recursive) {
            // Some part of expressionString resolved into a different expression.
            // So, start over, ignoring failures. Ignore failures because we don't require
            // that expressions must not resolve to something that *looks like* an expression but isn't
            return resolveExpressionStringRecursively(resolved.result, true, false);
        } else if (resolved.modified) {
            // Typical case
            return new ModelNode(resolved.result);
        } else if (initial && EXPRESSION_PATTERN.matcher(expressionString).matches()) {
            // We should only get an unmodified expression string back if there was a resolution
            // failure that we ignored.
            assert ignoreDMRResolutionFailure;
            // expressionString came from a node of type expression, so since we did nothing send it back in the same type
            return new ModelNode(new ValueExpression(expressionString));
        } else {
            // The string wasn't really an expression. Two possible cases:
            // 1) if initial == true, someone created a expression node with a non-expression string, which is legal
            // 2) if initial == false, we resolved from an ModelType.EXPRESSION to a string that looked like an
            // expression but can't be resolved. We don't require that expressions must not resolve to something that
            // *looks like* an expression but isn't, so we'll just treat this as a string
            return new ModelNode(expressionString);
        }
    }

    private ParseAndResolveResult parseAndResolve(final String initialValue, boolean lenient) throws OperationFailedException {


        final StringBuilder builder = new StringBuilder();
        final int len = initialValue.length();
        int state = INITIAL;
        int ignoreBraceLevel = 0;
        boolean modified = false;
        Stack<OpenExpression> stack = null;
        for (int i = 0; i < len; i = initialValue.offsetByCodePoints(i, 1)) {
            final int ch = initialValue.codePointAt(i);
            switch (state) {
                case INITIAL: {
                    switch (ch) {
                        case '$': {
                            stack = addToStack(stack, i);
                            state = GOT_DOLLAR;
                            continue;
                        }
                        default: {
                            builder.appendCodePoint(ch);
                            continue;
                        }
                    }
                    // not reachable
                }
                case GOT_DOLLAR: {
                    switch (ch) {
                        case '{': {
                            state = GOT_OPEN_BRACE;
                            continue;
                        }
                        default: {
                            // Previous $ was not the start of an expression
                            if (stack.size() == 1) {
                                // not in an outer expression; store to the builder and resume
                                stack.clear(); // looks faster than pop()
                                if (ch != '$') {
                                    // Preceding $ wasn't an escape, so restore it
                                    builder.append('$');
                                } else {
                                    modified = true; // since we discarded the '$'
                                }
                                builder.appendCodePoint(ch);
                                state = INITIAL;
                            } else {
                                // We're in an outer expression, so just discard the top stack element
                                // created when we saw the '$' and resume tracking the outer expression
                                stack.pop();
                                state = GOT_OPEN_BRACE;
                            }
                            continue;
                        }
                    }
                    // not reachable
                }
                case GOT_OPEN_BRACE: {
                    switch (ch) {
                        case '$': {
                            stack.push(new OpenExpression(i));
                            state = GOT_DOLLAR;
                            continue;
                        }
                        case '{': {
                            ignoreBraceLevel++;
                            continue;
                        }
                        case '}': {
                            if (ignoreBraceLevel > 0) {
                                ignoreBraceLevel--;
                                continue;
                            }
                            String toResolve = getStringToResolve(initialValue, stack, i);
                            final String resolved = resolveExpressionString(toResolve);
                            // We only successfully resolved if toResolve != resolved
                            if (!toResolve.equals(resolved)) {
                                if (EXPRESSION_PATTERN.matcher(resolved).matches()) {
                                    // The resolved value is itself an expression, so
                                    // there will need to be another pass.
                                    // We need to discard any changes made from initialValue
                                    // prior to this expression, because if there were any
                                    // escaped $ sequences in there, we can't lose the escape char
                                    return createRecursiveResult(initialValue, resolved, stack, i);
                                }

                                // Non-recursive case

                                // Update the stack
                                recordResolutionInStack(resolved, stack);

                                if (stack.size() == 0) {
                                    // All expressions so far are resolved; record for output
                                    builder.append(resolved);
                                    state = INITIAL;
                                } else {
                                    // We resolved a nested expression; keep going with the outer one
                                    state = GOT_OPEN_BRACE;
                                }
                                modified = true;
                                continue;
                            } else if (stack.size() > 1) {
                                // We don't fail the overall resolution due to not resolving a nested expression,
                                // as the nested part may be irrelevant to the final resolution.
                                // For example '${bar}' is irrelevant to resolving '${foo:${bar}}'
                                // if system property 'foo' is set.

                                // Clean up stack and store toResolve so we don't have to build it again
                                // when we get to the end of the outer expression
                                recordResolutionInStack(toResolve, stack);
                                state = GOT_OPEN_BRACE;
                                continue;
                            } else if (lenient) {
                                // just respond with the initial value
                                return new ParseAndResolveResult(initialValue, false, false);
                            } else {
                                throw ControllerMessages.MESSAGES.cannotResolveExpression(initialValue);
                            }
                        }
                        default: {
                            continue;
                        }
                    }
                    // not reachable
                }
                default:
                    // If we reach this, there's a programming error in this class
                    throw new IllegalStateException();
            }
        }

        if (stack != null && stack.size() > 0) {
            if (state == GOT_DOLLAR) {
                stack.pop();
            }

            if (stack.size() > 0) {
                throw ControllerMessages.MESSAGES.incompleteExpression(initialValue);
            }

            // Stack was a single item due to GOT_DOLLAR. Need to restore the lost $
            builder.append('$');
        }

        return new ParseAndResolveResult(builder.toString(), modified, false);
    }

    private static Stack<OpenExpression> addToStack(Stack<OpenExpression> stack, int startIndex) {
        Stack<OpenExpression> result = stack == null ? new Stack<OpenExpression>() : stack;
        result.push(new OpenExpression(startIndex));
        return result;
    }

    /** Resolve the given string using any plugin and the DMR resolve method */
    private String resolveExpressionString(final String unresolvedString) throws OperationFailedException {

        // parseAndResolve should only be providing expressions with no leading or trailing chars
        assert unresolvedString.startsWith("${") && unresolvedString.endsWith("}");

        // Default result is no change from input
        String result = unresolvedString;

        ModelNode resolveNode = new ModelNode(new ValueExpression(unresolvedString));

        // Try plug-in resolution; i.e. vault
        resolvePluggableExpression(resolveNode);

        if (resolveNode.getType() == ModelType.EXPRESSION ) {
            // resolvePluggableExpression did nothing. Try standard resolution
            String resolvedString = resolveStandardExpression(resolveNode);
            if (!unresolvedString.equals(resolvedString)) {
                // resolveStandardExpression made progress
                result = resolvedString;
            } // else there is nothing more we can do with this string
        } else {
            // resolvePluggableExpression made progress
            result = resolveNode.asString();
        }

        return result;
    }

    /**
     * Perform a standard {@link org.jboss.dmr.ModelNode#resolve()} on the given {@code unresolved} node.
     * @param unresolved  the unresolved node, which should be of type {@link org.jboss.dmr.ModelType#EXPRESSION}
     * @return a node of type {@link ModelType#STRING}
     *
     * @throws OperationFailedException if {@code ignoreFailures} is {@code false} and the expression cannot be resolved
     */
    private static String resolveStandardExpression(final ModelNode unresolved) throws OperationFailedException {
        try {
            return unresolved.resolve().asString();
        } catch (SecurityException e) {
            // A security exception should propagate no matter what the value of ignoreUnresolvable is. The first call to
            // this method for any expression will have ignoreUnresolvable set to 'false' which means a basic test of
            // ability to read system properties will have already passed. So a failure with ignoreUnresolvable set to
            // true means a specific property caused the failure, and that should not be ignored
            throw new OperationFailedException(new ModelNode(ControllerMessages.MESSAGES.noPermissionToResolveExpression(unresolved, e)));
        } catch (IllegalStateException e) {
            return unresolved.asString();
        }

    }

    private static String getStringToResolve(String initialValue, Stack<OpenExpression> stack, int expressionEndIndex) {
        int stackSize = stack.size();

        int expressionElement = -1;
        OpenExpression firstUnresolved = null;
        for (int i = stackSize - 1; i >= 0; i--) {
            OpenExpression oe = stack.get(i);
            if (oe.resolvedValue == null) {
                expressionElement = i;
                firstUnresolved = oe;
                break;
            }
        }

        assert expressionElement > -1;

        // Now we know how long this expression is
        firstUnresolved.endIndex = expressionEndIndex;

        if (expressionElement == stackSize - 1) {
            // Simple case; no already resolved nested stuff to patch in
            return initialValue.substring(firstUnresolved.startIndex, expressionEndIndex + 1);
        } else {
            // Compose the new expression from the original and resolved nested elements
            StringBuilder sb = new StringBuilder();
            int nextStart = firstUnresolved.startIndex;
            for (int i = expressionElement + 1; i < stackSize; i++) {
                OpenExpression oe = stack.get(i);
                sb.append(initialValue.substring(nextStart, oe.startIndex));
                sb.append(oe.resolvedValue);
                nextStart = oe.endIndex + 1;
            }
            // Add the last bits, which will at least be the trailing '}' at expressionEndIndex
            sb.append(initialValue.substring(nextStart, expressionEndIndex + 1));
            return sb.toString();
        }
    }

    private static ParseAndResolveResult createRecursiveResult(String initialValue, String val,
                                                          Stack<OpenExpression> stack, int expressionEndIndex) {
        int initialLength = initialValue.length();

        int expressionIndex = -1;
        while (expressionIndex == -1) {
            OpenExpression oe = stack.pop();
            if (oe.resolvedValue == null) {
                expressionIndex = oe.startIndex;
            }
        }
        String result;
        if (expressionIndex == 0 && expressionEndIndex == initialLength -1) {
            // basic case
            result = val;
        } else if (expressionIndex == 0) {
            result = val + initialValue.substring(expressionEndIndex + 1);
        } else {
            StringBuilder sb = new StringBuilder(initialValue.substring(0, expressionIndex));
            sb.append(val);
            if (expressionEndIndex < initialLength - 1) {
                sb.append(initialValue.substring(expressionEndIndex + 1));
            }
            result = sb.toString();

        }
        return new ParseAndResolveResult(result, true, true);
    }

    private static void recordResolutionInStack(String val, Stack<OpenExpression> stack) {

        for (int i = stack.size() -1; i >= 0; i--) {
            OpenExpression oe = i == 0 ? stack.pop() : stack.peek();
            if (oe.resolvedValue == null) {
                oe.resolvedValue = val;
                break;
            } else {
                assert i > 0;
                // Don't need the nested data any more
                stack.pop();
            }
        }
    }

    private static class ParseAndResolveResult {
        private final String result;
        private final boolean modified;
        private final boolean recursive;

        private ParseAndResolveResult(String result, boolean modified, boolean recursive) {
            this.result = result;
            this.modified = modified;
            this.recursive = recursive;
        }
    }

    private static class OpenExpression {
        private final int startIndex;
        private int endIndex = -1;
        private String resolvedValue;

        private OpenExpression(int startIndex) {
            this.startIndex = startIndex;
        }
    }

}
