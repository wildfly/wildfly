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

import java.util.Map;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.naming.NamingMessages;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.naming.subsystem.NamingSubsystemModel.EXTERNAL_CONTEXT;

/**
 * @author Stuart Douglas
 */
class BindingType12RejectChecker extends RejectAttributeChecker.DefaultRejectAttributeChecker implements RejectAttributeChecker {

    @Override
    public boolean rejectOperationParameter(PathAddress address, String attributeName, ModelNode attributeValue, ModelNode operation, TransformationContext context) {
        return rejectCheck(attributeValue, operation);
    }

    private boolean rejectCheck(ModelNode attributeValue, ModelNode model) {
        final String type = attributeValue.asString();
        if (type.equals(EXTERNAL_CONTEXT)) {
            return true;
        }
        return false;
    }

    @Override
    protected boolean rejectAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
        return false;
    }

    @Override
    public boolean rejectResourceAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
        ModelNode model = context.readResource(address).getModel();
        return rejectCheck(attributeValue, model);
    }

    @Override
    public String getRejectionLogMessage(Map<String, ModelNode> attributes) {
        return NamingMessages.MESSAGES.failedToTransformSimpleURLNameBindingAddOperation("1.2.0");
    }
}
