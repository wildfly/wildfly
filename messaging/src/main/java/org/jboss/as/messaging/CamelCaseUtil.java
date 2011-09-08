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

package org.jboss.as.messaging;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Utility for converting camel case based HQ formats to AS standards.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class CamelCaseUtil {

    public static ModelNode convertSecurityRole(final ModelNode camelCase) {
        final ModelNode result = new ModelNode();
        result.setEmptyObject();
        if (camelCase.isDefined()) {
            for (Property prop : camelCase.asPropertyList()) {
                String key = prop.getName();
                if ("createDurableQueue".equals(key)) {
                    key = SecurityRoleAdd.CREATE_DURABLE_QUEUE.getName();
                } else if ("deleteDurableQueue".equals(key)) {
                    key = SecurityRoleAdd.DELETE_DURABLE_QUEUE.getName();
                } else if ("createNonDurableQueue".equals(key)) {
                    key = SecurityRoleAdd.CREATE_NON_DURABLE_QUEUE.getName();
                } else if ("deleteNonDurableQueue".equals(key)) {
                    key = SecurityRoleAdd.DELETE_NON_DURABLE_QUEUE.getName();
                }

                result.get(key).set(prop.getValue());
            }
        }

        return result;
    }

    private CamelCaseUtil() {
    }
}
