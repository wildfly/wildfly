/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.subsystem.test.validation;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_LENGTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_LENGTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Locale;

import junit.framework.Assert;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.validation.subsystem.ValidateSubsystemExtension;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ValidateOperationsTestCase extends AbstractSubsystemTest {

    public ValidateOperationsTestCase() {
        super(ValidateSubsystemExtension.SUBSYSTEM_NAME, new ValidateSubsystemExtension());
    }

    /**
     * Tests that a valid operation passes validation
     */
    @Test
    public void testValidNoArgs() throws Exception {
        getMainExtension().setAddDescriptionProvider(new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode description = new ModelNode();
                description.get(OPERATION_NAME).set(ADD);
                description.get(DESCRIPTION).set("Add operation");
                return description;
            }
        });
        ModelNode operation = createAddOperation();
        KernelServices services = installInController(AdditionalInitialization.MANAGEMENT, Collections.singletonList(operation));

        services.validateOperation(operation);
    }

    /**
     * Tests that a valid operation passes validation
     */
    @Test
    public void testValidArgs() throws Exception {
        getMainExtension().setAddDescriptionProvider(new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode description = new ModelNode();
                description.get(DESCRIPTION).set("Add operation");
                description.get(OPERATION_NAME).set(ADD);
                description.get(REQUEST_PROPERTIES, "test", DESCRIPTION).set("A param");
                description.get(REQUEST_PROPERTIES, "test", TYPE).set(ModelType.LONG);
                description.get(REQUEST_PROPERTIES, "test", REQUIRED).set(true);
                description.get(REQUEST_PROPERTIES, "test", MIN).set(0);
                description.get(REQUEST_PROPERTIES, "test", MAX).set(2);
                return description;
            }
        });
        ModelNode operation = createAddOperation();
        operation.get("test").set(1);
        KernelServices services = installInController(AdditionalInitialization.MANAGEMENT, Collections.singletonList(operation));

        services.validateOperation(operation);
    }

    @Test
    public void testNonExistantParameter() throws Exception {
        getMainExtension().setAddDescriptionProvider(new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode description = new ModelNode();
                description.get(DESCRIPTION).set("Add operation");
                description.get(OPERATION_NAME).set(ADD);
                return description;
            }
        });
        ModelNode operation = createAddOperation();
        operation.get("test").set(1);
        try {
            installInController(AdditionalInitialization.MANAGEMENT, Collections.singletonList(operation));
            Assert.fail("Not valid");
        } catch (Exception expected) {
        }
    }

    @Test
    public void testMissingRequiredParam() throws Exception {
        getMainExtension().setAddDescriptionProvider(new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode description = new ModelNode();
                description.get(DESCRIPTION).set("Add operation");
                description.get(OPERATION_NAME).set(ADD);
                description.get(REQUEST_PROPERTIES, "test", TYPE).set(ModelType.LONG);
                description.get(REQUEST_PROPERTIES, "test", REQUIRED).set(true);
                return description;
            }
        });
        ModelNode operation = createAddOperation();
        try {
            installInController(AdditionalInitialization.MANAGEMENT, Collections.singletonList(operation));
            Assert.fail("Not valid");
        } catch (Exception expected) {
        }
    }

    @Test
    public void testMissingRequiredParam2() throws Exception {
        getMainExtension().setAddDescriptionProvider(new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode description = new ModelNode();
                description.get(DESCRIPTION).set("Add operation");
                description.get(OPERATION_NAME).set(ADD);
                description.get(REQUEST_PROPERTIES, "test", TYPE).set(ModelType.LONG);
                return description;
            }
        });
        ModelNode operation = createAddOperation();
        try {
            installInController(AdditionalInitialization.MANAGEMENT, Collections.singletonList(operation));
            Assert.fail("Not valid");
        } catch (Exception expected) {
        }
    }

    @Test
    public void testMissingNonRequiredParam() throws Exception {
        getMainExtension().setAddDescriptionProvider(new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode description = new ModelNode();
                description.get(DESCRIPTION).set("Add operation");
                description.get(OPERATION_NAME).set(ADD);
                description.get(REQUEST_PROPERTIES, "test", DESCRIPTION).set("A param");
                description.get(REQUEST_PROPERTIES, "test", TYPE).set(ModelType.LONG);
                description.get(REQUEST_PROPERTIES, "test", REQUIRED).set(false);
                return description;
            }
        });
        ModelNode operation = createAddOperation();
        installInController(AdditionalInitialization.MANAGEMENT, Collections.singletonList(operation));
    }

    @Test
    public void testWrongParamType() throws Exception {
        getMainExtension().setAddDescriptionProvider(new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode description = new ModelNode();
                description.get(DESCRIPTION).set("Add operation");
                description.get(OPERATION_NAME).set(ADD);
                description.get(REQUEST_PROPERTIES, "test", TYPE).set(ModelType.LONG);
                return description;
            }
        });
        ModelNode operation = createAddOperation();
        operation.get("test").set("Hello");
        try {
            installInController(AdditionalInitialization.MANAGEMENT, Collections.singletonList(operation));
            Assert.fail("Not valid");
        } catch (Exception expected) {
        }
    }

    @Test
    public void testBigDecimalRangeTooSmall() throws Exception {
        getMainExtension().setAddDescriptionProvider(new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode description = new ModelNode();
                description.get(DESCRIPTION).set("Add operation");
                description.get(OPERATION_NAME).set(ADD);
                description.get(REQUEST_PROPERTIES, "test", TYPE).set(ModelType.BIG_DECIMAL);
                description.get(REQUEST_PROPERTIES, "test", MIN).set(new BigDecimal("10"));
                return description;
            }
        });
        ModelNode operation = createAddOperation();
        operation.get("test").set(new BigDecimal("5"));
        try {
            installInController(AdditionalInitialization.MANAGEMENT, Collections.singletonList(operation));
            Assert.fail("Not valid");
        } catch (Exception expected) {
        }
    }

    @Test
    public void testBigDecimalRangeTooLarge() throws Exception {
        getMainExtension().setAddDescriptionProvider(new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode description = new ModelNode();
                description.get(DESCRIPTION).set("Add operation");
                description.get(OPERATION_NAME).set(ADD);
                description.get(REQUEST_PROPERTIES, "test", TYPE).set(ModelType.BIG_DECIMAL);
                description.get(REQUEST_PROPERTIES, "test", MAX).set(new BigDecimal("10"));
                return description;
            }
        });
        ModelNode operation = createAddOperation();
        operation.get("test").set(new BigDecimal("15"));
        try {
            installInController(AdditionalInitialization.MANAGEMENT, Collections.singletonList(operation));
            Assert.fail("Not valid");
        } catch (Exception expected) {
        }
    }

    @Test
    public void testIntRangeTooSmall() throws Exception {
        getMainExtension().setAddDescriptionProvider(new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode description = new ModelNode();
                description.get(DESCRIPTION).set("Add operation");
                description.get(OPERATION_NAME).set(ADD);
                description.get(REQUEST_PROPERTIES, "test", TYPE).set(ModelType.INT);
                description.get(REQUEST_PROPERTIES, "test", MIN).set(10);
                return description;
            }
        });
        ModelNode operation = createAddOperation();
        operation.get("test").set(5);
        try {
            installInController(AdditionalInitialization.MANAGEMENT, Collections.singletonList(operation));
            Assert.fail("Not valid");
        } catch (Exception expected) {
        }
    }

    @Test
    public void testIntRangeTooLarge() throws Exception {
        getMainExtension().setAddDescriptionProvider(new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode description = new ModelNode();
                description.get(DESCRIPTION).set("Add operation");
                description.get(OPERATION_NAME).set(ADD);
                description.get(REQUEST_PROPERTIES, "test", TYPE).set(ModelType.INT);
                description.get(REQUEST_PROPERTIES, "test", MAX).set(10);
                return description;
            }
        });
        ModelNode operation = createAddOperation();
        operation.get("test").set(15);
        try {
            installInController(AdditionalInitialization.MANAGEMENT, Collections.singletonList(operation));
            Assert.fail("Not valid");
        } catch (Exception expected) {
        }
    }

    @Test
    public void testStringTooShort() throws Exception {
        getMainExtension().setAddDescriptionProvider(new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode description = new ModelNode();
                description.get(DESCRIPTION).set("Add operation");
                description.get(OPERATION_NAME).set(ADD);
                description.get(REQUEST_PROPERTIES, "test", TYPE).set(ModelType.STRING);
                description.get(REQUEST_PROPERTIES, "test", MIN_LENGTH).set(3);
                return description;
            }
        });
        ModelNode operation = createAddOperation();
        operation.get("test").set("Yo");
        try {
            installInController(AdditionalInitialization.MANAGEMENT, Collections.singletonList(operation));
            Assert.fail("Not valid");
        } catch (Exception expected) {
        }
    }

    @Test
    public void testStringTooLong() throws Exception {
        getMainExtension().setAddDescriptionProvider(new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode description = new ModelNode();
                description.get(DESCRIPTION).set("Add operation");
                description.get(OPERATION_NAME).set(ADD);
                description.get(REQUEST_PROPERTIES, "test", TYPE).set(ModelType.STRING);
                description.get(REQUEST_PROPERTIES, "test", MAX_LENGTH).set(1);
                return description;
            }
        });
        ModelNode operation = createAddOperation();
        operation.get("test").set("Yo");
        try {
            installInController(AdditionalInitialization.MANAGEMENT, Collections.singletonList(operation));
            Assert.fail("Not valid");
        } catch (Exception expected) {
        }
    }

    @Test
    public void testStringJustRight() throws Exception {
        getMainExtension().setAddDescriptionProvider(new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode description = new ModelNode();
                description.get(DESCRIPTION).set("Add operation");
                description.get(OPERATION_NAME).set(ADD);
                description.get(REQUEST_PROPERTIES, "test", DESCRIPTION).set("A param");
                description.get(REQUEST_PROPERTIES, "test", TYPE).set(ModelType.STRING);
                description.get(REQUEST_PROPERTIES, "test", MIN_LENGTH).set(2);
                description.get(REQUEST_PROPERTIES, "test", MAX_LENGTH).set(2);
                return description;
            }
        });
        ModelNode operation = createAddOperation();
        operation.get("test").set("Yo");
        installInController(AdditionalInitialization.MANAGEMENT, Collections.singletonList(operation));
    }

    @Test
    public void testBytesTooShort() throws Exception {
        getMainExtension().setAddDescriptionProvider(new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode description = new ModelNode();
                description.get(DESCRIPTION).set("Add operation");
                description.get(OPERATION_NAME).set(ADD);
                description.get(REQUEST_PROPERTIES, "test", TYPE).set(ModelType.BYTES);
                description.get(REQUEST_PROPERTIES, "test", MIN_LENGTH).set(3);
                return description;
            }
        });
        ModelNode operation = createAddOperation();
        operation.get("test").set(new byte[] {1, 2});
        try {
            installInController(AdditionalInitialization.MANAGEMENT, Collections.singletonList(operation));
            Assert.fail("Not valid");
        } catch (Exception expected) {
        }
    }

    @Test
    public void testBytesTooLong() throws Exception {
        getMainExtension().setAddDescriptionProvider(new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode description = new ModelNode();
                description.get(DESCRIPTION).set("Add operation");
                description.get(OPERATION_NAME).set(ADD);
                description.get(REQUEST_PROPERTIES, "test", TYPE).set(ModelType.BYTES);
                description.get(REQUEST_PROPERTIES, "test", MAX_LENGTH).set(1);
                return description;
            }
        });
        ModelNode operation = createAddOperation();
        operation.get("test").set(new byte[] {1, 2});
        try {
            installInController(AdditionalInitialization.MANAGEMENT, Collections.singletonList(operation));
            Assert.fail("Not valid");
        } catch (Exception expected) {
        }
    }

    @Test
    public void testBytesJustRight() throws Exception {
        getMainExtension().setAddDescriptionProvider(new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode description = new ModelNode();
                description.get(DESCRIPTION).set("Add operation");
                description.get(OPERATION_NAME).set(ADD);
                description.get(REQUEST_PROPERTIES, "test", DESCRIPTION).set("A param");
                description.get(REQUEST_PROPERTIES, "test", TYPE).set(ModelType.BYTES);
                description.get(REQUEST_PROPERTIES, "test", MIN_LENGTH).set(2);
                description.get(REQUEST_PROPERTIES, "test", MAX_LENGTH).set(2);
                return description;
            }
        });
        ModelNode operation = createAddOperation();
        operation.get("test").set(new byte[] {1, 2});
        installInController(AdditionalInitialization.MANAGEMENT, Collections.singletonList(operation));
    }

    @Test
    public void testListTooShort() throws Exception {
        getMainExtension().setAddDescriptionProvider(new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode description = new ModelNode();
                description.get(DESCRIPTION).set("Add operation");
                description.get(OPERATION_NAME).set(ADD);
                description.get(REQUEST_PROPERTIES, "test", TYPE).set(ModelType.LIST);
                description.get(REQUEST_PROPERTIES, "test", MIN_LENGTH).set(3);
                return description;
            }
        });
        ModelNode operation = createAddOperation();
        operation.get("test").add("1");
        operation.get("test").add("2");
        try {
            installInController(AdditionalInitialization.MANAGEMENT, Collections.singletonList(operation));
            Assert.fail("Not valid");
        } catch (Exception expected) {
        }
    }

    @Test
    public void testListTooLong() throws Exception {
        getMainExtension().setAddDescriptionProvider(new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode description = new ModelNode();
                description.get(DESCRIPTION).set("Add operation");
                description.get(OPERATION_NAME).set(ADD);
                description.get(REQUEST_PROPERTIES, "test", TYPE).set(ModelType.LIST);
                description.get(REQUEST_PROPERTIES, "test", MAX_LENGTH).set(1);
                return description;
            }
        });
        ModelNode operation = createAddOperation();
        operation.get("test").add("1");
        operation.get("test").add("2");
        try {
            installInController(AdditionalInitialization.MANAGEMENT, Collections.singletonList(operation));
            Assert.fail("Not valid");
        } catch (Exception expected) {
        }
    }

    @Test
    public void testListJustRight() throws Exception {
        getMainExtension().setAddDescriptionProvider(new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode description = new ModelNode();
                description.get(DESCRIPTION).set("Add operation");
                description.get(OPERATION_NAME).set(ADD);
                description.get(REQUEST_PROPERTIES, "test", DESCRIPTION).set("A param");
                description.get(REQUEST_PROPERTIES, "test", TYPE).set(ModelType.LIST);
                description.get(REQUEST_PROPERTIES, "test", VALUE_TYPE).set(ModelType.INT);
                description.get(REQUEST_PROPERTIES, "test", MIN_LENGTH).set(2);
                description.get(REQUEST_PROPERTIES, "test", MAX_LENGTH).set(2);
                return description;
            }
        });
        ModelNode operation = createAddOperation();
        operation.get("test").add("1");
        operation.get("test").add("2");
        installInController(AdditionalInitialization.MANAGEMENT, Collections.singletonList(operation));
    }

    @Test
    public void testListCorrectType() throws Exception {
        getMainExtension().setAddDescriptionProvider(new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode description = new ModelNode();
                description.get(DESCRIPTION).set("Add operation");
                description.get(OPERATION_NAME).set(ADD);
                description.get(REQUEST_PROPERTIES, "test", DESCRIPTION).set("A param");
                description.get(REQUEST_PROPERTIES, "test", TYPE).set(ModelType.LIST);
                description.get(REQUEST_PROPERTIES, "test", VALUE_TYPE).set(ModelType.INT);
                return description;
            }
        });
        ModelNode operation = createAddOperation();
        operation.get("test").add(1);
        operation.get("test").add(2);
        installInController(AdditionalInitialization.MANAGEMENT, Collections.singletonList(operation));
    }

    @Test
    public void testListWrongType() throws Exception {
        getMainExtension().setAddDescriptionProvider(new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode description = new ModelNode();
                description.get(DESCRIPTION).set("Add operation");
                description.get(OPERATION_NAME).set(ADD);
                description.get(REQUEST_PROPERTIES, "test", TYPE).set(ModelType.LIST);
                description.get(REQUEST_PROPERTIES, "test", VALUE_TYPE).set(ModelType.INT);
                return description;
            }
        });
        ModelNode operation = createAddOperation();
        operation.get("test").add(1);
        operation.get("test").add("Hello");
        try {
            installInController(AdditionalInitialization.MANAGEMENT, Collections.singletonList(operation));
            Assert.fail("Not valid");
        } catch (Exception expected) {
        }
    }

    protected ValidateSubsystemExtension getMainExtension() {
        return (ValidateSubsystemExtension)super.getMainExtension();
    }

    private ModelNode createAddOperation() {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).set(PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, ValidateSubsystemExtension.SUBSYSTEM_NAME)).toModelNode());
        return operation;
    }
}
