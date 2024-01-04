/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.client.helpers.ClientConstants.NAME;
import static org.wildfly.extension.messaging.activemq.SecurityRoleDefinition.BROWSE;
import static org.wildfly.extension.messaging.activemq.SecurityRoleDefinition.CONSUME;
import static org.wildfly.extension.messaging.activemq.SecurityRoleDefinition.CREATE_ADDRESS;
import static org.wildfly.extension.messaging.activemq.SecurityRoleDefinition.CREATE_DURABLE_QUEUE;
import static org.wildfly.extension.messaging.activemq.SecurityRoleDefinition.CREATE_NON_DURABLE_QUEUE;
import static org.wildfly.extension.messaging.activemq.SecurityRoleDefinition.DELETE_ADDRESS;
import static org.wildfly.extension.messaging.activemq.SecurityRoleDefinition.DELETE_DURABLE_QUEUE;
import static org.wildfly.extension.messaging.activemq.SecurityRoleDefinition.DELETE_NON_DURABLE_QUEUE;
import static org.wildfly.extension.messaging.activemq.SecurityRoleDefinition.MANAGE;
import static org.wildfly.extension.messaging.activemq.SecurityRoleDefinition.SEND;

import org.jboss.as.controller.OperationContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Helper class to report management attributes or operation results
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ManagementUtil {

    public static void reportRolesAsJSON(OperationContext context, String rolesAsJSON) {
        ModelNode camelCase = ModelNode.fromJSONString(rolesAsJSON);
        ModelNode converted = convertSecurityRole(camelCase);
        String json = converted.toJSONString(true);
        context.getResult().set(json);
    }

    public static void reportRoles(OperationContext context, Object[] roles) {
        context.getResult().set(convertRoles(roles));
    }

    public static ModelNode convertRoles(Object[] roles) {
        final ModelNode result = new ModelNode();
        result.setEmptyList();
        if (roles != null && roles.length > 0) {
            for (Object objRole : roles) {
                Object[] role = (Object[])objRole;
                final ModelNode roleNode = result.add();
                roleNode.get(NAME).set(role[0].toString());
                roleNode.get(SEND.getName()).set((Boolean)role[1]);
                roleNode.get(CONSUME.getName()).set((Boolean)role[2]);
                roleNode.get(CREATE_DURABLE_QUEUE.getName()).set((Boolean)role[3]);
                roleNode.get(DELETE_DURABLE_QUEUE.getName()).set((Boolean)role[4]);
                roleNode.get(CREATE_NON_DURABLE_QUEUE.getName()).set((Boolean)role[5]);
                roleNode.get(DELETE_NON_DURABLE_QUEUE.getName()).set((Boolean)role[6]);
                roleNode.get(MANAGE.getName()).set((Boolean)role[7]);
                roleNode.get(BROWSE.getName()).set((Boolean)role[8]);
                roleNode.get(CREATE_ADDRESS.getName()).set((Boolean)role[9]);
                roleNode.get(DELETE_ADDRESS.getName()).set((Boolean)role[10]);
            }
        }
        return result;
    }

    public static void reportListOfStrings(OperationContext context, String[] strings) {
        final ModelNode result = context.getResult();
        result.setEmptyList();
        for (String str : strings) {
            result.add(str);
        }
    }

    private ManagementUtil() {
    }

    /**
     *  Utility for converting camel case based ActiveMQ formats to WildFly standards.
     */
    static ModelNode convertSecurityRole(final ModelNode camelCase) {
        final ModelNode result = new ModelNode();
        result.setEmptyList();
        if (camelCase.isDefined()) {
            for (ModelNode role : camelCase.asList()) {
                final ModelNode roleNode = result.add();
                for (Property prop : role.asPropertyList()) {
                    String key = prop.getName();
                    if (null != key) switch (key) {
                        case "createDurableQueue":
                            key = SecurityRoleDefinition.CREATE_DURABLE_QUEUE.getName();
                            break;
                        case "deleteDurableQueue":
                            key = SecurityRoleDefinition.DELETE_DURABLE_QUEUE.getName();
                            break;
                        case "createNonDurableQueue":
                            key = SecurityRoleDefinition.CREATE_NON_DURABLE_QUEUE.getName();
                            break;
                        case "deleteNonDurableQueue":
                            key = SecurityRoleDefinition.DELETE_NON_DURABLE_QUEUE.getName();
                            break;
                        case "createAddress":
                            key = "create-address";
                            break;
                        case "deleteAddress":
                            key = "delete-address";
                            break;
                        default:
                            break;
                    }
                    roleNode.get(key).set(prop.getValue());
                }
            }
        }

        return result;
    }
}
