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

package org.jboss.as.controller.transform;

import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * {@code ExpressionResolver}.
 *
 * @author Emanuel Muckenhuber
 */
class TransformerExpressionResolver implements ExpressionResolver {

    static ExpressionResolver create(final OperationContext context, final TransformationTarget.TransformationTargetType type) {
        switch (type) { // TODO differentiate between the different types
            case DOMAIN:
            case HOST:
            case SERVER:
        }
        final Map<String, String> environment = Collections.emptyMap();
        final Map<String, String> properties = new HashMap<String, String>();
        final PathElement systemProperty = PathElement.pathElement(SYSTEM_PROPERTY);
        final PathAddress address = PathAddress.EMPTY_ADDRESS.append(systemProperty);
        if(context.getRootResourceRegistration().getChildNames(address).contains(SYSTEM_PROPERTY)) {
            final Resource resource = context.readResourceFromRoot(PathAddress.pathAddress(systemProperty));
            for(Resource.ResourceEntry entry : resource.getChildren(SYSTEM_PROPERTY)) {
                final ModelNode model = entry.getModel();
                properties.put(entry.getPathElement().getValue(), model.get(VALUE).asString());
            }
        }
        return new TransformerExpressionResolver(properties, environment);
    }

    private final Map<String, String> properties;
    private final Map<String, String> environment;

    TransformerExpressionResolver(Map<String, String> properties, Map<String, String> environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public ModelNode resolveExpressions(ModelNode node) throws OperationFailedException {
        return resolveExpressionsRecursively(node);
    }

    private ModelNode resolveExpressionsRecursively(final ModelNode node) {
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

    protected void resolvePluggableExpression(ModelNode node) {
        // Replace the properties here
        node.set(replaceProperties(node.asString(), properties, environment));
    }

    private static final int INITIAL = 0;
    private static final int GOT_DOLLAR = 1;
    private static final int GOT_OPEN_BRACE = 2;
    private static final int RESOLVED = 3;
    private static final int DEFAULT = 4;

    private static String replaceProperties(final String value, final Map<String, String> systemProperties, final Map<String, String> env) {
        final StringBuilder builder = new StringBuilder();
        final int len = value.length();
        int state = INITIAL;
        int start = -1;
        int nameStart = -1;
        String resolvedValue = null;
        for (int i = 0; i < len; i = value.offsetByCodePoints(i, 1)) {
            final int ch = value.codePointAt(i);
            switch (state) {
                case INITIAL: {
                    switch (ch) {
                        case '$': {
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
                        case '$': {
                            builder.appendCodePoint(ch);
                            state = INITIAL;
                            continue;
                        }
                        case '{': {
                            start = i + 1;
                            nameStart = start;
                            state = GOT_OPEN_BRACE;
                            continue;
                        }
                        default: {
                            // invalid; emit and resume
                            builder.append('$').appendCodePoint(ch);
                            state = INITIAL;
                            continue;
                        }
                    }
                    // not reachable
                }
                case GOT_OPEN_BRACE: {
                    switch (ch) {
                        case ':':
                        case '}':
                        case ',': {
                            final String name = value.substring(nameStart, i).trim();
                            if ("/".equals(name)) {
                                builder.append(File.separator);
                                state = ch == '}' ? INITIAL : RESOLVED;
                                continue;
                            } else if (":".equals(name)) {
                                builder.append(File.pathSeparator);
                                state = ch == '}' ? INITIAL : RESOLVED;
                                continue;
                            }
                            // First check for system property, then env variable
                            String val = systemProperties.get(name); // System.getProperty(name);
                            if (val == null && name.startsWith("env."))
                                val = env.get(name.substring(4)); // System.getenv(name.substring(4));

                            if (val != null) {
                                builder.append(val);
                                resolvedValue = val;
                                state = ch == '}' ? INITIAL : RESOLVED;
                                continue;
                            } else if (ch == ',') {
                                nameStart = i + 1;
                                continue;
                            } else if (ch == ':') {
                                start = i + 1;
                                state = DEFAULT;
                                continue;
                            } else {
                                throw new IllegalStateException("Failed to resolve expression: "+ value.substring(start - 2, i + 1));
                            }
                        }
                        default: {
                            continue;
                        }
                    }
                    // not reachable
                }
                case RESOLVED: {
                    if (ch == '}') {
                        state = INITIAL;
                    }
                    continue;
                }
                case DEFAULT: {
                    if (ch == '}') {
                        state = INITIAL;
                        builder.append(value.substring(start, i));
                    }
                    continue;
                }
                default:
                    throw new IllegalStateException("Unexpected char seen: "+ch);
            }
        }
        switch (state) {
            case GOT_DOLLAR: {
                builder.append('$');
                break;
            }
            case DEFAULT: {
                builder.append(value.substring(start - 2));
                break;
            }
            case GOT_OPEN_BRACE: {
                // We had a reference that was not resolved, throw ISE
                if (resolvedValue == null)
                    throw new IllegalStateException("Incomplete expression: "+builder.toString());
                break;
           }
        }
        return builder.toString();
    }


}
