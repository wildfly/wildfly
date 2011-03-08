/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.cli.operation.impl;

import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.OperationRequestParser.CallbackHandler;

/**
 *
 * @author Alexey Loubyansky
 */
public abstract class ValidatingOperationCallbackHandler implements CallbackHandler {

    private String operationStr;

    @Override
    public void start(String operationString) {
        this.operationStr = operationString;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.operation.OperationParser.CallbackHandler#nodeType(java.lang.String)
     */
    @Override
    public void nodeType(String nodeType) throws OperationFormatException {

        if (!Util.isValidIdentifier(nodeType)) {
            throw new OperationFormatException(
                    "The node type is not a valid identifier '"
                            + nodeType
                            + "' or the format is wrong for prefix '"
                            + operationStr + "'");
        }
        validatedNodeType(nodeType);
    }

    protected abstract void validatedNodeType(String nodeType) throws OperationFormatException;

    /* (non-Javadoc)
     * @see org.jboss.as.cli.operation.OperationParser.CallbackHandler#nodeName(java.lang.String)
     */
    @Override
    public void nodeName(String nodeName) throws OperationFormatException {

        if (!Util.isValidIdentifier(nodeName)) {
            throw new OperationFormatException(
                    "The node name is not a valid identifier '"
                            + nodeName
                            + "' or the format is wrong for operation '"
                            + operationStr + "'");
        }
        validatedNodeName(nodeName);
    }

    protected abstract void validatedNodeName(String nodeName) throws OperationFormatException;

    /* (non-Javadoc)
     * @see org.jboss.as.cli.operation.OperationParser.CallbackHandler#operationName(java.lang.String)
     */
    @Override
    public void operationName(String operationName)
            throws OperationFormatException {

        if(!Util.isValidIdentifier(operationName)) {
            throw new OperationFormatException("Operation name '" + operationName + "' is not a valid identifier or command '" + operationStr + "' doesn't follow the format...");
        }
        validatedOperationName(operationName);
    }

    protected abstract void validatedOperationName(String operationName) throws OperationFormatException;

    /* (non-Javadoc)
     * @see org.jboss.as.cli.operation.OperationParser.CallbackHandler#propertyName(java.lang.String)
     */
    @Override
    public void propertyName(String propertyName)
            throws OperationFormatException {

        if (!Util.isValidIdentifier(propertyName)) {
            throw new OperationFormatException(
                    "Argument name '"
                            + propertyName
                            + "' is not a valid identifier or the format is wrong for the property list.");
        }

        validatedPropertyName(propertyName);
    }

    protected abstract void validatedPropertyName(String propertyName) throws OperationFormatException;

    /* (non-Javadoc)
     * @see org.jboss.as.cli.operation.OperationParser.CallbackHandler#property(java.lang.String, java.lang.String, int)
     */
    @Override
    public void property(String name, String value, int nameValueSeparatorIndex)
            throws OperationFormatException {

        if (!Util.isValidIdentifier(name)) {
            throw new OperationFormatException(
                    "Argument name '"
                            + name
                            + "' is not a valid identifier or the format is wrong for the argument list.");
        }

        if (value.isEmpty()) {
            throw new OperationFormatException(
                    "The argument value is missing or the format is wrong for argument '"
                            + value + "'");
        }

        validatedProperty(name, value, nameValueSeparatorIndex);
    }

    protected abstract void validatedProperty(String name, String value, int nameValueSeparatorIndex) throws OperationFormatException;

    /* (non-Javadoc)
     * @see org.jboss.as.cli.operation.OperationParser.CallbackHandler#nodeTypeOrName(java.lang.String)
     */
    @Override
    public void nodeTypeOrName(String typeOrName) throws OperationFormatException {

        if (!Util.isValidIdentifier(typeOrName)) {
            throw new OperationFormatException(
                    "The node type or name is not a valid identifier '"
                            + typeOrName
                            + "' or the format is wrong for opreation '"
                            + operationStr + "'");
        }

        validatedNodeTypeOrName(typeOrName);
    }

    protected abstract void validatedNodeTypeOrName(String typeOrName) throws OperationFormatException;

}
