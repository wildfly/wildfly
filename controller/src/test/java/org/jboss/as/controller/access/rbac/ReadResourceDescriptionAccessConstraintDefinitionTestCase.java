/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS_CONSTRAINTS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.APPLICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INHERITED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SENSITIVE;

import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.constraint.ApplicationTypeConfig;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.ApplicationTypeAccessConstraintDefinition;
import org.jboss.as.controller.access.management.ConstrainedResourceDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.test.AbstractControllerTestBase;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ReadResourceDescriptionAccessConstraintDefinitionTestCase extends AbstractControllerTestBase {

    private static final String SOCKET_CONFIG_NAME = SensitivityClassification.SOCKET_CONFIG.getName();
    private static final String DEPLOYMENT_NAME = ApplicationTypeConfig.DEPLOYMENT.getName();

    private static final AccessConstraintDefinition SOCKET_CONFIG_SENSITIVE_CONSTRAINT = new SensitiveTargetAccessConstraintDefinition(SensitivityClassification.SOCKET_CONFIG);
    private static final AccessConstraintDefinition SENSITIVE_CONSTRAINT = new SensitiveTargetAccessConstraintDefinition(new SensitivityClassification("test", "SENSITIVE-CONSTRAINT", true, true, true));
    private static final AccessConstraintDefinition DEPLOYMENT_APPLICATION_CONSTRAINT = new ApplicationTypeAccessConstraintDefinition(ApplicationTypeConfig.DEPLOYMENT);
    private static final AccessConstraintDefinition APPLICATION_CONSTRAINT = new ApplicationTypeAccessConstraintDefinition(new ApplicationTypeConfig("test", "APPLICATION-CONSTRAINT", true));
    private static final OperationDefinition CUSTOM_OPERATION = new SimpleOperationDefinitionBuilder("custom", new NonResolvingResourceDescriptionResolver())
        .addAccessConstraint(SOCKET_CONFIG_SENSITIVE_CONSTRAINT)
        .build();

    private static final AttributeDefinition STANDARD_ATTR = SimpleAttributeDefinitionBuilder.create("attribute-with-no-constraints", ModelType.STRING, false)
            .build();

    private static final AttributeDefinition SENSITIVE_ATTR = SimpleAttributeDefinitionBuilder.create("attribute-with-sensitive-constraints", ModelType.STRING, false)
            .addAccessConstraint(SOCKET_CONFIG_SENSITIVE_CONSTRAINT)
            .addAccessConstraint(SENSITIVE_CONSTRAINT)
            .build();

    private static final AttributeDefinition APPLICATION_ATTR = SimpleAttributeDefinitionBuilder.create("attribute-with-application-constraints", ModelType.STRING, false)
            .addAccessConstraint(DEPLOYMENT_APPLICATION_CONSTRAINT)
            .addAccessConstraint(APPLICATION_CONSTRAINT)
            .build();


    @Test
    public void testReadResourceDefinition() throws Exception {
        ModelNode op = Util.createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, PathAddress.EMPTY_ADDRESS);
        op.get(RECURSIVE).set(true);
        op.get(OPERATIONS).set(true);
        op.get(INHERITED).set(false);
        ModelNode result = executeForResult(op);


        List<Property> rootOps = result.get(OPERATIONS).asPropertyList();
        Assert.assertTrue(rootOps.size() > 0);

        //The root ops should not have any access constraints
        for (Property rootOp : rootOps) {
            Assert.assertFalse(rootOp.getValue().get(ACCESS_CONSTRAINTS).isDefined());
        }

        //Get rid of the root ops to keep the output shorter before printing out
        result.get(OPERATIONS).set("-TRIMMED-");
        //System.out.println(result);


        ModelNode nonConstrained = result.get(CHILDREN, "nonconstrained-resource", MODEL_DESCRIPTION, "*");
        Assert.assertFalse(nonConstrained.get(ACCESS_CONSTRAINTS).isDefined());
        checkAttributesLength(nonConstrained, 1);
        ModelNode attr = getAndCheckDefinedAttribute(nonConstrained, STANDARD_ATTR.getName());
        Assert.assertFalse(attr.get(ACCESS_CONSTRAINTS).isDefined());
        List<Property> ops = checkOperationsSize(nonConstrained, 2);
        for (Property currentOp : ops) {
            Assert.assertFalse(currentOp.getValue().get(ACCESS_CONSTRAINTS).isDefined());
        }

        ModelNode constrained = result.get(CHILDREN, "constrained-resource", MODEL_DESCRIPTION, "*");

        ModelNode accessConstraints = constrained.get(ACCESS_CONSTRAINTS);
        Assert.assertFalse(accessConstraints.hasDefined(APPLICATION));
        Assert.assertTrue(accessConstraints.hasDefined(SENSITIVE));
        Assert.assertEquals(1, accessConstraints.get(SENSITIVE).keys().size());
        checkSensitiveAccessConstraint(accessConstraints.get(SENSITIVE, SOCKET_CONFIG_NAME));
        checkAttributesLength(constrained, 2);
        attr = getAndCheckDefinedAttribute(constrained, STANDARD_ATTR.getName());
        Assert.assertFalse(attr.get(ACCESS_CONSTRAINTS).isDefined());
        attr = getAndCheckDefinedAttribute(constrained, SENSITIVE_ATTR.getName());
        Assert.assertFalse(attr.get(ACCESS_CONSTRAINTS, APPLICATION).isDefined());
        Assert.assertEquals(2, attr.get(ACCESS_CONSTRAINTS, SENSITIVE).keys().size());
        checkSensitiveAccessConstraint(attr.get(ACCESS_CONSTRAINTS, SENSITIVE, SOCKET_CONFIG_NAME));
        checkSocketConfigSensitiveConstraint(attr.get(ACCESS_CONSTRAINTS, SENSITIVE, SENSITIVE_CONSTRAINT.getName()));
        ops = checkOperationsSize(constrained, 3);
        for (Property currentOp : ops) {
            if (currentOp.getName().equals(REMOVE) || currentOp.getName().equals(ADD)) {
                Assert.assertFalse(currentOp.getValue().get(ACCESS_CONSTRAINTS).isDefined());
            } else {
                accessConstraints = currentOp.getValue().get(ACCESS_CONSTRAINTS);
                Assert.assertFalse(accessConstraints.hasDefined(APPLICATION));
                Assert.assertTrue(accessConstraints.hasDefined(SENSITIVE));
                Assert.assertEquals(1, accessConstraints.get(SENSITIVE).keys().size());
                checkSensitiveAccessConstraint(accessConstraints.get(SENSITIVE, SOCKET_CONFIG_NAME));
            }
        }


        ModelNode application = result.get(CHILDREN, "application-resource", MODEL_DESCRIPTION, "*");
        accessConstraints = application.get(ACCESS_CONSTRAINTS);
        Assert.assertFalse(accessConstraints.hasDefined(SENSITIVE));
        Assert.assertEquals(1, accessConstraints.get(APPLICATION).keys().size());
        checkDeploymentApplicationConstraint(accessConstraints.get(APPLICATION, DEPLOYMENT_NAME));
        checkAttributesLength(constrained, 2);
        attr = getAndCheckDefinedAttribute(application, STANDARD_ATTR.getName());
        Assert.assertFalse(attr.get(ACCESS_CONSTRAINTS).isDefined());
        attr = getAndCheckDefinedAttribute(application, APPLICATION_ATTR.getName());
        Assert.assertFalse(attr.get(ACCESS_CONSTRAINTS, SENSITIVE).isDefined());
        Assert.assertEquals(2, attr.get(ACCESS_CONSTRAINTS, APPLICATION).keys().size());
        checkDeploymentApplicationConstraint(attr.get(ACCESS_CONSTRAINTS, APPLICATION, DEPLOYMENT_NAME));
        checkApplicationConstraint(attr.get(ACCESS_CONSTRAINTS, APPLICATION, APPLICATION_CONSTRAINT.getName()));

        ops = checkOperationsSize(application, 2);
        for (Property currentOp : ops) {
            Assert.assertFalse(currentOp.getValue().get(ACCESS_CONSTRAINTS).isDefined());
        }
    }

    private void checkSocketConfigSensitiveConstraint(ModelNode constraint){
        checkAccessConstraint(constraint, "test", true, true, true);
    }

    private void checkSensitiveAccessConstraint(ModelNode constraint){
        checkAccessConstraint(constraint, "core", false, false, true);
    }


    private void checkAccessConstraint(ModelNode constraint, String type, boolean access, boolean read, boolean write) {
        //System.out.println(constraint);
        Assert.assertEquals(type, constraint.get("type").asString());
    }

    private void checkDeploymentApplicationConstraint(ModelNode constraint) {
        checkApplicationConstraint(constraint, "core", false);
    }

    private void checkApplicationConstraint(ModelNode constraint) {
        checkApplicationConstraint(constraint, "test", true);
    }

    private void checkApplicationConstraint(ModelNode constraint, String type, boolean application) {
        Assert.assertEquals(type, constraint.get("type").asString());
    }


    private List<Property> checkOperationsSize(ModelNode desc, int length) {
        Assert.assertEquals(length, desc.get(OPERATIONS).keys().size());
        return desc.get(OPERATIONS).asPropertyList();
    }

    private void checkAttributesLength(ModelNode desc, int length) {
        Assert.assertEquals(length, desc.get(ATTRIBUTES).keys().size());
    }

    private ModelNode getAndCheckDefinedAttribute(ModelNode desc, String name) {
        ModelNode attr = desc.get(ATTRIBUTES, name);
        Assert.assertTrue(attr.isDefined());
        return attr;
    }

    @Override
    protected AbstractControllerTestBase.ModelControllerService createModelControllerService(ProcessType processType) {
        return new AbstractControllerTestBase.ModelControllerService(processType, new RootResourceDefinition());
    }

    @Override
    protected void initModel(Resource rootResource, ManagementResourceRegistration registration) {
    }

    private static class TestResourceDefinition extends SimpleResourceDefinition {
        TestResourceDefinition(PathElement pathElement) {
            super(pathElement,
                    new NonResolvingResourceDescriptionResolver(),
                    new AbstractAddStepHandler() {},
                    new AbstractRemoveStepHandler() {});
        }
    }

    private static class RootResourceDefinition extends TestResourceDefinition {
        RootResourceDefinition() {
            super(PathElement.pathElement("root"));
        }

        @Override
        public void registerOperations(ManagementResourceRegistration resourceRegistration) {
            super.registerOperations(resourceRegistration);
            GlobalOperationHandlers.registerGlobalOperations(resourceRegistration, ProcessType.EMBEDDED_SERVER);
        }

        @Override
        public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
            super.registerAttributes(resourceRegistration);
        }

        @Override
        public void registerChildren(ManagementResourceRegistration resourceRegistration) {
            resourceRegistration.registerSubModel(new NonConstrainedChildResourceDefinition());
            resourceRegistration.registerSubModel(new ConstrainedChildResourceDefinition());
            resourceRegistration.registerSubModel(new ApplicationChildResourceDefinition());
        }
    }

    private static class ConstrainedChildResourceDefinition extends TestResourceDefinition implements ConstrainedResourceDefinition {
        ConstrainedChildResourceDefinition(){
            super(PathElement.pathElement("constrained-resource"));
        }

        @Override
        public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
            resourceRegistration.registerReadWriteAttribute(STANDARD_ATTR, null, new TestWriteAttributeHandler(STANDARD_ATTR));
            resourceRegistration.registerReadWriteAttribute(SENSITIVE_ATTR, null, new TestWriteAttributeHandler(SENSITIVE_ATTR));
        }

        @Override
        public void registerOperations(ManagementResourceRegistration resourceRegistration) {
            super.registerOperations(resourceRegistration);
            resourceRegistration.registerOperationHandler(CUSTOM_OPERATION, new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    context.stepCompleted();
                }
            });
        }

        @Override
        public List<AccessConstraintDefinition> getAccessConstraints() {
            return Collections.singletonList(SOCKET_CONFIG_SENSITIVE_CONSTRAINT);
        }
    }

    private static class NonConstrainedChildResourceDefinition extends TestResourceDefinition {
        NonConstrainedChildResourceDefinition(){
            super(PathElement.pathElement("nonconstrained-resource"));
        }

        @Override
        public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
            resourceRegistration.registerReadWriteAttribute(STANDARD_ATTR, null, new TestWriteAttributeHandler(STANDARD_ATTR));
        }
    }

    private static class ApplicationChildResourceDefinition extends TestResourceDefinition implements ConstrainedResourceDefinition {
        ApplicationChildResourceDefinition(){
            super(PathElement.pathElement("application-resource"));
        }

        @Override
        public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
            resourceRegistration.registerReadWriteAttribute(STANDARD_ATTR, null, new TestWriteAttributeHandler(STANDARD_ATTR));
            resourceRegistration.registerReadWriteAttribute(APPLICATION_ATTR, null, new TestWriteAttributeHandler(APPLICATION_ATTR));
        }


        @Override
        public List<AccessConstraintDefinition> getAccessConstraints() {
            return Collections.singletonList(DEPLOYMENT_APPLICATION_CONSTRAINT);
        }

    }

    private static class TestWriteAttributeHandler extends AbstractWriteAttributeHandler<Void> {


        public TestWriteAttributeHandler(AttributeDefinition... definitions) {
            super(definitions);
        }

        @Override
        protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                ModelNode resolvedValue, ModelNode currentValue,
                org.jboss.as.controller.AbstractWriteAttributeHandler.HandbackHolder<Void> handbackHolder)
                throws OperationFailedException {
            return false;
        }

        @Override
        protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
        }
    }
}
