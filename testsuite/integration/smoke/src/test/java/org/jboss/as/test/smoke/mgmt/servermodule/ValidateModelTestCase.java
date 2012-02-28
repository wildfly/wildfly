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
package org.jboss.as.test.smoke.mgmt.servermodule;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.ValidateAddressOperationHandler;
import org.jboss.as.platform.mbean.PlatformMBeanConstants;
import org.jboss.as.remoting.RemotingExtension;
import org.jboss.as.subsystem.test.ModelDescriptionValidator;
import org.jboss.as.subsystem.test.ModelDescriptionValidator.ValidationConfiguration;
import org.jboss.as.subsystem.test.ModelDescriptionValidator.ValidationFailure;
import org.jboss.as.threads.ThreadsExtension;
import org.jboss.dmr.ModelNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xnio.IoUtils;

import static org.jboss.as.arquillian.container.Authentication.getCallbackHandler;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INHERITED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PLATFORM_MBEAN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALIDATE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ValidateModelTestCase {

    @ContainerResource
    private ManagementClient managementClient;

    @Test
    public void testValidateModel() throws Exception {

        ModelNode description = getDescription();

        removeSubsystems(description);

        ValidationConfiguration config = new ValidationConfiguration();

        //TODO should be possible to validate the operations executed...

        //TODO get rid of this to test the deployment section AS7-1832
        description.get(CHILDREN, DEPLOYMENT, MODEL_DESCRIPTION, "*", CHILDREN, "subdeployment", MODEL_DESCRIPTION, "*", CHILDREN).remove("subdeployment");

        System.out.println(description);

        final ModelNode ROOT_ADDR = new ModelNode().setEmptyList();
        //Exclude the operations where one of the reply properties is known to have {type=OBJECT,value-type=UNDEFINED}
        config.allowNullValueTypeForOperationParameter(ROOT_ADDR, VALIDATE_OPERATION, VALUE);
        config.allowNullValueTypeForOperationParameter(ROOT_ADDR, ValidateAddressOperationHandler.OPERATION_NAME, VALUE);

        //Exclude the operations where reply-properties is known to have {type=OBJECT,value-type=UNDEFINED}
        config.allowNullValueTypeForOperationReplyProperties(ROOT_ADDR, READ_OPERATION_DESCRIPTION_OPERATION);
        config.allowNullValueTypeForOperationReplyProperties(ROOT_ADDR, READ_RESOURCE_DESCRIPTION_OPERATION);
        config.allowNullValueTypeForOperationReplyProperties(ROOT_ADDR, READ_RESOURCE_OPERATION);
        config.allowNullValueTypeForOperationReplyProperties(ROOT_ADDR, READ_ATTRIBUTE_OPERATION);
        config.allowNullValueTypeForOperationReplyProperties(createPlatformMBeanAddress(PlatformMBeanConstants.COMPILATION), READ_RESOURCE_OPERATION);
        config.allowNullValueTypeForOperationReplyProperties(createPlatformMBeanAddress(PlatformMBeanConstants.THREADING), READ_RESOURCE_OPERATION);
        config.allowNullValueTypeForOperationReplyProperties(createPlatformMBeanAddress(PlatformMBeanConstants.OPERATING_SYSTEM), READ_RESOURCE_OPERATION);
        config.allowNullValueTypeForOperationReplyProperties(createPlatformMBeanAddress(PlatformMBeanConstants.MEMORY_POOL), READ_RESOURCE_OPERATION);
        config.allowNullValueTypeForOperationReplyProperties(createPlatformMBeanAddress(PlatformMBeanConstants.RUNTIME), READ_RESOURCE_OPERATION);
        final ModelNode MEMORY_POOL_CHILDREN_ADDR = createPlatformMBeanAddress(PlatformMBeanConstants.MEMORY_POOL);
        MEMORY_POOL_CHILDREN_ADDR.add(NAME, "*");
        config.allowNullValueTypeForOperationReplyProperties(MEMORY_POOL_CHILDREN_ADDR, READ_RESOURCE_OPERATION);

        ModelDescriptionValidator validator = new ModelDescriptionValidator(new ModelNode().setEmptyList(), description, config);
        List<ValidationFailure> failures = validator.validateResource();

        if (failures.size() > 0) {
            System.out.println("==== VALIDATION FAILURES: " + failures.size());
            for (ValidationFailure failure : failures) {
                System.out.println(failure);
                System.out.println();
            }
            Assert.fail("The model failed validation");
        }
    }

    private ModelNode createPlatformMBeanAddress(String type) {
        ModelNode addr = new ModelNode();
        addr.add(CORE_SERVICE, PLATFORM_MBEAN);
        addr.add(TYPE, type);
        return addr;
    }

    private void removeSubsystems(ModelNode description) {
        //TODO should remove all subsystems since they are tested in unit tests
        //but for now leave threads and remoting in since unit tests could not be created
        //for them due to circular maven dependencies

        ModelNode subsystemDescriptions = description.get(CHILDREN, SUBSYSTEM, MODEL_DESCRIPTION);
        Set<String> removes = new HashSet<String>();
        for (String key : subsystemDescriptions.keys()) {
            if (!key.equals(RemotingExtension.SUBSYSTEM_NAME) && !key.equals(ThreadsExtension.SUBSYSTEM_NAME)) {
                removes.add(key);
            }
        }

        for (String remove : removes) {
            subsystemDescriptions.remove(remove);
        }
    }

    protected ModelNode getDescription() throws Exception {
        ModelControllerClient client = ModelControllerClient.Factory.create(managementClient.getMgmtAddress(), managementClient.getMgmtPort(), getCallbackHandler());
        try {
            ModelNode op = new ModelNode();
            op.get(OP).set(READ_RESOURCE_DESCRIPTION_OPERATION);
            //op.get(OP_ADDR).setEmptyList();
            op.get(OP_ADDR).setEmptyList();
            op.get(RECURSIVE).set(true);
            op.get(INHERITED).set(false);
            op.get(OPERATIONS).set(true);
            ModelNode result = client.execute(op);
            if (result.hasDefined(FAILURE_DESCRIPTION)) {
                throw new RuntimeException(result.get(FAILURE_DESCRIPTION).asString());
            }
            return result.require(RESULT);

        } finally {
            IoUtils.safeClose(client);
        }
    }
}
