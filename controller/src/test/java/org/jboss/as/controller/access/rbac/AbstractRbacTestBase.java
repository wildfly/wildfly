/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.access.rbac;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.test.AbstractControllerTestBase;
import org.jboss.dmr.ModelNode;

/**
 * @author Ladislav Thon <lthon@redhat.com>
 */
public abstract class AbstractRbacTestBase extends AbstractControllerTestBase {
    protected ModelNode executeWithRole(ModelNode operation, StandardRole role) {
        return executeWithRoles(operation, role);
    }

    protected ModelNode executeWithRoles(ModelNode operation, StandardRole... roles) {
        for (StandardRole role : roles) {
            operation.get(OPERATION_HEADERS, "roles").add(role.name());
        }
        return getController().execute(operation, null, null, null);
    }

    protected static void assertPermitted(ModelNode operationResult) {
        assertEquals(SUCCESS, operationResult.get(OUTCOME).asString());
    }

    protected static void assertDenied(ModelNode operationResult) {
        assertEquals(FAILED, operationResult.get(OUTCOME).asString());
        assertTrue(operationResult.get(FAILURE_DESCRIPTION).asString().contains("Permission denied"));
    }

    protected static void assertNoAddress(ModelNode operationResult) {
        assertEquals(FAILED, operationResult.get(OUTCOME).asString());
        assertTrue(operationResult.get(FAILURE_DESCRIPTION).asString().contains("not found"));
    }

    protected static enum ResultExpectation { PERMITTED, DENIED, NO_ACCESS }

    protected static void assertOperationResult(ModelNode operationResult, ResultExpectation resultExpectation) {
        switch (resultExpectation) {
            case PERMITTED: assertPermitted(operationResult); break;
            case DENIED:    assertDenied(operationResult); break;
            case NO_ACCESS: assertNoAddress(operationResult); break;
        }
    }

    protected void permitted(String operation, PathAddress pathAddress, StandardRole role) {
        assertPermitted(executeWithRole(Util.createOperation(operation, pathAddress), role));
    }

    protected void denied(String operation, PathAddress pathAddress, StandardRole role) {
        assertDenied(executeWithRole(Util.createOperation(operation, pathAddress), role));
    }

    protected void noAddress(String operation, PathAddress pathAddress, StandardRole role) {
        assertNoAddress(executeWithRole(Util.createOperation(operation, pathAddress), role));
    }
}
