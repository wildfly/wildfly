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
package org.jboss.as.core.model.test;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_CONTROLLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOCAL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_CLIENT_CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PLATFORM_MBEAN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLOUT_PLAN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLOUT_PLANS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALIDATE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;

import org.jboss.as.controller.operations.common.ValidateAddressOperationHandler;
import org.jboss.as.model.test.ModelTestModelDescriptionValidator.ValidationConfiguration;
import org.jboss.as.platform.mbean.PlatformMBeanConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * A model validation configuration excluding validation for known issues in the model.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class KnownIssuesValidationConfiguration extends ValidationConfiguration {

    public static ValidationConfiguration createAndFixupModel(TestModelType type, ModelNode description) {
        switch (type) {
        case STANDALONE:
            trimSubDeploymentDescription(description);
            return createForStandalone();
        case DOMAIN:
            return createForDomain();
        case HOST:
            replaceLocalDomainControllerType(description);
            return createForHost();
        default:
            throw new IllegalArgumentException("Unknown type");
        }
    }

    private static ValidationConfiguration createForStandalone() {
        ValidationConfiguration config = createWithGlobalOperations();
        config.allowNullValueTypeForOperationReplyProperties(createStandlonePlatformMBeanAddress(PlatformMBeanConstants.COMPILATION), READ_RESOURCE_OPERATION);
        config.allowNullValueTypeForOperationReplyProperties(createStandlonePlatformMBeanAddress(PlatformMBeanConstants.THREADING), READ_RESOURCE_OPERATION);
        config.allowNullValueTypeForOperationReplyProperties(createStandlonePlatformMBeanAddress(PlatformMBeanConstants.OPERATING_SYSTEM), READ_RESOURCE_OPERATION);
        config.allowNullValueTypeForOperationReplyProperties(createStandlonePlatformMBeanAddress(PlatformMBeanConstants.MEMORY_POOL), READ_RESOURCE_OPERATION);
        config.allowNullValueTypeForOperationReplyProperties(createStandlonePlatformMBeanAddress(PlatformMBeanConstants.RUNTIME), READ_RESOURCE_OPERATION);
        final ModelNode MEMORY_POOL_CHILDREN_ADDR = createStandlonePlatformMBeanAddress(PlatformMBeanConstants.MEMORY_POOL);
        MEMORY_POOL_CHILDREN_ADDR.add(NAME, "*");
        config.allowNullValueTypeForOperationReplyProperties(MEMORY_POOL_CHILDREN_ADDR, READ_RESOURCE_OPERATION);

        return config;
    }

    private static ValidationConfiguration createForHost() {
        ValidationConfiguration config = createWithGlobalOperations();
        config.allowNullValueTypeForAttribute(new ModelNode(), DOMAIN_CONTROLLER);
        config.allowNullValueTypeForOperationReplyProperties(createHostPlatformMBeanAddress(PlatformMBeanConstants.COMPILATION), READ_RESOURCE_OPERATION);
        config.allowNullValueTypeForOperationReplyProperties(createHostPlatformMBeanAddress(PlatformMBeanConstants.THREADING), READ_RESOURCE_OPERATION);
        config.allowNullValueTypeForOperationReplyProperties(createHostPlatformMBeanAddress(PlatformMBeanConstants.OPERATING_SYSTEM), READ_RESOURCE_OPERATION);
        config.allowNullValueTypeForOperationReplyProperties(createHostPlatformMBeanAddress(PlatformMBeanConstants.MEMORY_POOL), READ_RESOURCE_OPERATION);
        config.allowNullValueTypeForOperationReplyProperties(createHostPlatformMBeanAddress(PlatformMBeanConstants.RUNTIME), READ_RESOURCE_OPERATION);
        final ModelNode MEMORY_POOL_CHILDREN_ADDR = createHostPlatformMBeanAddress(PlatformMBeanConstants.MEMORY_POOL);
        MEMORY_POOL_CHILDREN_ADDR.add(NAME, "*");
        config.allowNullValueTypeForOperationReplyProperties(MEMORY_POOL_CHILDREN_ADDR, READ_RESOURCE_OPERATION);

        config.allowNullValueTypeForOperationParameter(new ModelNode().add(HOST, "master"), ValidateAddressOperationHandler.OPERATION_NAME, VALUE);

        return config;
    }


    private static ValidationConfiguration createForDomain() {
        ValidationConfiguration config = createWithGlobalOperations();

        ModelNode rolloutPlan = new ModelNode().add(MANAGEMENT_CLIENT_CONTENT, ROLLOUT_PLANS).add(ROLLOUT_PLAN, "*");
        config.allowNullValueTypeForAttribute(rolloutPlan, CONTENT);
        config.allowNullValueTypeForOperationParameter(rolloutPlan, "store", CONTENT);
        config.allowNullValueTypeForOperationParameter(rolloutPlan, ADD, CONTENT);
        return config;
    }

    private static ValidationConfiguration createWithGlobalOperations() {
        ValidationConfiguration config = new ValidationConfiguration();

        //TODO should be possible to validate the operations executed...

        final ModelNode ROOT_ADDR = new ModelNode().setEmptyList();
        //Exclude the operations where one of the reply properties is known to have {type=OBJECT,value-type=UNDEFINED}
        config.allowNullValueTypeForOperationParameter(ROOT_ADDR, VALIDATE_OPERATION, VALUE);
        config.allowNullValueTypeForOperationParameter(ROOT_ADDR, ValidateAddressOperationHandler.OPERATION_NAME, VALUE);

        //Exclude the operations where reply-properties is known to have {type=OBJECT,value-type=UNDEFINED}
        config.allowNullValueTypeForOperationReplyProperties(ROOT_ADDR, READ_OPERATION_DESCRIPTION_OPERATION);
        config.allowNullValueTypeForOperationReplyProperties(ROOT_ADDR, READ_RESOURCE_DESCRIPTION_OPERATION);
        config.allowNullValueTypeForOperationReplyProperties(ROOT_ADDR, READ_RESOURCE_OPERATION);
        config.allowNullValueTypeForOperationReplyProperties(ROOT_ADDR, READ_ATTRIBUTE_OPERATION);
        return config;
    }

    private static ModelNode createStandlonePlatformMBeanAddress(String type) {
        ModelNode addr = new ModelNode();
        addr.add(CORE_SERVICE, PLATFORM_MBEAN);
        addr.add(TYPE, type);
        return addr;
    }

    private static ModelNode createHostPlatformMBeanAddress(String type) {
        ModelNode addr = new ModelNode();
        //TODO see TODO (1) in CoreModelTestDelegate, which trims the model to be validated
        //addr.add(HOST, "master");
        addr.add(CORE_SERVICE, PLATFORM_MBEAN);
        addr.add(TYPE, type);
        return addr;
    }

    public static void trimSubDeploymentDescription(ModelNode description) {
        //TODO get rid of this to test the deployment section AS7-1832
        remove(description, "subsystem", CHILDREN, DEPLOYMENT, MODEL_DESCRIPTION, "*", CHILDREN, "subdeployment", MODEL_DESCRIPTION, "*", CHILDREN);
    }

    public static void replaceLocalDomainControllerType(ModelNode description) {
        /*
         The description looks like, and we don't want the local to be OBJECT for validation so we replace it with STRING
        "domain-controller" => {
            "type" => OBJECT,
            "description" => "Configuration of how the host should interact with the Domain Controller",
            "expressions-allowed" => false,
            "nillable" => false,
            "value-type" => {
                "local" => {
                    "type" => OBJECT,
                    "description" => "Configure a local Domain Controller",
                    "expressions-allowed" => false,
                    "nillable" => true
                },
                "remote" => {
                    "type" => OBJECT,
                    "description" => "Remote Domain Controller connection configuration",
                    "expressions-allowed" => false,
                    "nillable" => true,
                    "value-type" => {"host" => {
                        "type" => INT,
                        "description" => "The address used for the Domain Controller connection",
                        "expressions-allowed" => false,
                        "nillable" => false
                    }}
                }
            },
            "access-type" => "read-only",
            "storage" => "runtime"
        }*/
        ModelNode node = find(description, ATTRIBUTES, DOMAIN_CONTROLLER, VALUE_TYPE, LOCAL, TYPE);
        if (node != null) {
            if (node.getType() != ModelType.TYPE && node.asType() != ModelType.OBJECT) {
                //Validate it here before we do the naughty replacement
                throw new IllegalStateException("Bad local domain controller " + node);
            }
            node.set(ModelType.STRING);
        }
    }

    private static void remove(ModelNode description, String remove, String...parentKeys) {
        ModelNode node = find(description, parentKeys);
        if (node != null) {
            node.remove(remove);
        }
    }

    private static ModelNode find(ModelNode description, String...parentKeys) {
        ModelNode current = description;
        for (String s : parentKeys) {
            if (!current.hasDefined(s)) {
                return null;
            }
            current = current.get(s);
        }
        return current;
    }

}
