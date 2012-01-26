/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.controller.test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_TYPES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;

import java.util.Set;

import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.registry.AttributeAccess.AccessType;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class DomainServerGlobalOperationsTestCase extends AbstractGlobalOperationsTestCase {

    public DomainServerGlobalOperationsTestCase() {
        super(ProcessType.DOMAIN_SERVER, AccessType.READ_ONLY);
    }

    @Test
    public void testReadResourceDescriptionOperation() throws Exception {
        ModelNode operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION);
        ModelNode result = executeForResult(operation);
        checkRootNodeDescription(result, false, false);
        assertFalse(result.get(OPERATIONS).isDefined());

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA");
        result = executeForResult(operation);
        checkProfileNodeDescription(result, false, false);

        //TODO this is not possible - the wildcard address does not correspond to anything in the real model
        //operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "*");
        //result = execute(operation);
        //checkProfileNodeDescription(result, false);

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1");
        result = executeForResult(operation);
        checkSubsystem1Description(result, false, false);
        assertFalse(result.get(OPERATIONS).isDefined());

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1", "type1", "thing1");
        result = executeForResult(operation);
        checkType1Description(result);
        assertFalse(result.get(OPERATIONS).isDefined());

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1", "type1", "thing2");
        result = executeForResult(operation);
        checkType1Description(result);
        assertFalse(result.get(OPERATIONS).isDefined());

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1", "type2", "other");
        result = executeForResult(operation);
        checkType2Description(result);
        assertFalse(result.get(OPERATIONS).isDefined());
    }

    @Test
    public void testReadRecursiveResourceDescriptionOperation() throws Exception {
        ModelNode operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION);
        operation.get(RECURSIVE).set(true);
        ModelNode result = executeForResult(operation);
        checkRootNodeDescription(result, true, false);
        assertFalse(result.get(OPERATIONS).isDefined());

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA");
        operation.get(RECURSIVE).set(true);
        result = executeForResult(operation);
        checkProfileNodeDescription(result, true, false);

        //TODO this is not possible - the wildcard address does not correspond to anything in the real model
        //operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "*");
        //operation.get(RECURSIVE).set(true);
        //result = execute(operation);
        //checkProfileNodeDescription(result, false);

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1");
        operation.get(RECURSIVE).set(true);
        result = executeForResult(operation);
        checkSubsystem1Description(result, true, false);
        assertFalse(result.get(OPERATIONS).isDefined());

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1", "type1", "thing1");
        operation.get(RECURSIVE).set(true);
        result = executeForResult(operation);
        checkType1Description(result);
        assertFalse(result.get(OPERATIONS).isDefined());

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1", "type1", "thing2");
        operation.get(RECURSIVE).set(true);
        result = executeForResult(operation);
        checkType1Description(result);
        assertFalse(result.get(OPERATIONS).isDefined());

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1", "type2", "other");
        operation.get(RECURSIVE).set(true);
        result = executeForResult(operation);
        checkType2Description(result);
        assertFalse(result.get(OPERATIONS).isDefined());
    }

    @Test
    public void testReadResourceDescriptionWithOperationsOperation() throws Exception {
        ModelNode operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION);
        operation.get(OPERATIONS).set(true);
        ModelNode result = executeForResult(operation);
        checkRootNodeDescription(result, false, true);
        assertTrue(result.require(OPERATIONS).isDefined());
        Set<String> ops = result.require(OPERATIONS).keys();
        assertTrue(ops.contains(READ_ATTRIBUTE_OPERATION));
        assertTrue(ops.contains(READ_CHILDREN_NAMES_OPERATION));
        assertTrue(ops.contains(READ_CHILDREN_TYPES_OPERATION));
        assertTrue(ops.contains(READ_OPERATION_DESCRIPTION_OPERATION));
        assertTrue(ops.contains(READ_OPERATION_NAMES_OPERATION));
        assertTrue(ops.contains(READ_RESOURCE_DESCRIPTION_OPERATION));
        assertTrue(ops.contains(READ_RESOURCE_OPERATION));
        for (String op : ops) {
            assertEquals(op, result.require(OPERATIONS).require(op).require(OPERATION_NAME).asString());
        }

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1");
        operation.get(OPERATIONS).set(true);
        result = executeForResult(operation);
        checkSubsystem1Description(result, false, true);

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1", "type1", "thing1");
        operation.get(OPERATIONS).set(true);
        result = executeForResult(operation);
        checkType1Description(result);

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1", "type1", "thing2");
        operation.get(OPERATIONS).set(true);
        result = executeForResult(operation);
        checkType1Description(result);

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1", "type2", "other");
        operation.get(OPERATIONS).set(true);
        result = executeForResult(operation);
        checkType2Description(result);
    }

    @Test
    public void testRecursiveReadResourceDescriptionWithOperationsOperation() throws Exception {
        ModelNode operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION);
        operation.get(OPERATIONS).set(true);
        operation.get(RECURSIVE).set(true);
        ModelNode result = executeForResult(operation);
        checkRootNodeDescription(result, true, true);


        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1");
        operation.get(OPERATIONS).set(true);
        operation.get(RECURSIVE).set(true);
        result = executeForResult(operation);
        checkSubsystem1Description(result, true, true);

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1", "type1", "thing1");
        operation.get(OPERATIONS).set(true);
        operation.get(RECURSIVE).set(true);
        result = executeForResult(operation);
        checkType1Description(result);

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1", "type1", "thing2");
        operation.get(OPERATIONS).set(true);
        operation.get(RECURSIVE).set(true);
        result = executeForResult(operation);
        checkType1Description(result);

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1", "type2", "other");
        operation.get(OPERATIONS).set(true);
        operation.get(RECURSIVE).set(true);
        result = executeForResult(operation);
        checkType2Description(result);
    }
}
