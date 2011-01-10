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

package org.jboss.as.managedbean;

import org.jboss.as.managedbean.processors.ManagedBeanAnnotationProcessor;
import org.jboss.as.managedbean.processors.ManagedBeanDependencyProcessor;
import org.jboss.as.managedbean.processors.ManagedBeanDeploymentProcessor;
import org.jboss.as.managedbean.processors.ManagedBeanSubDeploymentProcessor;
import org.jboss.as.model.AbstractSubsystemAdd;
import org.jboss.as.model.BootUpdateContext;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.as.server.deployment.Phase;

/**
 * The managed subsystem add update.
 *
 * @author Emanuel Muckenhuber
 */
public class ManagedBeansSubsystemAdd extends AbstractSubsystemAdd<ManagedBeansSubsystemElement> {

    private static final long serialVersionUID = 8639964348855747105L;

    protected ManagedBeansSubsystemAdd() {
        super(ManagedBeansExtension.NAMESPACE);
    }

    protected <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
    }

    /** {@inheritDoc} */
    protected void applyUpdateBootAction(final BootUpdateContext updateContext) {
        updateContext.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_MANAGED_BEAN_SUB_DEPLOY_CHECK, new ManagedBeanSubDeploymentProcessor());
        updateContext.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_MANAGED_BEAN, new ManagedBeanDependencyProcessor());
        updateContext.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_ANNOTATION_MANAGED_BEAN, new ManagedBeanAnnotationProcessor());
        updateContext.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_MANAGED_BEAN_DEPLOYMENT, new ManagedBeanDeploymentProcessor());
    }

    /** {@inheritDoc} */
    protected ManagedBeansSubsystemElement createSubsystemElement() {
        return new ManagedBeansSubsystemElement();
    }
}
