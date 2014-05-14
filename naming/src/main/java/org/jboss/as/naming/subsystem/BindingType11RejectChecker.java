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

import static org.jboss.as.naming.subsystem.NamingSubsystemModel.ENVIRONMENT;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.OBJECT_FACTORY;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.SIMPLE;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.TYPE;

import java.net.URL;
import java.util.Map;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.naming.logging.NamingLogger;
import org.jboss.dmr.ModelNode;

/**
* @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
*/
class BindingType11RejectChecker extends RejectAttributeChecker.DefaultRejectAttributeChecker implements RejectAttributeChecker {
    private String rejectMessage = null;
    @Override
    public boolean rejectOperationParameter(PathAddress address, String attributeName, ModelNode attributeValue, ModelNode operation, TransformationContext context) {
        return rejectCheck(attributeValue, operation);
    }

    private boolean rejectCheck(ModelNode attributeValue, ModelNode model) {
        final String type = attributeValue.asString();
        if (type.equals(SIMPLE) && model.hasDefined(TYPE)) {
            if (URL.class.getName().equals(model.get(TYPE).asString())) {
                // simple binding with type URL, not supported on 1.1.0
                rejectMessage = NamingLogger.ROOT_LOGGER.failedToTransformSimpleURLNameBindingAddOperation("1.1.0");
                return true;
            }
        } else if (type.equals(OBJECT_FACTORY) && model.hasDefined(ENVIRONMENT)) {
            // object factory bind with environment, not supported on 1.1.0
            rejectMessage = NamingLogger.ROOT_LOGGER.failedToTransformObjectFactoryWithEnvironmentNameBindingAddOperation("1.1.0");
            return true;
        }
        return false;
    }

    @Override
    protected boolean rejectAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
        //will not get called
        return false;
    }

    @Override
    public boolean rejectResourceAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
        ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        return rejectCheck(attributeValue,model);
    }

    @Override
    public String getRejectionLogMessage(Map<String, ModelNode> attributes) {
        return rejectMessage;
    }
}
