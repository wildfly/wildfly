/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.controller;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_DEFAULTS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;

/**
 * Set of utility methods for testing properties transformations for EAP 6.x versions.
 *
 * @author Kabir Khan
 * @author Radoslav Husar
 * @version October 2015
 */
public class PropertiesTestUtil {

    private PropertiesTestUtil() {
        // Hide utility class
    }

    public static void checkMapResults(KernelServices services, ModelNode expected, ModelVersion version, ModelNode operation) throws Exception {
        ModelNode main = ModelTestUtils.checkOutcome(services.executeOperation(operation.clone())).get(ModelDescriptionConstants.RESULT);
        ModelNode legacyResult = services.executeOperation(version, services.transformOperation(version, operation.clone()));
        ModelNode legacy;
        if (expected.isDefined()) {
            legacy = ModelTestUtils.checkOutcome(legacyResult).get(ModelDescriptionConstants.RESULT);
        } else {
            ModelTestUtils.checkFailed(legacyResult);
            legacy = new ModelNode();
        }
        Assert.assertEquals(main, legacy);
        Assert.assertEquals(expected, legacy);
    }

    public static void checkMainMapModel(ModelNode model, String... properties) {
        Assert.assertEquals(0, properties.length % 2);

        ModelNode props = model.get("properties");
        Assert.assertEquals(properties.length / 2, props.isDefined() ? props.keys().size() : 0);
        for (int i = 0; i < properties.length; i += 2) {
            Assert.assertEquals(properties[i + 1], props.get(properties[i]).asString());
        }
    }

    public static void checkLegacyChildResourceModel(ModelNode model, String... properties) {
        Assert.assertEquals(0, properties.length % 2);

        ModelNode props = model.get("property");
        Assert.assertEquals(properties.length / 2, props.isDefined() ? props.keys().size() : 0);
        for (int i = 0; i < properties.length; i += 2) {
            ModelNode property = props.get(properties[i]);
            Assert.assertTrue(property.isDefined());
            Assert.assertEquals(1, property.keys().size());
            Assert.assertEquals(properties[i + 1], property.get("value").asString());
        }
    }

    public static void checkMapModels(KernelServices services, ModelVersion version, PathAddress address, String... properties) throws Exception {
        final ModelNode readResource = Util.createEmptyOperation(READ_RESOURCE_OPERATION, address);
        readResource.get(RECURSIVE).set(true);
        readResource.get(INCLUDE_DEFAULTS).set(false);
        ModelNode mainModel = services.executeForResult(readResource.clone());
        checkMainMapModel(mainModel, properties);

        final ModelNode legacyModel;
        if (address.getLastElement().getKey().equals("transport")) {
            //TODO get rid of this once the PathAddress transformer works properly
            //Temporary workaround
            readResource.get(OP_ADDR).set(address.subAddress(0, address.size() - 1).append("transport", "TRANSPORT").toModelNode());
            legacyModel = services.getLegacyServices(version).executeForResult(readResource);
        } else {
            legacyModel = ModelTestUtils.checkResultAndGetContents(services.executeOperation(version, services.transformOperation(version, readResource.clone())));
        }

        checkLegacyChildResourceModel(legacyModel, properties);
    }

    public static ModelNode success() {
        final ModelNode result = new ModelNode();
        result.get(OUTCOME).set(SUCCESS);
        result.get(RESULT);
        return result;
    }

    /**
     * Executes a given operation asserting that an attachment has been created. Given {@link KernelServices} must have enabled attachment grabber.
     *
     * @return {@link ModelNode} result of the transformed operation
     */
    public static ModelNode executeOpInBothControllersWithAttachments(KernelServices services, ModelVersion version, ModelNode operation) throws Exception {
        OperationTransformer.TransformedOperation op = services.executeInMainAndGetTheTransformedOperation(operation, version);
        Assert.assertFalse(op.rejectOperation(success()));
//        System.out.println(operation + "\nbecomes\n" + op.getTransformedOperation());
        if (op.getTransformedOperation() != null) {
            return ModelTestUtils.checkOutcome(services.getLegacyServices(version).executeOperation(op.getTransformedOperation()));
        }
        return null;
    }

}
