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

package org.jboss.as.test.integration.ejb.management.deployments;

import static org.junit.Assert.*;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import static org.jboss.as.ejb3.subsystem.deployment.AbstractEJBComponentResourceDefinition.*;
import static org.jboss.as.ejb3.subsystem.deployment.TimerAttributeDefinition.*;

import java.io.IOException;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.ejb3.subsystem.deployment.EJBComponentType;
import org.jboss.as.ejb3.subsystem.deployment.TimerAttributeDefinition;
import org.jboss.as.test.shared.TestUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Base class for tests of management resources exposed by runtime EJB components.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class EjbJarRuntimeResourceTestBase {

    protected static final String JAR_NAME = "ejb-management.jar";

    private static final AttributeDefinition[] POOL_ATTRIBUTES =
            new AttributeDefinition[]{POOL_AVAILABLE_COUNT, POOL_CREATE_COUNT, POOL_CURRENT_SIZE, POOL_MAX_SIZE, POOL_REMOVE_COUNT};

    private static final String[] TIMER_ATTRIBUTES = { TIME_REMAINING, NEXT_TIMEOUT, CALENDAR_TIMER};
    private static final String[] SCHEDULE_ATTRIBUTES = { DAY_OF_MONTH, DAY_OF_WEEK, HOUR, MINUTE, YEAR, TIMEZONE, TimerAttributeDefinition.START, END };

    public static Archive<?> getEJBJar() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, JAR_NAME);
        jar.addPackage(EjbJarRuntimeResourcesTestCase.class.getPackage());
        return jar;
    }

    private final PathAddress baseAddress;
    private  ModelControllerClient client;

    protected EjbJarRuntimeResourceTestBase(final PathAddress baseAddress) {
        this.baseAddress = baseAddress;
    }

    @Before
    public void setup() throws Exception {
        client = TestUtils.getModelControllerClient();
    }

    @After
    public void tearDown() throws Exception {
        try {
            client.close();
        } catch (Exception ignored) {
            //
        }
    }

    @Test
    public void testMDB() throws Exception {
        testComponent(EJBComponentType.MESSAGE_DRIVEN, ManagedMDB.class.getSimpleName(), true);
    }

    @Test
    public void testNoTimerMDB() throws Exception {
        testComponent(EJBComponentType.MESSAGE_DRIVEN, NoTimerMDB.class.getSimpleName(), false);
    }

    @Test
    public void testSLSB() throws Exception {
        testComponent(EJBComponentType.STATELESS, ManagedStatelessBean.class.getSimpleName(), true);
    }

    @Test
    public void testNoTimerSLSB() throws Exception {
        testComponent(EJBComponentType.STATELESS, NoTimerStatelessBean.class.getSimpleName(), false);
    }

    @Test
    public void testSingleton() throws Exception {
        testComponent(EJBComponentType.SINGLETON, ManagedSingletonBean.class.getSimpleName(), true);
    }

    @Test
    public void testNoTimerSingleton() throws Exception {
        testComponent(EJBComponentType.SINGLETON, NoTimerSingletonBean.class.getSimpleName(), false);
    }

    @Test
    public void testSFSB() throws Exception {
        testComponent(EJBComponentType.STATEFUL, ManagedStatefulBean.class.getSimpleName(), false);
    }

    /*
    TODO implement a test of entity beans
    @Test
    public void testEntityBean() throws Exception {
        testComponent(EJBComponentType.ENTITY, ???.class.getSimpleName());
    }
    */

    private void testComponent(EJBComponentType type, String name, boolean expectTimer) throws Exception {

        ModelNode address = getComponentAddress(type, name).toModelNode();
        address.protect();
        ModelNode resourceDescription = executeOperation(ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION, address);
        ModelNode resource = executeOperation(ModelDescriptionConstants.READ_RESOURCE_OPERATION, address);

        assertTrue(resourceDescription.get(ATTRIBUTES, COMPONENT_CLASS_NAME.getName()).isDefined());
        assertEquals(ModelType.STRING, resourceDescription.get(ATTRIBUTES, COMPONENT_CLASS_NAME.getName(), DESCRIPTION).getType());
        assertEquals(ModelType.STRING, resourceDescription.get(ATTRIBUTES, COMPONENT_CLASS_NAME.getName(), TYPE).asType());

        assertTrue(resource.get(COMPONENT_CLASS_NAME.getName()).isDefined());

        validateSecurity(address, resourceDescription, resource);

        if (type.hasPool()) {
            validatePool(address, resourceDescription, resource);
        } else {
            for (AttributeDefinition attr : POOL_ATTRIBUTES) {
                assertFalse(resourceDescription.get(ModelDescriptionConstants.ATTRIBUTES).has(attr.getName()));
                assertFalse(resource.has(attr.getName()));
            }
        }

        if (type.hasTimer()) {
            validateTimer(address, resourceDescription, resource, expectTimer);
        } else {
            assertFalse(resourceDescription.get(ModelDescriptionConstants.ATTRIBUTES).has(TimerAttributeDefinition.INSTANCE.getName()));
            assertFalse(resource.has(TimerAttributeDefinition.INSTANCE.getName()));
        }

    }

    private void validateSecurity(ModelNode address, ModelNode resourceDescription, ModelNode resource) {

        assertTrue(resourceDescription.get(ATTRIBUTES, SECURITY_DOMAIN.getName()).isDefined());
        assertEquals(ModelType.STRING, resourceDescription.get(ATTRIBUTES, SECURITY_DOMAIN.getName(), DESCRIPTION).getType());
        assertEquals(ModelType.STRING, resourceDescription.get(ATTRIBUTES, SECURITY_DOMAIN.getName(), TYPE).asType());

        assertTrue(resource.get(SECURITY_DOMAIN.getName()).isDefined());
        assertEquals("other", resource.get(SECURITY_DOMAIN.getName()).asString());

        assertTrue(resourceDescription.get(ATTRIBUTES, RUN_AS_ROLE.getName()).isDefined());
        assertEquals(ModelType.STRING, resourceDescription.get(ATTRIBUTES, RUN_AS_ROLE.getName(), DESCRIPTION).getType());
        assertEquals(ModelType.STRING, resourceDescription.get(ATTRIBUTES, RUN_AS_ROLE.getName(), TYPE).asType());

        assertTrue(resource.get(RUN_AS_ROLE.getName()).isDefined());
        assertEquals("Role3", resource.get(RUN_AS_ROLE.getName()).asString());

        assertTrue(resourceDescription.get(ATTRIBUTES, DECLARED_ROLES.getName()).isDefined());
        assertEquals(ModelType.STRING, resourceDescription.get(ATTRIBUTES, DECLARED_ROLES.getName(), DESCRIPTION).getType());
        assertEquals(ModelType.LIST, resourceDescription.get(ATTRIBUTES, DECLARED_ROLES.getName(), TYPE).asType());
        assertEquals(ModelType.STRING, resourceDescription.get(ATTRIBUTES, DECLARED_ROLES.getName(), VALUE_TYPE).asType());

        assertTrue(resource.get(DECLARED_ROLES.getName()).isDefined());
        assertEquals(ModelType.LIST, resource.get(DECLARED_ROLES.getName()).getType());
        final List<ModelNode> roles =  resource.get(DECLARED_ROLES.getName()).asList();
        for (int i = 1; i < 4; i++) {
            assertTrue(roles.contains(new ModelNode().set("Role" + i)));
        }
        assertEquals(3, roles.size());

    }

    private void validatePool(ModelNode address, ModelNode resourceDescription, ModelNode resource) {

        for (AttributeDefinition attr : POOL_ATTRIBUTES) {
            final String name = attr.getName();
            assertTrue(resourceDescription.get(ATTRIBUTES, name).isDefined());
            assertEquals(ModelType.STRING, resourceDescription.get(ATTRIBUTES, name, DESCRIPTION).getType());
            assertEquals(ModelType.INT, resourceDescription.get(ATTRIBUTES, name, TYPE).asType());

            assertTrue(resource.get(name).isDefined());
            assertEquals(ModelType.INT, resource.get(name).getType());
        }
    }

    private void validateTimer(ModelNode address, ModelNode resourceDescription, ModelNode resource, boolean expectTimer) {

        assertTrue(resourceDescription.get(ATTRIBUTES, TimerAttributeDefinition.INSTANCE.getName()).isDefined());
        assertEquals(ModelType.STRING, resourceDescription.get(ATTRIBUTES, TimerAttributeDefinition.INSTANCE.getName(), DESCRIPTION).getType());
        assertEquals(ModelType.LIST, resourceDescription.get(ATTRIBUTES, TimerAttributeDefinition.INSTANCE.getName(), TYPE).asType());
        assertEquals(ModelType.OBJECT, resourceDescription.get(ATTRIBUTES, TimerAttributeDefinition.INSTANCE.getName(), VALUE_TYPE).getType());

        final ModelNode timerAttr = resource.get(TimerAttributeDefinition.INSTANCE.getName());
        assertTrue(timerAttr.isDefined());
        final List<ModelNode> timers = timerAttr.asList();
        if (!expectTimer) {
            assertEquals(0, timers.size());
        }
        else {
            assertEquals(1, timers.size());
            final ModelNode timer = timers.get(0);
            assertTrue(timer.get("persistent").isDefined());
            assertFalse(timer.get("persistent").asBoolean());
            assertTrue(timer.get("schedule", "second").isDefined());
            assertEquals("15", timer.get("schedule", "second").asString());

            for (String field : TIMER_ATTRIBUTES) {
                assertTrue(field, timer.has(field));
            }

            for (String field : SCHEDULE_ATTRIBUTES) {
                assertTrue(field, timer.get("schedule").has(field));
            }
        }
    }

    private ModelNode executeOperation(String name, ModelNode address) throws IOException {

        final ModelNode op = new ModelNode();
        op.get(ModelDescriptionConstants.OP).set(name);
        op.get(ModelDescriptionConstants.OP_ADDR).set(address);
        op.get(ModelDescriptionConstants.INCLUDE_RUNTIME).set(true);
        op.get(ModelDescriptionConstants.OPERATIONS).set(true);

        ModelNode response = client.execute(op);
        assertTrue(response.isDefined());
        ModelNode outcome = response.get(ModelDescriptionConstants.OUTCOME);
        assertTrue(outcome.isDefined());
        if (!ModelDescriptionConstants.SUCCESS.equals(outcome.asString())) {
            System.out.println(response);
        }
        assertEquals(ModelDescriptionConstants.SUCCESS, outcome.asString());

        return response.get(ModelDescriptionConstants.RESULT);
    }

    private PathAddress getComponentAddress(EJBComponentType type, String name) {
        return baseAddress.append(PathElement.pathElement(SUBSYSTEM, "ejb3")).append(PathElement.pathElement(type.getResourceType(), name));
    }
}
