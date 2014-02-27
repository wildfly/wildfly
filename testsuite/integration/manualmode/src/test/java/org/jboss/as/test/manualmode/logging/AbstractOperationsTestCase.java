/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.manualmode.logging;

import static org.jboss.as.controller.client.helpers.ClientConstants.RESULT;
import static org.junit.Assert.*;

import java.io.IOException;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public abstract class AbstractOperationsTestCase {

    protected ModelNode testWrite(final ModelControllerClient client, final ModelNode address, final String attribute, final String value) throws IOException {
        final ModelNode writeOp = Operations.createWriteAttributeOperation(address, attribute, value);
        final ModelNode writeResult = executeOperation(client, writeOp);
        // Create the read operation
        final ModelNode readOp = Operations.createReadAttributeOperation(address, attribute);
        final ModelNode result = executeOperation(client, readOp);
        assertEquals(value, readResultAsString(result));
        return writeResult;
    }

    protected ModelNode testWrite(final ModelControllerClient client, final ModelNode address, final String attribute, final ModelNode value) throws IOException {
        final ModelNode writeOp = Operations.createWriteAttributeOperation(address, attribute, value);
        final ModelNode writeResult = executeOperation(client, writeOp);
        // Create the read operation
        final ModelNode readOp = Operations.createReadAttributeOperation(address, attribute);
        final ModelNode result = executeOperation(client, readOp);
        assertEquals(value, Operations.readResult(result));
        return writeResult;
    }

    protected ModelNode testUndefine(final ModelControllerClient client, final ModelNode address, final String attribute) throws IOException {
        return testUndefine(client, address, attribute, false);
    }

    protected ModelNode testUndefine(final ModelControllerClient client, final ModelNode address, final String attribute, final boolean expectFailure) throws IOException {
        final ModelNode undefineOp = Operations.createUndefineAttributeOperation(address, attribute);
        final ModelNode undefineResult;
        if (expectFailure) {
            undefineResult = client.execute(undefineOp);
            assertFalse("Undefining attribute " + attribute + " should have failed.", Operations.isSuccessfulOutcome(undefineResult));
        } else {
            undefineResult = executeOperation(client, undefineOp);
            // Create the read operation
            final ModelNode readOp = Operations.createReadAttributeOperation(address, attribute);
            readOp.get("include-defaults").set(false);
            final ModelNode result = executeOperation(client, readOp);
            assertFalse("Attribute '" + attribute + "' was not undefined.", Operations.readResult(result).isDefined());
        }
        return undefineResult;
    }

    protected void verifyRemoved(final ModelControllerClient client, final ModelNode address) throws IOException {
        final ModelNode op = Operations.createReadResourceOperation(address);
        final ModelNode result = client.execute(op);
        assertFalse("Resource not removed: " + address, Operations.isSuccessfulOutcome(result));
    }

    protected ModelNode executeOperation(final ModelControllerClient client, final ModelNode op) throws IOException {
        final ModelNode result = client.execute(op);
        assertTrue(getFailureDescriptionAsString(result), Operations.isSuccessfulOutcome(result));
        return result;
    }

    /**
     * Reads the result of an operation and returns the result as a string. If the operation does not have a {@link
     * org.jboss.as.controller.client.helpers.ClientConstants#RESULT} attribute and empty string is returned.
     *
     * @param result the result of executing an operation
     *
     * @return the result of the operation or an empty string
     */
    public static String readResultAsString(final ModelNode result) {
        return (result.hasDefined(RESULT) ? result.get(RESULT).asString() : "");
    }

    /**
     * Parses the result and returns the failure description. If the result was successful, an empty string is
     * returned.
     *
     * @param result the result of executing an operation
     *
     * @return the failure message or an empty string
     */
    public static String getFailureDescriptionAsString(final ModelNode result) {
        if (Operations.isSuccessfulOutcome(result)) {
            return "";
        }
        return Operations.getFailureDescription(result).asString();
    }
}
