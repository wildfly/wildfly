/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.jboss.as.naming.subsystem;

import static org.jboss.as.naming.subsystem.NamingSubsystemModel.EXTERNAL_CONTEXT;

import java.util.Map;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.naming.logging.NamingLogger;
import org.jboss.dmr.ModelNode;

/**
 * @author Stuart Douglas
 */
class BindingType12RejectChecker extends RejectAttributeChecker.DefaultRejectAttributeChecker implements RejectAttributeChecker {

    @Override
    protected boolean rejectAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
        final String type = attributeValue.asString();
        if (type.equals(EXTERNAL_CONTEXT)) {
            return true;
        }
        return false;
    }

    @Override
    public String getRejectionLogMessage(Map<String, ModelNode> attributes) {
        return NamingLogger.ROOT_LOGGER.failedToTransformExternalContext("1.2.0");
    }
}
