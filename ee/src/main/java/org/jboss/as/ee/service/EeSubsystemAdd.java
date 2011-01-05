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

package org.jboss.as.ee.service;

import org.jboss.as.ee.processor.EarStructureProcessor;
import org.jboss.as.model.AbstractSubsystemAdd;
import org.jboss.as.model.BootUpdateContext;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.as.server.deployment.Phase;
import org.jboss.logging.Logger;

/**
 * @author Weston M. Price
 *
 */
public final class EeSubsystemAdd extends AbstractSubsystemAdd<EeSubsystemElement> {

    private static final long serialVersionUID = -3501832241733737257L;

    private static final Logger logger = Logger.getLogger("org.jboss.as.ee");

    /**
     * @param namespaceUri
     */
    protected EeSubsystemAdd() {
        super(EeExtension.NAMESPACE);
    }

    @Override
    protected <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
    }

    @Override
    protected void applyUpdateBootAction(BootUpdateContext updateContext) {
        logger.info("Activating EE subsystem");
        updateContext.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_EAR_DEPLOYMENT, new EarStructureProcessor());
    }

    @Override
    protected EeSubsystemElement createSubsystemElement() {
        return new EeSubsystemElement();
    }

}
