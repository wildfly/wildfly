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
package org.jboss.as.cli.impl;

import java.util.List;
import java.util.Set;

import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandHandler;
import org.jboss.as.cli.ParsedArguments;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.OperationRequestParser;
import org.jboss.as.cli.operation.ParsedOperationRequest;
import org.jboss.as.cli.operation.impl.DefaultOperationCallbackHandler;
import org.jboss.as.cli.parsing.TheParser;

/**
 *
 * @author Alexey Loubyansky
 */
public class DefaultParsedArguments implements ParsedArguments, OperationRequestParser.CallbackHandler, ParsedOperationRequest {

    /** current command's arguments */
    private String argsStr;
    private boolean parsed;

    protected DefaultOperationCallbackHandler opCallback = new DefaultOperationCallbackHandler();

    public void reset(String args, CommandHandler handler) {
        argsStr = args;
        opCallback.reset();
        parsed = false;
    }

    public void parse(String args) throws CommandFormatException {
        reset(args, null);
        parseArgs();
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.ParsedArguments#getArgumentsString()
     */
    @Override
    public String getArgumentsString() {
        return argsStr;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.ParsedArguments#hasArguments()
     */
    @Override
    public boolean hasArguments() throws CommandFormatException {
        if(!parsed) {
            parseArgs();
        }
        return !opCallback.getPropertyNames().isEmpty() || !opCallback.getOtherArguments().isEmpty();
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.ParsedArguments#hasArgument(java.lang.String)
     */
    @Override
    public boolean hasArgument(String argName) throws CommandFormatException {
        if(!parsed) {
            parseArgs();
        }
        return opCallback.getPropertyNames().contains(argName);
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.ParsedArguments#getArgument(java.lang.String)
     */
    @Override
    public String getArgument(String argName) throws CommandFormatException {
        if(!parsed) {
            parseArgs();
        }
        return opCallback.getPropertyValue(argName);
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.ParsedArguments#getArgumentNames()
     */
    @Override
    public Set<String> getArgumentNames() throws CommandFormatException {
        if(!parsed) {
            parseArgs();
        }
        return opCallback.getPropertyNames();
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.ParsedArguments#getOtherArguments()
     */
    @Override
    public List<String> getOtherArguments() throws CommandFormatException {
        if(!parsed) {
            parseArgs();
        }
        return opCallback.getOtherArguments();
    }

    protected void parseArgs() throws CommandFormatException {
        if (argsStr != null && !argsStr.isEmpty()) {
            callParser(argsStr);
        }
        parsed = true;
    }

    protected void callParser(String argsStr) throws CommandFormatException {
        TheParser.parseCommandArgs(argsStr, this);
    }

    @Override
    public void outputTarget(String outputTarget) throws CommandFormatException {
        opCallback.outputTarget(outputTarget);
    }

    @Override
    public String getOutputTarget() {
        return opCallback.getOutputTarget();
    }

    @Override
    public void start(String operationString) {
        opCallback.start(operationString);
    }

    @Override
    public void rootNode() {
        opCallback.rootNode();
    }

    @Override
    public void parentNode() {
        opCallback.parentNode();
    }

    @Override
    public void nodeType() {
        opCallback.nodeType();
    }

    @Override
    public void nodeType(String nodeType) throws OperationFormatException {
        opCallback.nodeType(nodeType);
    }

    @Override
    public void nodeTypeNameSeparator(int index) {
        opCallback.nodeTypeNameSeparator(index);
    }

    @Override
    public void nodeName(String nodeName) throws OperationFormatException {
        opCallback.nodeName(nodeName);
    }

    @Override
    public void nodeSeparator(int index) {
        opCallback.nodeSeparator(index);
    }

    @Override
    public void addressOperationSeparator(int index) {
        opCallback.addressOperationSeparator(index);
    }

    @Override
    public void operationName(String operationName)
            throws CommandFormatException {
        opCallback.operationName(operationName);
    }

    @Override
    public void propertyListStart(int index) {
        opCallback.propertyListStart(index);
    }

    @Override
    public void propertyName(String propertyName)
            throws CommandFormatException {
        opCallback.propertyName(propertyName);
    }

    @Override
    public void propertyNameValueSeparator(int index) {
        opCallback.propertyNameValueSeparator(index);
    }

    @Override
    public void property(String name, String value, int nameValueSeparatorIndex)
            throws CommandFormatException {
        opCallback.property(name, value, nameValueSeparatorIndex);
    }

    @Override
    public void propertySeparator(int index) {
        opCallback.propertySeparator(index);
    }

    @Override
    public void propertyListEnd(int index) {
        opCallback.propertyListEnd(index);
    }

    @Override
    public void nodeTypeOrName(String typeOrName)
            throws OperationFormatException {
        opCallback.nodeTypeOrName(typeOrName);
    }

    @Override
    public boolean isRequestComplete() {
        return opCallback.isRequestComplete();
    }

    @Override
    public boolean endsOnPropertySeparator() {
        return opCallback.endsOnPropertySeparator();
    }

    @Override
    public boolean endsOnPropertyValueSeparator() {
        return opCallback.endsOnPropertyValueSeparator();
    }

    @Override
    public boolean endsOnPropertyListStart() {
        return opCallback.endsOnPropertyListStart();
    }

    @Override
    public boolean endsOnAddressOperationNameSeparator() {
        return opCallback.endsOnAddressOperationNameSeparator();
    }

    @Override
    public boolean endsOnNodeSeparator() {
        return opCallback.endsOnNodeSeparator();
    }

    @Override
    public boolean endsOnNodeTypeNameSeparator() {
        return opCallback.endsOnNodeTypeNameSeparator();
    }

    @Override
    public boolean hasAddress() {
        return opCallback.hasAddress();
    }

    @Override
    public OperationRequestAddress getAddress() {
        return opCallback.getAddress();
    }

    @Override
    public boolean hasOperationName() {
        return opCallback.hasOperationName();
    }

    @Override
    public String getOperationName() {
        return opCallback.getOperationName();
    }

    @Override
    public boolean hasProperties() {
        return opCallback.hasProperties();
    }

    @Override
    public Set<String> getPropertyNames() {
        return opCallback.getPropertyNames();
    }

    @Override
    public String getPropertyValue(String name) {
        return opCallback.getPropertyValue(name);
    }

    @Override
    public int getLastSeparatorIndex() {
        return opCallback.getLastSeparatorIndex();
    }

    public void setParsed() {
        parsed = true;
    }

    @Override
    public String getLastParsedPropertyName() {
        return opCallback.getLastParsedPropertyName();
    }

    @Override
    public String getLastParsedPropertyValue() {
        return opCallback.getLastParsedPropertyValue();
    }

    @Override
    public boolean isValueComplete(String propertyName) {
        return opCallback.isValueComplete(propertyName);
    }
}
