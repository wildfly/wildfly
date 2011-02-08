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

package org.jboss.as.weld;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.server.BootOperationHandler;
import org.jboss.as.server.NewBootOperationContext;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.weld.deployment.processors.BeanArchiveProcessor;
import org.jboss.as.weld.deployment.processors.BeansXmlProcessor;
import org.jboss.as.weld.deployment.processors.WebIntegrationProcessor;
import org.jboss.as.weld.deployment.processors.WeldDependencyProcessor;
import org.jboss.as.weld.deployment.processors.WeldDeploymentProcessor;
import org.jboss.dmr.ModelNode;
import org.jboss.weld.bootstrap.api.SingletonProvider;
import org.jboss.weld.bootstrap.api.helpers.TCCLSingletonProvider;

/**
 * @author Emanuel Muckenhuber
 */
class NewWeldSubsystemAdd implements ModelAddOperationHandler, BootOperationHandler {

    static final NewWeldSubsystemAdd INSTANCE = new NewWeldSubsystemAdd();

    /** {@inheritDoc} */
    @Override
    public Cancellable execute(final NewOperationContext context, final ModelNode operation, final ResultHandler resultHandler) {

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set(REMOVE);
        compensatingOperation.get(OP_ADDR).set(operation.require(OP_ADDR));

        if(context instanceof NewBootOperationContext) {
            final NewBootOperationContext bootContext = (NewBootOperationContext) context;
            SingletonProvider.initialize(new TCCLSingletonProvider());
            bootContext.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_WELD, new WeldDependencyProcessor());
            bootContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_WELD_DEPLOYMENT, new BeansXmlProcessor());
            bootContext.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_WELD_WEB_INTEGRATION, new WebIntegrationProcessor());
            bootContext.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_WELD_BEAN_ARCHIVE, new BeanArchiveProcessor());
            bootContext.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_WELD_DEPLOYMENT, new WeldDeploymentProcessor());
        }

        resultHandler.handleResultComplete(compensatingOperation);

        return Cancellable.NULL;
    }

}
