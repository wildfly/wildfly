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

import java.util.regex.Pattern;

import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.OperationRequestParser.CallbackHandler;

/**
 *
 * @author Alexey Loubyansky
 */
public abstract class ValidatingOperationCallbackHandler implements CallbackHandler {

    private static final Pattern ALPHANUMERICS_PATTERN = Pattern.compile("[_a-zA-Z](?:[-_a-zA-Z0-9]*[_a-zA-Z0-9])?");
    private static final Pattern NODE_NAME_PATTERN = Pattern.compile("\\*|[^*\\p{Space}\\p{Cntrl}]+");


    protected String operationStr;

    @Override
    public void start(String operationString) {
        this.operationStr = operationString;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.operation.OperationParser.CallbackHandler#nodeType(java.lang.String)
     */
    @Override
    public void nodeType(String nodeType) throws OperationFormatException {

        assertValidType(nodeType);
        validatedNodeType(nodeType);
    }

    protected abstract void validatedNodeType(String nodeType) throws OperationFormatException;

    /* (non-Javadoc)
     * @see org.jboss.as.cli.operation.OperationParser.CallbackHandler#nodeName(java.lang.String)
     */
    @Override
    public void nodeName(String nodeName) throws OperationFormatException {

        assertValidNodeName(nodeName);
        validatedNodeName(nodeName);
    }

    protected abstract void validatedNodeName(String nodeName) throws OperationFormatException;

    /* (non-Javadoc)
     * @see org.jboss.as.cli.operation.OperationParser.CallbackHandler#operationName(java.lang.String)
     */
    @Override
    public void operationName(String operationName)
            throws OperationFormatException {

        if (operationName == null || !ALPHANUMERICS_PATTERN.matcher(operationName).matches()) {
            throw new OperationFormatException("'" + operationName + "' is not a valid operation name.");
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

        assertValidParameterName(propertyName);
        validatedPropertyName(propertyName);
    }

    protected abstract void validatedPropertyName(String propertyName) throws OperationFormatException;

    /* (non-Javadoc)
     * @see org.jboss.as.cli.operation.OperationParser.CallbackHandler#property(java.lang.String, java.lang.String, int)
     */
    @Override
    public void property(String name, String value, int nameValueSeparatorIndex)
            throws OperationFormatException {

        assertValidParameterName(name);

        if (value.isEmpty()) {
            throw new OperationFormatException("Parameter '" + value + "' is missing value.");
        }

        validatedProperty(name, value, nameValueSeparatorIndex);
    }

    protected abstract void validatedProperty(String name, String value, int nameValueSeparatorIndex) throws OperationFormatException;

    protected void assertValidType(String nodeType)
            throws OperationFormatException {
        if (nodeType == null || !ALPHANUMERICS_PATTERN.matcher(nodeType).matches()) {
            throw new OperationFormatException("'" + nodeType + "' is not a valid node type name.");
        }
    }

    protected void assertValidNodeName(String nodeName)
            throws OperationFormatException {
        if (nodeName == null || !NODE_NAME_PATTERN.matcher(nodeName).matches()) {
            throw new OperationFormatException("'" + nodeName + "' is not a valid node name.");
        }
    }

    protected void assertValidParameterName(String name)
            throws OperationFormatException {
        if (name == null || !ALPHANUMERICS_PATTERN.matcher(name).matches()) {
            throw new OperationFormatException("'" + name + "' is not a valid parameter name.");
        }
    }

}
