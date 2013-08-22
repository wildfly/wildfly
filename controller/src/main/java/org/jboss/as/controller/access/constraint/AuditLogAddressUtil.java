/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.controller.access.constraint;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUDIT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUDIT_LOG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class AuditLogAddressUtil {

    private static final PathAddress STANDALONE_CORE_AUDIT_LOG = PathAddress.pathAddress(
            PathElement.pathElement(CORE_SERVICE, MANAGEMENT),
            PathElement.pathElement(ACCESS, AUDIT));

    private static final PathAddress HOST_AUDIT_LOG = PathAddress.pathAddress(PathElement.pathElement(HOST)).append(STANDALONE_CORE_AUDIT_LOG);

    private static final PathAddress STANDALONE_JMX_AUDIT_LOG = PathAddress.pathAddress(
            PathElement.pathElement(SUBSYSTEM, "jmx"),
            PathElement.pathElement("configuration", AUDIT_LOG));

    private static final PathAddress DOMAIN_AUDIT_LOG = PathAddress.pathAddress(PathElement.pathElement(PROFILE)).append(STANDALONE_JMX_AUDIT_LOG);

    private static final PathAddress[] AUDIT_LOG_ADDRESSES = new PathAddress[] {
                                                                STANDALONE_CORE_AUDIT_LOG,
                                                                HOST_AUDIT_LOG,
                                                                STANDALONE_JMX_AUDIT_LOG,
                                                                DOMAIN_AUDIT_LOG};


    static boolean isAuditLogAddress(PathAddress address) {
        for (PathAddress pattern : AUDIT_LOG_ADDRESSES) {
            if (matches(pattern, address)) {
                return true;
            }
        }
        return false;
    }
    private static boolean matches(PathAddress pattern, PathAddress address) {
        if (address.size() >= pattern.size()) {
            for (int i = 0 ; i < pattern.size() ; i++) {
                if (!matches(pattern.getElement(i), address.getElement(i))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private static boolean matches(PathElement pattern, PathElement element) {
        if (!pattern.getKey().equals(element.getKey())) {
            return false;
        }
        if (pattern.isWildcard()) {
            return true;
        }
        return pattern.getValue().equals(element.getValue());
    }
}
