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

package org.jboss.as.arquillian.parser;

import org.jboss.as.arquillian.service.ArquillianDeploymentProcessor;
import org.jboss.as.arquillian.service.ArquillianRunWithAnnotationProcessor;
import org.jboss.as.arquillian.service.ArquillianService;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.model.AbstractSubsystemAdd;
import org.jboss.as.model.BootUpdateContext;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.logging.Logger;

/**
 * Arquillian subsystem add element.
 *
 * @author Thomas.Diesler@jboss.com
 * @author Kabir Khan
 * @since 17-Nov-2010
 */
public final class ArquillianSubsystemAdd extends AbstractSubsystemAdd<ArquillianSubsystemElement> {

    private static final long serialVersionUID = -7876823389815006153L;
    private static final Logger log = Logger.getLogger("org.jboss.as.arquillian");

    protected ArquillianSubsystemAdd() {
        super(ArquillianExtension.NAMESPACE);
    }

    @Override
    protected ArquillianSubsystemElement createSubsystemElement() {
        return new ArquillianSubsystemElement();
    }

    @Override
    protected <P> void applyUpdate(final UpdateContext updateContext, final UpdateResultHandler<? super Void, P> resultHandler, final P param) {
    }

    @Override
    protected void applyUpdateBootAction(final BootUpdateContext updateContext) {
        log.infof("Activating Arquillian Subsystem");
        ArquillianService.addService(updateContext.getServiceTarget());
        updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_ARQUILLIAN_RUNWITH, new ArquillianRunWithAnnotationProcessor());
        updateContext.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_ARQUILLIAN_DEPLOYMENT, new ArquillianDeploymentProcessor());
    }

}
