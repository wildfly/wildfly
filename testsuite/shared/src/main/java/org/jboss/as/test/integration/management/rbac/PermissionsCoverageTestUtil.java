package org.jboss.as.test.integration.management.rbac;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS_CONTROL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.executeOperation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Ladislav Thon <lthon@redhat.com>
 */
public class PermissionsCoverageTestUtil {
    public static void assertTheEntireDomainTreeHasPermissionsDefined(ModelControllerClient client) throws IOException {
        ModelNode operation = Util.createOperation(READ_RESOURCE_OPERATION, PathAddress.EMPTY_ADDRESS);
        operation.get(RECURSIVE).set(true);
        ModelNode resource = executeOperation(client, operation, Outcome.SUCCESS).get(RESULT);

        operation = Util.createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, PathAddress.EMPTY_ADDRESS);
        operation.get(RECURSIVE).set(true);
        operation.get(ACCESS_CONTROL).set("combined-descriptions");
        ModelNode resourceDescription = executeOperation(client, operation, Outcome.SUCCESS).get(RESULT);

        verify(resource, resourceDescription, DescriptionContainer.CHILDREN, "");
    }

    // "children" alternates with "model-description"
    private static enum DescriptionContainer {
        CHILDREN(ModelDescriptionConstants.CHILDREN, true, "/"),
        MODEL_DESCRIPTION(ModelDescriptionConstants.MODEL_DESCRIPTION, false, "=");

        public final String name;
        public final boolean shouldHaveAccessControlSibling; // "children" must always have a "access-control" sibling
        public final String pathSeparator;

        private DescriptionContainer(String name, boolean shouldHaveAccessControlSibling, String pathSeparator) {
            this.name = name;
            this.shouldHaveAccessControlSibling = shouldHaveAccessControlSibling;
            this.pathSeparator = pathSeparator;
        }

        public DescriptionContainer next() {
            return this == CHILDREN ? MODEL_DESCRIPTION : CHILDREN;
        }

        @Override
        public String toString() {
            return name.toUpperCase();
        }
    }

    private static void verify(ModelNode resource, ModelNode resourceDescription,
                               DescriptionContainer descriptionContainer, String currentPath) {

        if (!resource.isDefined()) {
            return;
        }
        assert resource.getType() == ModelType.OBJECT;

        System.out.println("Verifying " + (currentPath.isEmpty() ? "<root>" : currentPath));

        if (descriptionContainer.shouldHaveAccessControlSibling) {
            assertTrue(resourceDescription.has(ACCESS_CONTROL));
            ModelNode accessControl = resourceDescription.get(ACCESS_CONTROL);

            assertTrue(accessControl.has(DEFAULT));
            ModelNode defaultAccessControl = accessControl.get(DEFAULT);

            assertTrue(defaultAccessControl.has("read"));
            assertEquals(ModelType.BOOLEAN, defaultAccessControl.get("read").getType());
            assertTrue(defaultAccessControl.has("write"));
            assertEquals(ModelType.BOOLEAN, defaultAccessControl.get("write").getType());
        }

        for (String key : resource.keys()) {
            if (resourceDescription.get(ATTRIBUTES).has(key)) {
                // not interesting
            } else if (resourceDescription.get(descriptionContainer.name).has(key)) {
                ModelNode child = resource.get(key);
                ModelNode childDescription = resourceDescription.get(descriptionContainer.name, key);
                verify(child, childDescription, descriptionContainer.next(), currentPath + descriptionContainer.pathSeparator + key);
            } else if (resourceDescription.get(descriptionContainer.name).has("*")) {
                ModelNode child = resource.get(key);
                ModelNode childDescription = resourceDescription.get(descriptionContainer.name, "*");
                verify(child, childDescription, descriptionContainer.next(), currentPath + descriptionContainer.pathSeparator + key);
            } else {
                fail("No description for key " + key + " of " + currentPath);
            }
        }
    }
}
