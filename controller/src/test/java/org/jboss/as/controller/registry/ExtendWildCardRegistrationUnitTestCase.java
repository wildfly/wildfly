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

package org.jboss.as.controller.registry;

import static org.junit.Assert.*;

import java.util.Locale;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test for AS7-2930 functionality.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ExtendWildCardRegistrationUnitTestCase {

    private static final PathElement parentWild = PathElement.pathElement("parent");
    private static final PathElement parentExt = PathElement.pathElement("parent", "ext");

    private static final PathElement childWild = PathElement.pathElement("child");
    private static final PathElement childExt = PathElement.pathElement("child", "ext");
    private static final PathElement childWildExt = PathElement.pathElement("child", "wild-ext");

    private static final DescriptionProvider rootDP = new TestDescriptionProvider("root");
    private static final DescriptionProvider parentWildDP = new TestDescriptionProvider("parentWild");
    private static final DescriptionProvider parentExtDP = new TestDescriptionProvider("parentExt");
    private static final DescriptionProvider childWildDP = new TestDescriptionProvider("childWild");
    private static final DescriptionProvider childExtDP = new TestDescriptionProvider("childExt");
    private static final DescriptionProvider childWildExtDP = new TestDescriptionProvider("childWildExt");

    private static final OperationStepHandler parentWildAttr = new TestOSH();
    private static final OperationStepHandler parentExtAttr = new TestOSH();
    private static final OperationStepHandler childWildAttr = new TestOSH();
    private static final OperationStepHandler childExtAttr = new TestOSH();
    private static final OperationStepHandler childWildExtAttr = new TestOSH();
    private static final OperationStepHandler parentWildOverrideAttr = new TestOSH();
    private static final OperationStepHandler parentExtOverrideAttr = new TestOSH();
    private static final OperationStepHandler childWildOverrideAttr = new TestOSH();
    private static final OperationStepHandler childExtOverrideAttr = new TestOSH();

    private static final OperationStepHandler parentWildOp = new TestOSH();
    private static final OperationStepHandler parentExtOp = new TestOSH();
    private static final OperationStepHandler childWildOp = new TestOSH();
    private static final OperationStepHandler childExtOp = new TestOSH();
    private static final OperationStepHandler childWildExtOp = new TestOSH();
    private static final OperationStepHandler parentWildOverrideOp = new TestOSH();
    private static final OperationStepHandler parentExtOverrideOp = new TestOSH();
    private static final OperationStepHandler childWildOverrideOp = new TestOSH();
    private static final OperationStepHandler childExtOverrideOp = new TestOSH();

    private static ManagementResourceRegistration registration;
    private static ManagementResourceRegistration parentWildReg;
    private static ManagementResourceRegistration parentExtReg;
    private static ManagementResourceRegistration childWildReg;
    private static ManagementResourceRegistration childWildExtReg;
    private static ManagementResourceRegistration childExtReg;

    @BeforeClass
    public static void setup() {
        registration = ManagementResourceRegistration.Factory.create(rootDP);

        parentWildReg = registration.registerSubModel(parentWild, parentWildDP);
        parentWildReg.registerReadOnlyAttribute("wildAttr", parentWildAttr, AttributeAccess.Storage.CONFIGURATION);
        parentWildReg.registerOperationHandler("wildOp", parentWildOp, new TestDescriptionProvider("wildOp"));
        parentWildReg.registerReadOnlyAttribute("overrideAttr", parentWildOverrideAttr, AttributeAccess.Storage.CONFIGURATION);
        parentWildReg.registerOperationHandler("overrideOp", parentWildOverrideOp, new TestDescriptionProvider("overrideOp"));

        parentExtReg = registration.registerSubModel(parentExt, parentExtDP);
        parentExtReg.registerReadOnlyAttribute("extAttr", parentExtAttr, AttributeAccess.Storage.CONFIGURATION);
        parentExtReg.registerOperationHandler("extOp", parentExtOp, new TestDescriptionProvider("extOp"));
        parentExtReg.registerReadOnlyAttribute("overrideAttr", parentExtOverrideAttr, AttributeAccess.Storage.CONFIGURATION);
        parentExtReg.registerOperationHandler("overrideOp", parentExtOverrideOp, new TestDescriptionProvider("overrideOp"));

        childWildReg = parentWildReg.registerSubModel(childWild, childWildDP);
        childWildReg.registerReadOnlyAttribute("wildAttr", childWildAttr, AttributeAccess.Storage.CONFIGURATION);
        childWildReg.registerOperationHandler("wildOp", childWildOp, new TestDescriptionProvider("wildOp"));
        childWildReg.registerReadOnlyAttribute("overrideAttr", childWildOverrideAttr, AttributeAccess.Storage.CONFIGURATION);
        childWildReg.registerOperationHandler("overrideOp", childWildOverrideOp, new TestDescriptionProvider("overrideOp"));

        childWildExtReg = parentWildReg.registerSubModel(childWildExt, childWildExtDP);
        childWildExtReg.registerReadOnlyAttribute("wildExtAttr", childWildExtAttr, AttributeAccess.Storage.CONFIGURATION);
        childWildExtReg.registerOperationHandler("wildExtOp", childWildExtOp, new TestDescriptionProvider("wildExtOp"));

        childExtReg = parentExtReg.registerSubModel(childExt, childExtDP);
        childExtReg.registerReadOnlyAttribute("extAttr", childExtAttr, AttributeAccess.Storage.CONFIGURATION);
        childExtReg.registerOperationHandler("extOp", childExtOp, new TestDescriptionProvider("extOp"));
        childExtReg.registerReadOnlyAttribute("overrideAttr", childExtOverrideAttr, AttributeAccess.Storage.CONFIGURATION);
        childExtReg.registerOperationHandler("overrideOp", childExtOverrideOp, new TestDescriptionProvider("overrideOp"));
    }

    @AfterClass
    public static void tearDown() {
        registration = null;
    }

    @Test
    public void testParentDescriptionProvider() throws Exception {
        assertSame(parentWildDP, registration.getModelDescription(PathAddress.pathAddress(parentWild)));
        assertSame(parentExtDP, registration.getModelDescription(PathAddress.pathAddress(parentExt)));
        assertSame(parentWildDP, parentWildReg.getModelDescription(PathAddress.EMPTY_ADDRESS));
        assertSame(parentExtDP, parentExtReg.getModelDescription(PathAddress.EMPTY_ADDRESS));
    }

    @Test
    public void testParentWildcardAttribute() throws Exception {
        assertSame(parentWildAttr, registration.getAttributeAccess(PathAddress.pathAddress(parentWild), "wildAttr").getReadHandler());
        assertSame(parentWildAttr, registration.getAttributeAccess(PathAddress.pathAddress(parentExt), "wildAttr").getReadHandler());
        assertSame(parentWildAttr, parentWildReg.getAttributeAccess(PathAddress.EMPTY_ADDRESS, "wildAttr").getReadHandler());
        assertSame(parentWildAttr, parentExtReg.getAttributeAccess(PathAddress.EMPTY_ADDRESS, "wildAttr").getReadHandler());
    }

    @Test
    public void testParentExtensionAttribute() throws Exception {
        assertNull(registration.getAttributeAccess(PathAddress.pathAddress(parentWild), "extAttr"));
        assertSame(parentExtAttr, registration.getAttributeAccess(PathAddress.pathAddress(parentExt), "extAttr").getReadHandler());
        assertNull(parentWildReg.getAttributeAccess(PathAddress.EMPTY_ADDRESS, "extAttr"));
        assertSame(parentExtAttr, parentExtReg.getAttributeAccess(PathAddress.EMPTY_ADDRESS, "extAttr").getReadHandler());
    }

    @Test
    public void testParentOverrideAttribute() throws Exception {
        assertSame(parentWildOverrideAttr, registration.getAttributeAccess(PathAddress.pathAddress(parentWild), "overrideAttr").getReadHandler());
        assertSame(parentExtOverrideAttr, registration.getAttributeAccess(PathAddress.pathAddress(parentExt), "overrideAttr").getReadHandler());
        assertSame(parentWildOverrideAttr, parentWildReg.getAttributeAccess(PathAddress.EMPTY_ADDRESS, "overrideAttr").getReadHandler());
        assertSame(parentExtOverrideAttr, parentExtReg.getAttributeAccess(PathAddress.EMPTY_ADDRESS, "overrideAttr").getReadHandler());
    }

    @Test
    public void testParentMissingAttribute() throws Exception {
        assertNull(registration.getAttributeAccess(PathAddress.pathAddress(parentWild), "na"));
        assertNull(registration.getAttributeAccess(PathAddress.pathAddress(parentExt), "na"));
        assertNull(parentWildReg.getAttributeAccess(PathAddress.EMPTY_ADDRESS, "na"));
        assertNull(parentExtReg.getAttributeAccess(PathAddress.EMPTY_ADDRESS, "na"));

    }

    @Test
    public void testParentWildcardOperation() throws Exception {
        assertSame(parentWildOp, registration.getOperationHandler(PathAddress.pathAddress(parentWild), "wildOp"));
        assertSame(parentWildOp, registration.getOperationHandler(PathAddress.pathAddress(parentExt), "wildOp"));
        assertSame(parentWildOp, parentWildReg.getOperationHandler(PathAddress.EMPTY_ADDRESS, "wildOp"));
        assertSame(parentWildOp, parentExtReg.getOperationHandler(PathAddress.EMPTY_ADDRESS, "wildOp"));
    }

    @Test
    public void testParentExtensionOperation() throws Exception {
        assertNull(registration.getOperationHandler(PathAddress.pathAddress(parentWild), "extOp"));
        assertSame(parentExtOp, registration.getOperationHandler(PathAddress.pathAddress(parentExt), "extOp"));
        assertNull(parentWildReg.getOperationHandler(PathAddress.EMPTY_ADDRESS, "extOp"));
        assertSame(parentExtOp, parentExtReg.getOperationHandler(PathAddress.EMPTY_ADDRESS, "extOp"));
    }

    @Test
    public void testParentOverrideOperation() throws Exception {
        assertSame(parentWildOverrideOp, registration.getOperationHandler(PathAddress.pathAddress(parentWild), "overrideOp"));
        assertSame(parentExtOverrideOp, registration.getOperationHandler(PathAddress.pathAddress(parentExt), "overrideOp"));
        assertSame(parentWildOverrideOp, parentWildReg.getOperationHandler(PathAddress.EMPTY_ADDRESS, "overrideOp"));
        assertSame(parentExtOverrideOp, parentExtReg.getOperationHandler(PathAddress.EMPTY_ADDRESS, "overrideOp"));
    }

    @Test
    public void testParentMissingOperation() throws Exception {
        assertNull(registration.getOperationHandler(PathAddress.pathAddress(parentWild), "na"));
        assertNull(registration.getOperationHandler(PathAddress.pathAddress(parentExt), "na"));
        assertNull(parentWildReg.getOperationHandler(PathAddress.EMPTY_ADDRESS, "na"));
        assertNull(parentExtReg.getOperationHandler(PathAddress.EMPTY_ADDRESS, "na"));
    }

    @Test
    public void testChildDescriptionProvider() throws Exception {
        assertSame(childWildDP, registration.getModelDescription(PathAddress.pathAddress(parentWild, childWild)));
        assertSame(childWildDP, registration.getModelDescription(PathAddress.pathAddress(parentExt, childWild)));
        // This one is a bit odd
        assertSame(childWildDP, registration.getModelDescription(PathAddress.pathAddress(parentWild, childExt)));
        assertSame(childExtDP, registration.getModelDescription(PathAddress.pathAddress(parentExt, childExt)));

        assertSame(childWildDP, parentWildReg.getModelDescription(PathAddress.pathAddress(childWild)));
        assertSame(childWildDP, parentExtReg.getModelDescription(PathAddress.pathAddress(childWild)));
        // This one is a bit odd
        assertSame(childWildDP, parentWildReg.getModelDescription(PathAddress.pathAddress(childExt)));
        assertSame(childExtDP, parentExtReg.getModelDescription(PathAddress.pathAddress(childExt)));

        assertSame(childWildDP, childWildReg.getModelDescription(PathAddress.EMPTY_ADDRESS));
        assertSame(childExtDP, childExtReg.getModelDescription(PathAddress.EMPTY_ADDRESS));
    }

    @Test
    public void testChildWildcardAttribute() throws Exception {
        assertSame(childWildAttr, registration.getAttributeAccess(PathAddress.pathAddress(parentWild, childWild), "wildAttr").getReadHandler());
        assertSame(childWildAttr, registration.getAttributeAccess(PathAddress.pathAddress(parentWild, childExt), "wildAttr").getReadHandler());
        assertSame(childWildAttr, registration.getAttributeAccess(PathAddress.pathAddress(parentWild, childWildExt), "wildAttr").getReadHandler());
        assertSame(childWildAttr, registration.getAttributeAccess(PathAddress.pathAddress(parentExt, childWild), "wildAttr").getReadHandler());
        assertSame(childWildAttr, registration.getAttributeAccess(PathAddress.pathAddress(parentExt, childExt), "wildAttr").getReadHandler());
        assertSame(childWildAttr, registration.getAttributeAccess(PathAddress.pathAddress(parentExt, childWildExt), "wildAttr").getReadHandler());

        assertSame(childWildAttr, parentWildReg.getAttributeAccess(PathAddress.pathAddress(childWild), "wildAttr").getReadHandler());
        assertSame(childWildAttr, parentExtReg.getAttributeAccess(PathAddress.pathAddress(childWild), "wildAttr").getReadHandler());
        assertSame(childWildAttr, parentWildReg.getAttributeAccess(PathAddress.pathAddress(childExt), "wildAttr").getReadHandler());
        assertSame(childWildAttr, parentExtReg.getAttributeAccess(PathAddress.pathAddress(childExt), "wildAttr").getReadHandler());
        assertSame(childWildAttr, parentWildReg.getAttributeAccess(PathAddress.pathAddress(childWildExt), "wildAttr").getReadHandler());
        assertSame(childWildAttr, parentExtReg.getAttributeAccess(PathAddress.pathAddress(childWildExt), "wildAttr").getReadHandler());

        assertSame(childWildAttr, childWildReg.getAttributeAccess(PathAddress.EMPTY_ADDRESS, "wildAttr").getReadHandler());
        assertSame(childWildAttr, childExtReg.getAttributeAccess(PathAddress.EMPTY_ADDRESS, "wildAttr").getReadHandler());
        assertSame(childWildAttr, childWildExtReg.getAttributeAccess(PathAddress.EMPTY_ADDRESS, "wildAttr").getReadHandler());
    }

    @Test
    public void testChildExtensionAttribute() throws Exception {
        assertNull(registration.getAttributeAccess(PathAddress.pathAddress(parentWild, childWild), "extAttr"));
        assertNull(registration.getAttributeAccess(PathAddress.pathAddress(parentExt, childWild), "extAttr"));
        assertNull(registration.getAttributeAccess(PathAddress.pathAddress(parentWild, childExt), "extAttr"));
        assertNull(registration.getAttributeAccess(PathAddress.pathAddress(parentWild, childWildExt), "extAttr"));
        assertSame(childExtAttr, registration.getAttributeAccess(PathAddress.pathAddress(parentExt, childExt), "extAttr").getReadHandler());

        assertNull(parentWildReg.getAttributeAccess(PathAddress.pathAddress(childWild), "extAttr"));
        assertNull(parentExtReg.getAttributeAccess(PathAddress.pathAddress(childWild), "extAttr"));
        assertNull(parentWildReg.getAttributeAccess(PathAddress.pathAddress(childExt), "extAttr"));
        assertNull(parentWildReg.getAttributeAccess(PathAddress.pathAddress(childWildExt), "extAttr"));
        assertSame(childExtAttr, parentExtReg.getAttributeAccess(PathAddress.pathAddress(childExt), "extAttr").getReadHandler());

        assertNull(childWildReg.getAttributeAccess(PathAddress.EMPTY_ADDRESS, "extAttr"));
        assertNull(childWildExtReg.getAttributeAccess(PathAddress.EMPTY_ADDRESS, "extAttr"));
        assertSame(childExtAttr, childExtReg.getAttributeAccess(PathAddress.EMPTY_ADDRESS, "extAttr").getReadHandler());
    }

    @Test
    public void testChildOverrideAttribute() throws Exception {
        assertSame(childWildOverrideAttr, registration.getAttributeAccess(PathAddress.pathAddress(parentWild, childWild), "overrideAttr").getReadHandler());
        assertSame(childWildOverrideAttr, registration.getAttributeAccess(PathAddress.pathAddress(parentExt, childWild), "overrideAttr").getReadHandler());
        assertSame(childWildOverrideAttr, registration.getAttributeAccess(PathAddress.pathAddress(parentWild, childWildExt), "overrideAttr").getReadHandler());
        assertSame(childWildOverrideAttr, registration.getAttributeAccess(PathAddress.pathAddress(parentExt, childWildExt), "overrideAttr").getReadHandler());
        assertSame(childWildOverrideAttr, registration.getAttributeAccess(PathAddress.pathAddress(parentWild, childExt), "overrideAttr").getReadHandler());
        assertSame(childExtOverrideAttr, registration.getAttributeAccess(PathAddress.pathAddress(parentExt, childExt), "overrideAttr").getReadHandler());

        assertSame(childWildOverrideAttr, parentWildReg.getAttributeAccess(PathAddress.pathAddress(childWild), "overrideAttr").getReadHandler());
        assertSame(childWildOverrideAttr, parentExtReg.getAttributeAccess(PathAddress.pathAddress(childWild), "overrideAttr").getReadHandler());
        assertSame(childWildOverrideAttr, parentWildReg.getAttributeAccess(PathAddress.pathAddress(childWildExt), "overrideAttr").getReadHandler());
        assertSame(childWildOverrideAttr, parentExtReg.getAttributeAccess(PathAddress.pathAddress(childWildExt), "overrideAttr").getReadHandler());
        assertSame(childWildOverrideAttr, parentWildReg.getAttributeAccess(PathAddress.pathAddress(childExt), "overrideAttr").getReadHandler());
        assertSame(childExtOverrideAttr, parentExtReg.getAttributeAccess(PathAddress.pathAddress(childExt), "overrideAttr").getReadHandler());

        assertSame(childWildOverrideAttr, childWildReg.getAttributeAccess(PathAddress.EMPTY_ADDRESS, "overrideAttr").getReadHandler());
        assertSame(childWildOverrideAttr, childWildExtReg.getAttributeAccess(PathAddress.EMPTY_ADDRESS, "overrideAttr").getReadHandler());
        assertSame(childExtOverrideAttr, childExtReg.getAttributeAccess(PathAddress.EMPTY_ADDRESS, "overrideAttr").getReadHandler());
    }

    @Test
    public void testChildMissingAttribute() throws Exception {
        assertNull(registration.getAttributeAccess(PathAddress.pathAddress(parentWild, childWild), "na"));
        assertNull(registration.getAttributeAccess(PathAddress.pathAddress(parentExt, childWild), "na"));
        assertNull(registration.getAttributeAccess(PathAddress.pathAddress(parentWild, childExt), "na"));
        assertNull(registration.getAttributeAccess(PathAddress.pathAddress(parentExt, childExt), "na"));

        assertNull(parentWildReg.getAttributeAccess(PathAddress.pathAddress(childWild), "na"));
        assertNull(parentExtReg.getAttributeAccess(PathAddress.pathAddress(childWild), "na"));
        assertNull(parentWildReg.getAttributeAccess(PathAddress.pathAddress(childExt), "na"));
        assertNull(parentExtReg.getAttributeAccess(PathAddress.pathAddress(childExt), "na"));

        assertNull(childWildReg.getAttributeAccess(PathAddress.EMPTY_ADDRESS, "na"));
        assertNull(childExtReg.getAttributeAccess(PathAddress.EMPTY_ADDRESS, "na"));
    }

    @Test
    public void testChildWildcardOperation() throws Exception {
        assertSame(childWildOp, registration.getOperationHandler(PathAddress.pathAddress(parentWild, childWild), "wildOp"));
        assertSame(childWildOp, registration.getOperationHandler(PathAddress.pathAddress(parentExt, childWild), "wildOp"));
        assertSame(childWildOp, registration.getOperationHandler(PathAddress.pathAddress(parentWild, childExt), "wildOp"));
        assertSame(childWildOp, registration.getOperationHandler(PathAddress.pathAddress(parentExt, childExt), "wildOp"));

        assertSame(childWildOp, parentWildReg.getOperationHandler(PathAddress.pathAddress(childWild), "wildOp"));
        assertSame(childWildOp, parentExtReg.getOperationHandler(PathAddress.pathAddress(childWild), "wildOp"));
        assertSame(childWildOp, parentWildReg.getOperationHandler(PathAddress.pathAddress(childExt), "wildOp"));
        assertSame(childWildOp, parentExtReg.getOperationHandler(PathAddress.pathAddress(childExt), "wildOp"));

        assertSame(childWildOp, childWildReg.getOperationHandler(PathAddress.EMPTY_ADDRESS, "wildOp"));
        assertSame(childWildOp, childExtReg.getOperationHandler(PathAddress.EMPTY_ADDRESS, "wildOp"));
    }

    @Test
    public void testChildExtensionOperation() throws Exception {
        assertNull(registration.getOperationHandler(PathAddress.pathAddress(parentWild, childWild), "extOp"));
        assertNull(registration.getOperationHandler(PathAddress.pathAddress(parentExt, childWild), "extOp"));
        assertNull(registration.getOperationHandler(PathAddress.pathAddress(parentWild, childExt), "extOp"));
        assertSame(childExtOp, registration.getOperationHandler(PathAddress.pathAddress(parentExt, childExt), "extOp"));

        assertNull(parentWildReg.getOperationHandler(PathAddress.pathAddress(childWild), "extOp"));
        assertNull(parentExtReg.getOperationHandler(PathAddress.pathAddress(childWild), "extOp"));
        assertNull(parentWildReg.getOperationHandler(PathAddress.pathAddress(childExt), "extOp"));
        assertSame(childExtOp, parentExtReg.getOperationHandler(PathAddress.pathAddress(childExt), "extOp"));

        assertNull(childWildReg.getOperationHandler(PathAddress.EMPTY_ADDRESS, "extOp"));
        assertSame(childExtOp, childExtReg.getOperationHandler(PathAddress.EMPTY_ADDRESS, "extOp"));
    }

    @Test
    public void testChildOverrideOperation() throws Exception {
        assertSame(childWildOverrideOp, registration.getOperationHandler(PathAddress.pathAddress(parentWild, childWild), "overrideOp"));
        assertSame(childWildOverrideOp, registration.getOperationHandler(PathAddress.pathAddress(parentExt, childWild), "overrideOp"));
        assertSame(childWildOverrideOp, registration.getOperationHandler(PathAddress.pathAddress(parentWild, childExt), "overrideOp"));
        assertSame(childExtOverrideOp, registration.getOperationHandler(PathAddress.pathAddress(parentExt, childExt), "overrideOp"));

        assertSame(childWildOverrideOp, parentWildReg.getOperationHandler(PathAddress.pathAddress(childWild), "overrideOp"));
        assertSame(childWildOverrideOp, parentExtReg.getOperationHandler(PathAddress.pathAddress(childWild), "overrideOp"));
        assertSame(childWildOverrideOp, parentWildReg.getOperationHandler(PathAddress.pathAddress(childExt), "overrideOp"));
        assertSame(childExtOverrideOp, parentExtReg.getOperationHandler(PathAddress.pathAddress(childExt), "overrideOp"));

        assertSame(childWildOverrideOp, childWildReg.getOperationHandler(PathAddress.EMPTY_ADDRESS, "overrideOp"));
        assertSame(childExtOverrideOp, childExtReg.getOperationHandler(PathAddress.EMPTY_ADDRESS, "overrideOp"));

    }

    @Test
    public void testChildMissingOperation() throws Exception {
        assertNull(registration.getOperationHandler(PathAddress.pathAddress(parentWild, childWild), "na"));
        assertNull(registration.getOperationHandler(PathAddress.pathAddress(parentExt, childWild), "na"));
        assertNull(registration.getOperationHandler(PathAddress.pathAddress(parentWild, childExt), "na"));
        assertNull(registration.getOperationHandler(PathAddress.pathAddress(parentExt, childExt), "na"));

        assertNull(parentWildReg.getOperationHandler(PathAddress.pathAddress(childWild), "na"));
        assertNull(parentExtReg.getOperationHandler(PathAddress.pathAddress(childWild), "na"));
        assertNull(parentWildReg.getOperationHandler(PathAddress.pathAddress(childExt), "na"));
        assertNull(parentExtReg.getOperationHandler(PathAddress.pathAddress(childExt), "na"));

        assertNull(childWildReg.getOperationHandler(PathAddress.EMPTY_ADDRESS, "na"));
        assertNull(childExtReg.getOperationHandler(PathAddress.EMPTY_ADDRESS, "na"));
    }

    @Test
    public void testDuplicateSubModel() {
        try {
            parentExtReg.registerSubModel(childWildExt, new TestDescriptionProvider("blah"));
            fail("Duplicate child not rejected");
        } catch (Exception good) {}

        try {
            parentExtReg.registerSubModel(childWild, new TestDescriptionProvider("blah"));
            fail("Duplicate child not rejected");
        } catch (Exception good) {}

        try {
            parentWildReg.registerSubModel(childWild, new TestDescriptionProvider("blah"));
            fail("Duplicate child not rejected");
        } catch (Exception good) {}

    }

    private static class TestDescriptionProvider implements DescriptionProvider {
        private final String desc;

        public TestDescriptionProvider(String desc) {
            this.desc = desc;
        }

        @Override
        public ModelNode getModelDescription(Locale locale) {
            return new ModelNode(desc);
        }
    }

    private static class TestOSH implements OperationStepHandler {

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            context.completeStep();
        }
    }

}
