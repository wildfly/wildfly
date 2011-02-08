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

import org.jboss.as.model.AbstractSubsystemAdd;
import org.jboss.as.model.BootUpdateContext;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.weld.deployment.processors.BeanArchiveProcessor;
import org.jboss.as.weld.deployment.processors.BeansXmlProcessor;
import org.jboss.as.weld.deployment.processors.WebIntegrationProcessor;
import org.jboss.as.weld.deployment.processors.WeldBeanManagerServiceProcessor;
import org.jboss.as.weld.deployment.processors.WeldDependencyProcessor;
import org.jboss.as.weld.deployment.processors.WeldDeploymentProcessor;
import org.jboss.weld.bootstrap.api.SingletonProvider;
import org.jboss.weld.bootstrap.api.helpers.TCCLSingletonProvider;

/**
 * The weld subsystem add update
 *
 * @author Stuart Douglas
 */
public class WeldSubsystemAdd extends AbstractSubsystemAdd<WeldSubsystemElement> {

    private static final long serialVersionUID = -2585735027037146422L;

    protected WeldSubsystemAdd() {
        super(WeldExtension.NAMESPACE);
    }

    /** {@inheritDoc} */
    protected <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {

    }

    /** {@inheritDoc} */
    protected void applyUpdateBootAction(final BootUpdateContext updateContext) {
        SingletonProvider.initialize(new TCCLSingletonProvider());
        updateContext.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_WELD, new WeldDependencyProcessor());
        updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_WELD_DEPLOYMENT, new BeansXmlProcessor());
        updateContext.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_WELD_WEB_INTEGRATION, new WebIntegrationProcessor());
        updateContext.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_WELD_BEAN_ARCHIVE, new BeanArchiveProcessor());
        updateContext.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_WELD_DEPLOYMENT, new WeldDeploymentProcessor());
        updateContext.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_WELD_BEAN_MANAGER, new WeldBeanManagerServiceProcessor());

    }

    /** {@inheritDoc} */
    protected WeldSubsystemElement createSubsystemElement() {
        return new WeldSubsystemElement();
    }

}
