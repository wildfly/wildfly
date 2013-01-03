/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jmx;

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Thomas.Diesler@jboss.com
 * @author Emanuel Muckenhuber
 */
class JMXSubsystemAdd extends AbstractAddStepHandler {

    static final JMXSubsystemAdd INSTANCE = new JMXSubsystemAdd();

    private JMXSubsystemAdd() {
        //
    }

    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        model.setEmptyObject();
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        launchServices(context, model, verificationHandler, newControllers);
    }

    void launchServices(OperationContext context, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        ModelNode recursiveModel = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));
        // Add the MBean service
        String resolvedDomain = getDomainName(context, recursiveModel, CommonAttributes.RESOLVED);
        String expressionsDomain = getDomainName(context, recursiveModel, CommonAttributes.EXPRESSION);
        boolean legacyWithProperPropertyFormat = false;
        if (model.hasDefined(CommonAttributes.PROPER_PROPERTY_FORMAT)) {
            legacyWithProperPropertyFormat = ExposeModelResourceExpression.DOMAIN_NAME.resolveModelAttribute(context, recursiveModel).asBoolean();
        }

        ServiceController<?> controller = verificationHandler != null ?
                MBeanServerService.addService(context.getServiceTarget(), resolvedDomain, expressionsDomain, legacyWithProperPropertyFormat, verificationHandler) :
                    MBeanServerService.addService(context.getServiceTarget(), resolvedDomain, expressionsDomain, legacyWithProperPropertyFormat);
        if (newControllers != null) {
            newControllers.add(controller);
        }
    }

    private String getDomainName(OperationContext context, ModelNode model, String child) throws OperationFailedException {
        if (!model.hasDefined(CommonAttributes.EXPOSE_MODEL)) {
            return null;
        }
        if (!model.get(CommonAttributes.EXPOSE_MODEL).hasDefined(child)) {
            return null;
        }
        ModelNode childModel = model.get(CommonAttributes.EXPOSE_MODEL, child);
        return ExposeModelResource.getDomainNameAttribute(child).resolveModelAttribute(context, childModel).asString();
    }

}
