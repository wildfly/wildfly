/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.test.integration.management.api;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ANNOTATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FEATURE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PACKAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PARAMS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROVIDES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_FEATURE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REFS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRES;

import java.io.IOException;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test of read-feature-description handling.
 *
 * @author Brian Stansberry
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ReadFeatureDescriptionTestCase extends ContainerResourceMgmtTestBase {

    @Test
    public void testRecursiveReadFeature() throws IOException, MgmtOperationException {
        ModelNode op = Util.createEmptyOperation(READ_FEATURE_DESCRIPTION_OPERATION, PathAddress.EMPTY_ADDRESS);
        op.get(RECURSIVE).set(true);
        ModelNode result = executeForResult(op);
        int maxDepth = validateBaseFeature(result, Integer.MAX_VALUE);
        Assert.assertTrue(result.toString(), maxDepth > 3); // >3 is a good sign we're recursing all the way
    }

    private ModelNode executeForResult(ModelNode op) throws IOException, MgmtOperationException {
        ModelNode result = executeOperation(op);
        Assert.assertTrue(result.isDefined());
        return result;
    }

    private static int validateBaseFeature(ModelNode base, int maxChildDepth) {
        Assert.assertTrue(base.toString(), base.hasDefined(FEATURE));
        Assert.assertEquals(base.toString(), 1, base.asInt());
        return validateFeature(base.get(FEATURE), null, maxChildDepth, 0);
    }

    private static int validateFeature(ModelNode feature, String expectedName, int maxChildDepth, int featureDepth) {
        int highestDepth = featureDepth;
        for (Property prop : feature.asPropertyList()) {
            switch (prop.getName()) {
                case NAME:
                    if (expectedName != null) {
                        Assert.assertEquals(feature.toString(), expectedName, prop.getValue().asString());
                    }
                    break;
                case CHILDREN:
                    if (prop.getValue().isDefined()) {
                        Assert.assertTrue(feature.toString(), maxChildDepth > 0);
                        for (Property child : prop.getValue().asPropertyList()) {
                            int treeDepth = validateFeature(child.getValue(), child.getName(),
                                    +maxChildDepth - 1, featureDepth + 1);
                            highestDepth = Math.max(highestDepth, treeDepth);
                        }
                    }
                    break;
                case ANNOTATION:
                case PARAMS:
                case REFS:
                case PROVIDES:
                case REQUIRES:
                case PACKAGES:
                    // all ok; no other validation right now
                    break;
                default:
                    Assert.fail("Unknown key " + prop.getName() + " in " + feature.toString());
            }
        }
        return highestDepth;
    }
}
