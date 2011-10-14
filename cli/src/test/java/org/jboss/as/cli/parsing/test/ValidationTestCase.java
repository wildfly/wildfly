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
package org.jboss.as.cli.parsing.test;

import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.CommandLineParser;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestParser;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class ValidationTestCase {

    private final CommandLineParser parser = DefaultOperationRequestParser.INSTANCE;
    private final DefaultCallbackHandler callback = new DefaultCallbackHandler();

    @Test
    public void testNodeTypes() {

        assertValidType("_");
        assertInvalidType("-");
        assertInvalidType("_-");
        assertValidType("_-_");
    }

    @Test
    public void testNodeNames() {

        assertValidNodeName("_");
        assertValidNodeName("-");
        assertValidNodeName("_-");
        assertValidNodeName("_-_");

        //assertInvalidNodeName(":");
    }

    @Test
    public void testOperationNames() {

        assertValidOperation("_");
        assertInvalidOperation("-");
        assertInvalidOperation("_-");
        assertValidOperation("_-_");
    }

  @Test
    public void testParameterNames() {

        assertValidParamName("_");
        assertInvalidParamName("-");
        assertInvalidParamName("_-");
        assertValidParamName("_-_");
    }

    protected void assertValidType(String type) {
        assertValidInput(type);
    }

    protected void assertInvalidType(String type) {
        assertInvalidInput(type);
    }

    protected void assertValidNodeName(String nodeName) {
        assertValidInput("type=" + nodeName);
    }

    protected void assertInvalidNodeName(String nodeName) {
        assertInvalidInput("type=" + nodeName);
    }

    protected void assertValidOperation(String operation) {
        assertValidInput(":" + operation);
    }

    protected void assertInvalidOperation(String operation) {
        assertInvalidInput(":" + operation);
    }

    protected void assertValidParamName(String paramName) {
        assertValidInput(":op(" + paramName);
    }

    protected void assertInvalidParamName(String paramName) {
        assertInvalidInput(":op(" + paramName);
    }

    protected void assertValidInput(String input) {
        try {
            parse(input);
        } catch (OperationFormatException e) {
            Assert.fail(e.getLocalizedMessage());
        }
    }

    protected void assertInvalidInput(String input) {
        try {
            parse(input);
            Assert.fail("'" + input + "' is not expected to be a valid input.");
        } catch (OperationFormatException e) {
        }
    }

    protected void parse(String input) throws OperationFormatException {
        callback.reset();
        parser.parse(input, callback);
    }
}
