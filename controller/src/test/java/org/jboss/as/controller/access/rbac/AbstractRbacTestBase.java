package org.jboss.as.controller.access.rbac;

import org.jboss.as.controller.test.AbstractControllerTestBase;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

    protected static void assertNoAccess(ModelNode operationResult) {
        assertEquals(FAILED, operationResult.get(OUTCOME).asString());
        assertTrue(operationResult.get(FAILURE_DESCRIPTION).asString().contains("not found"));
    }

    protected static enum ResultExpectation { PERMITTED, DENIED, NO_ACCESS }

    protected static void assertOperationResult(ModelNode operationResult, ResultExpectation resultExpectation) {
        switch (resultExpectation) {
            case PERMITTED: assertPermitted(operationResult); break;
            case DENIED:    assertDenied(operationResult); break;
            case NO_ACCESS: assertNoAccess(operationResult); break;
        }
    }
}
