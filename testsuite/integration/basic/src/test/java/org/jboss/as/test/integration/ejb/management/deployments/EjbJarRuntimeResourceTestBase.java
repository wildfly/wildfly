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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;
import static org.jboss.as.test.integration.ejb.remote.common.EJBManagementUtil.MESSAGE_DRIVEN;
import static org.jboss.as.test.integration.ejb.remote.common.EJBManagementUtil.SINGLETON;
import static org.jboss.as.test.integration.ejb.remote.common.EJBManagementUtil.STATEFUL;
import static org.jboss.as.test.integration.ejb.remote.common.EJBManagementUtil.STATELESS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.TransactionManagementType;

import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;

/**
 * Base class for tests of management resources exposed by runtime EJB components.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class EjbJarRuntimeResourceTestBase {
    protected static final String SECURITY_DOMAIN = "security-domain";

    protected static final String MODULE_NAME = "ejb-management";
    protected static final String JAR_NAME = MODULE_NAME + ".jar";

    private static final String COMPONENT_CLASS_NAME = "component-class-name";
    private static final String DECLARED_ROLES = "declared-roles";
    private static final String POOL_NAME = "pool-name";
    private static final String RUN_AS_ROLE = "run-as-role";
    private static final String TIMER_ATTRIBUTE = "timers";

    private static final String[] POOL_ATTRIBUTES =
            {"pool-available-count", "pool-create-count", "pool-current-size", POOL_NAME, "pool-max-size", "pool-remove-count"};

    private static final String[] TIMER_ATTRIBUTES = {"time-remaining", "next-timeout", "calendar-timer"};
    private static final String[] SCHEDULE_ATTRIBUTES = {"day-of-month", "day-of-week", "hour", "minute", "year", "timezone", "start", "end"};

    private static final String JNDI_NAMES = "jndi-names";
    private static final String BUSINESS_LOCAL = "business-local";
    private static final String BUSINESS_REMOTE = "business-remote";
    private static final String TIMEOUT_METHOD = "timeout-method";
    private static final String ASYNC_METHODS = "async-methods";
    private static final String TRANSACTION_TYPE = "transaction-type";

    // MDB specific resource names
    private static final String ACTIVATION_CONFIG = "activation-config";
    private static final String MESSAGING_TYPE = "messaging-type";
    private static final String MESSAGE_DESTINATION_TYPE = "message-destination-type";
    private static final String MESSAGE_DESTINATION_LINK = "message-destination-link";

    // STATEFUL specific resource names
    private static final String STATEFUL_TIMEOUT = "stateful-timeout";
    private static final String AFTER_BEGIN_METHOD = "after-begin-method";
    private static final String BEFORE_COMPLETION_METHOD = "before-completion-method";
    private static final String AFTER_COMPLETION_METHOD = "after-completion-method";
    private static final String PASSIVATION_CAPABLE = "passivation-capable";
    private static final String REMOVE_METHODS = "remove-methods";
    private static final String RETAIN_IF_EXCEPTION = "retain-if-exception";
    private static final String BEAN_METHOD = "bean-method";

    // SINGLETON specific resource names
    private static final String DEPENDS_ON = "depends-on";
    private static final String INIT_ON_STARTUP = "init-on-startup";
    private static final String CONCURRENCY_MANAGEMENT_TYPE = "concurrency-management-type";

    @ContainerResource
    private ManagementClient managementClient;

    public static Archive<?> getEJBJar() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, JAR_NAME);
        jar.addPackage(EjbJarRuntimeResourcesTestCase.class.getPackage());
        jar.addClass(TimeoutUtil.class);
        jar.addAsManifestResource(EjbJarRuntimeResourcesTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        jar.addAsManifestResource(EjbJarRuntimeResourcesTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        return jar;
    }

    private final PathAddress baseAddress;

    protected EjbJarRuntimeResourceTestBase(final PathAddress baseAddress) {
        this.baseAddress = baseAddress;
    }

    @Test
    public void testMDB() throws Exception {
        testComponent(MESSAGE_DRIVEN, ManagedMDB.class.getSimpleName(), true);
    }

    @Test
    public void testNoTimerMDB() throws Exception {
        testComponent(MESSAGE_DRIVEN, NoTimerMDB.class.getSimpleName(), false);
    }

    @Test
    public void testSLSB() throws Exception {
        testComponent(STATELESS, ManagedStatelessBean.class.getSimpleName(), true);
    }

    @Test
    public void testNoTimerSLSB() throws Exception {
        testComponent(STATELESS, NoTimerStatelessBean.class.getSimpleName(), false);
    }

    @Test
    public void testSingleton() throws Exception {
        testComponent(SINGLETON, ManagedSingletonBean.class.getSimpleName(), true);
    }

    @Test
    public void testNoTimerSingleton() throws Exception {
        testComponent(SINGLETON, NoTimerSingletonBean.class.getSimpleName(), false);
    }

    @Test
    public void testSFSB() throws Exception {
        testComponent(STATEFUL, ManagedStatefulBean.class.getSimpleName(), false);
    }

    /*
    TODO implement a test of entity beans
    @Test
    public void testEntityBean() throws Exception {
        testComponent(ENTITY, ???.class.getSimpleName());
    }
    */

    private void testComponent(String type, String name, boolean expectTimer) throws Exception {

        ModelNode address = getComponentAddress(type, name).toModelNode();
        address.protect();
        ModelNode resourceDescription = executeOperation(managementClient, ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION, address);
        ModelNode resource = executeOperation(managementClient, ModelDescriptionConstants.READ_RESOURCE_OPERATION, address);

        assertTrue(resourceDescription.get(ATTRIBUTES, COMPONENT_CLASS_NAME).isDefined());
        assertEquals(ModelType.STRING, resourceDescription.get(ATTRIBUTES, COMPONENT_CLASS_NAME, DESCRIPTION).getType());
        assertEquals(ModelType.STRING, resourceDescription.get(ATTRIBUTES, COMPONENT_CLASS_NAME, TYPE).asType());

        final ModelNode componentClassNameNode = resource.get(COMPONENT_CLASS_NAME);
        assertTrue(componentClassNameNode.isDefined());
        final String componentClassName = componentClassNameNode.asString();

        validateSecurity(address, resourceDescription, resource);

        if (!STATEFUL.equals(type) && !SINGLETON.equals(type)) {
            validatePool(address, resourceDescription, resource);
        } else {
            for (String attr : POOL_ATTRIBUTES) {
                assertFalse(resourceDescription.get(ModelDescriptionConstants.ATTRIBUTES).has(attr));
                assertFalse(resource.has(attr));
            }
        }

        if (STATELESS.equals(type) || SINGLETON.equals(type) || MESSAGE_DRIVEN.equals(type)) {
            validateTimer(address, resourceDescription, resource, expectTimer);
            assertEquals(TransactionManagementType.CONTAINER.name(), resource.get(TRANSACTION_TYPE).asString());
        } else {
            assertFalse(resourceDescription.get(ModelDescriptionConstants.ATTRIBUTES).has(TIMER_ATTRIBUTE));
            assertFalse(resource.has(TIMER_ATTRIBUTE));

            // stateful beans are configured to have bean managed transaction
            assertEquals(TransactionManagementType.BEAN.name(), resource.get(TRANSACTION_TYPE).asString());

            // STATEFUL specific resources
            if (componentClassName.equals("org.jboss.as.test.integration.ejb.management.deployments.ManagedStatefulBean")) {
                assertTrue(resource.get(PASSIVATION_CAPABLE).asBoolean());
                assertFalse(resource.get(AFTER_BEGIN_METHOD).isDefined());
                assertFalse(resource.get(BEFORE_COMPLETION_METHOD).isDefined());
                assertFalse(resource.get(AFTER_COMPLETION_METHOD).isDefined());
            } else {
                assertFalse(resource.get(PASSIVATION_CAPABLE).asBoolean());
                assertEquals("2 HOURS", resource.get(STATEFUL_TIMEOUT).asString());
                assertEquals("private void afterBegin()", resource.get(AFTER_BEGIN_METHOD).asString());
                assertEquals("private void beforeCompletion()", resource.get(BEFORE_COMPLETION_METHOD).asString());
                assertEquals("private void afterCompletion()", resource.get(AFTER_COMPLETION_METHOD).asString());

                final ModelNode removeMethodsNode = resource.get(REMOVE_METHODS);
                final List<ModelNode> removeMethodsList = removeMethodsNode.asList();

                // ManagedStatefulBean contains 1 remove method (inherited)
                // ManagedStatefulBean2 declares 2 remove methods and inherit 1
                assertTrue(removeMethodsList.size() == 1 || removeMethodsList.size() == 3);
                for (ModelNode m : removeMethodsList) {
                    final String beanMethod = m.get(BEAN_METHOD).asString();
                    final boolean retainIfException = m.get(RETAIN_IF_EXCEPTION).asBoolean();
                    if (beanMethod.contains("void removeTrue()")) {
                        assertTrue(retainIfException);
                    } else if (beanMethod.contains("void removeFalse()") || beanMethod.contains("void remove()")) {
                        assertFalse(retainIfException);
                    } else {
                        fail("Unknown stateful bean remove method: " + beanMethod);
                    }
                }
            }
        }

        // SINGLETON specific resource
        if (SINGLETON.equals(type)) {
            final ModelNode concurrencyTypeNode = resource.get(CONCURRENCY_MANAGEMENT_TYPE);
            final ModelNode initOnStartUpNode = resource.get(INIT_ON_STARTUP);
            final ModelNode dependsOnNode = resource.get(DEPENDS_ON);
            if (componentClassName.equals("org.jboss.as.test.integration.ejb.management.deployments.ManagedSingletonBean")) {
                assertFalse(initOnStartUpNode.asBoolean());
                assertFalse(dependsOnNode.isDefined());
                assertFalse(concurrencyTypeNode.isDefined());
            } else {
                assertTrue(initOnStartUpNode.asBoolean());
                assertEquals(ConcurrencyManagementType.BEAN.name(), concurrencyTypeNode.asString());
                final List<ModelNode> dependsOnList = dependsOnNode.asList();
                assertEquals(1, dependsOnList.size());
                for (ModelNode d : dependsOnList) {
                    if (!d.asString().equals("ManagedSingletonBean")) {
                        fail("Unknown value of depends-on: " + d.asString());
                    }
                }
            }
        }

        // MDB specific resources
        if (MESSAGE_DRIVEN.equals(type)) {
            assertEquals("javax.jms.MessageListener", resource.get(MESSAGING_TYPE).asString());

            if (componentClassName.equals("org.jboss.as.test.integration.ejb.management.deployments.NoTimerMDB")) {
                assertEquals("javax.jms.Queue", resource.get(MESSAGE_DESTINATION_TYPE).asString());
                assertEquals("queue/NoTimerMDB-queue", resource.get(MESSAGE_DESTINATION_LINK).asString());
            }

            final ModelNode activationConfigNode = resource.get(ACTIVATION_CONFIG);
            assertTrue(activationConfigNode.isDefined());
            final List<Property> activationConfigProps = activationConfigNode.asPropertyList();
            assertTrue(activationConfigProps.size() >= 2);
            for (Property p : activationConfigProps) {
                final String pName = p.getName();
                final String pValue = p.getValue().asString();
                switch (pName) {
                    case "destinationType":
                        assertEquals("javax.jms.Queue", pValue);
                        break;
                    case "destination":
                        assertTrue(pValue.startsWith("java:/queue/"));
                        break;
                    case "acknowledgeMode":
                        assertEquals("Auto-acknowledge", pValue);
                        break;
                    default:
                        fail("Unknown activation config property: " + pName);
                        break;
                }
            }
        } else {
            // resources for stateless, stateful and singleton
            if (expectTimer) {
                // the @Asynchronous method is declared on the bean super class AbstractManagedBean
                // Beans named NoTimer* do not extend AbstractManagedBean and so do not have async methods.
                final ModelNode asyncMethodsNode = resource.get(ASYNC_METHODS);
                final List<ModelNode> asyncMethodsList = asyncMethodsNode.asList();
                assertEquals(1, asyncMethodsList.size());
                for (ModelNode m : asyncMethodsList) {
                    if (!m.asString().contains("void async(int, int)")) {
                        fail("Unknown async methods: " + m.asString());
                    }
                }
            }

            final ModelNode businessRemoteNode = resource.get(BUSINESS_REMOTE);
            final List<ModelNode> businessRemoteList = businessRemoteNode.asList();
            assertEquals(1, businessRemoteList.size());
            for (ModelNode r : businessRemoteList) {
                if (!r.asString().equals("org.jboss.as.test.integration.ejb.management.deployments.BusinessInterface")) {
                    fail("Unknown business remote interface: " + r.asString());
                }
            }

            final ModelNode businessLocalNode = resource.get(BUSINESS_LOCAL);
            final List<ModelNode> businessLocalList = businessLocalNode.asList();
            assertEquals(1, businessLocalList.size());
            for (ModelNode l : businessLocalList) {
                if (!l.asString().equals(componentClassName)) {
                    fail("Unknown business local interface: " + l.asString());
                }
            }

            final ModelNode jndiNamesNode = resource.get(JNDI_NAMES);
            final List<ModelNode> jndiNamesList = jndiNamesNode.asList();

            //each session bean has 2 business interfaces (remote and local), in
            //3 scopes (global, app & module), plus jboss-specific names
            assertTrue(jndiNamesList.size() >= 6);
            for (ModelNode j : jndiNamesList) {
                final String n = j.asString();
                if (!(n.startsWith("java:global/") || n.startsWith("java:app/") || n.startsWith("java:module/")
                        || n.startsWith("ejb:/") || n.startsWith("java:jboss/"))) {
                    fail("Unknown jndi name for " + name + ": " + n);
                }
            }
        }
    }

    private void validateSecurity(ModelNode address, ModelNode resourceDescription, ModelNode resource) {

        assertTrue(resourceDescription.get(ATTRIBUTES, SECURITY_DOMAIN).isDefined());
        assertEquals(ModelType.STRING, resourceDescription.get(ATTRIBUTES, SECURITY_DOMAIN, DESCRIPTION).getType());
        assertEquals(ModelType.STRING, resourceDescription.get(ATTRIBUTES, SECURITY_DOMAIN, TYPE).asType());

        assertTrue(resource.get(SECURITY_DOMAIN).isDefined());
        assertEquals("other", resource.get(SECURITY_DOMAIN).asString());

        assertTrue(resourceDescription.get(ATTRIBUTES, RUN_AS_ROLE).isDefined());
        assertEquals(ModelType.STRING, resourceDescription.get(ATTRIBUTES, RUN_AS_ROLE, DESCRIPTION).getType());
        assertEquals(ModelType.STRING, resourceDescription.get(ATTRIBUTES, RUN_AS_ROLE, TYPE).asType());

        assertTrue(resource.get(RUN_AS_ROLE).isDefined());
        assertEquals("Role3", resource.get(RUN_AS_ROLE).asString());

        assertTrue(resourceDescription.get(ATTRIBUTES, DECLARED_ROLES).isDefined());
        assertEquals(ModelType.STRING, resourceDescription.get(ATTRIBUTES, DECLARED_ROLES, DESCRIPTION).getType());
        assertEquals(ModelType.LIST, resourceDescription.get(ATTRIBUTES, DECLARED_ROLES, TYPE).asType());
        assertEquals(ModelType.STRING, resourceDescription.get(ATTRIBUTES, DECLARED_ROLES, VALUE_TYPE).asType());

        assertTrue(resource.get(DECLARED_ROLES).isDefined());
        assertEquals(ModelType.LIST, resource.get(DECLARED_ROLES).getType());
        final List<ModelNode> roles = resource.get(DECLARED_ROLES).asList();
        for (int i = 1; i < 4; i++) {
            assertTrue(roles.contains(new ModelNode().set("Role" + i)));
        }
        assertEquals(3, roles.size());

    }

    private void validatePool(ModelNode address, ModelNode resourceDescription, ModelNode resource) {

        for (String attr : POOL_ATTRIBUTES) {
            final ModelType expectedType = POOL_NAME.equals(attr) ? ModelType.STRING : ModelType.INT;
            assertTrue(resourceDescription.get(ATTRIBUTES, attr).isDefined());
            assertEquals(ModelType.STRING, resourceDescription.get(ATTRIBUTES, attr, DESCRIPTION).getType());
            assertEquals(expectedType, resourceDescription.get(ATTRIBUTES, attr, TYPE).asType());

            assertTrue(attr + " is not defined", resource.get(attr).isDefined());
            assertEquals(expectedType, resource.get(attr).getType());
        }
    }

    private void validateTimer(ModelNode address, ModelNode resourceDescription, ModelNode resource, boolean expectTimer) {

        assertTrue(resourceDescription.get(ATTRIBUTES, TIMER_ATTRIBUTE).isDefined());
        assertEquals(ModelType.STRING, resourceDescription.get(ATTRIBUTES, TIMER_ATTRIBUTE, DESCRIPTION).getType());
        assertEquals(ModelType.LIST, resourceDescription.get(ATTRIBUTES, TIMER_ATTRIBUTE, TYPE).asType());
        assertEquals(ModelType.OBJECT, resourceDescription.get(ATTRIBUTES, TIMER_ATTRIBUTE, VALUE_TYPE).getType());

        final ModelNode timerAttr = resource.get(TIMER_ATTRIBUTE);
        assertTrue(timerAttr.isDefined());
        final List<ModelNode> timers = timerAttr.asList();
        if (!expectTimer) {
            assertEquals(0, timers.size());
        } else {
            assertEquals(1, timers.size());
            final ModelNode timer = timers.get(0);
            assertTrue(timer.get("persistent").isDefined());
            assertFalse(timer.get("persistent").asBoolean());
            assertTrue(timer.get("schedule", "second").isDefined());
            assertEquals("15", timer.get("schedule", "second").asString());
            assertTrue(timer.get("info").isDefined());
            assertEquals("timer1", timer.get("info").asString());
            for (String field : TIMER_ATTRIBUTES) {
                assertTrue(field, timer.has(field));
            }

            for (String field : SCHEDULE_ATTRIBUTES) {
                assertTrue(field, timer.get("schedule").has(field));
            }
        }
        if (expectTimer) {
            assertTrue(resource.get(TIMEOUT_METHOD).asString().contains("timeout(javax.ejb.Timer)"));
        }
    }

    static ModelNode execute(final ManagementClient managementClient, final ModelNode op) throws IOException {
        ModelNode response = managementClient.getControllerClient().execute(op);
        assertTrue(response.isDefined());
        if (!Operations.isSuccessfulOutcome(response)) {
            fail(Operations.getFailureDescription(response).asString());
        }

        return response.get(ModelDescriptionConstants.RESULT);
    }

    static ModelNode executeOperation(final ManagementClient managementClient, final String name, final ModelNode address) throws IOException {

        final ModelNode op = new ModelNode();
        op.get(ModelDescriptionConstants.OP).set(name);
        op.get(ModelDescriptionConstants.OP_ADDR).set(address);
        op.get(ModelDescriptionConstants.INCLUDE_RUNTIME).set(true);
        op.get(ModelDescriptionConstants.OPERATIONS).set(true);

        return execute(managementClient, op);
    }

    static PathAddress componentAddress(final PathAddress baseAddress, final String type, final String name) {
        return baseAddress.append(PathElement.pathElement(SUBSYSTEM, "ejb3")).append(PathElement.pathElement(type, name));
    }

    private PathAddress getComponentAddress(String type, String name) {
        return componentAddress(this.baseAddress, type, name);
    }
}
