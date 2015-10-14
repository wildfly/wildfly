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

package org.wildfly.extension.messaging.activemq;

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

    public static void reportRoles(OperationContext context, String rolesAsJSON) {
        ModelNode camelCase = ModelNode.fromJSONString(rolesAsJSON);
        ModelNode converted = convertSecurityRole(camelCase);
        context.getResult().set(converted);
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
                    if ("createDurableQueue".equals(key)) {
                        key = SecurityRoleDefinition.CREATE_DURABLE_QUEUE.getName();
                    } else if ("deleteDurableQueue".equals(key)) {
                        key = SecurityRoleDefinition.DELETE_DURABLE_QUEUE.getName();
                    } else if ("createNonDurableQueue".equals(key)) {
                        key = SecurityRoleDefinition.CREATE_NON_DURABLE_QUEUE.getName();
                    } else if ("deleteNonDurableQueue".equals(key)) {
                        key = SecurityRoleDefinition.DELETE_NON_DURABLE_QUEUE.getName();
                    }

                    roleNode.get(key).set(prop.getValue());
                }
            }
        }

        return result;
    }
}
