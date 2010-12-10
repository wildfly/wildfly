/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.mc;

import org.jboss.as.server.deployment.Phase;
import org.jboss.as.model.AbstractSubsystemAdd;
import org.jboss.as.model.BootUpdateContext;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;


/**
 * Microcontainer substem add.
 * Define processors for MC config handling.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
final class McSubsystemAdd extends AbstractSubsystemAdd<McSubsystemElement> {

    private static final long serialVersionUID = 1L;

    McSubsystemAdd() {
        super(MicrocontainerExtension.NAMESPACE);
    }

    @Override
    protected <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
    }

    protected void applyUpdateBootAction(final BootUpdateContext updateContext) {
        updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_MC_BEAN_DEPLOYMENT, new KernelDeploymentParsingProcessor());
        updateContext.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_MC_BEAN_DEPLOYMENT, new ParsedKernelDeploymentProcessor());
    }

    @Override
    protected McSubsystemElement createSubsystemElement() {
        return new McSubsystemElement();
    }
}