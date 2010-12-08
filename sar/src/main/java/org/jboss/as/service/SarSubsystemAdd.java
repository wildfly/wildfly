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

package org.jboss.as.service;

import org.jboss.as.deployment.Phase;
import org.jboss.as.model.AbstractSubsystemAdd;
import org.jboss.as.model.BootUpdateContext;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class SarSubsystemAdd extends AbstractSubsystemAdd<SarSubsystemElement> {

    private static final long serialVersionUID = 7065406809630792436L;

    public SarSubsystemAdd() {
        super(SarExtension.NAMESPACE);
    }

    protected <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
    }

    @Override
    protected void applyUpdateBootAction(final BootUpdateContext updateContext) {
        updateContext.addDeploymentProcessor(INIT_ME, new SarModuleDependencyProcessor(), Phase.SAR_MODULE_DEPENDENCY_PROCESSOR);
        updateContext.addDeploymentProcessor(INIT_ME, new ServiceDeploymentParsingProcessor(), Phase.SERVICE_DEPLOYMENT_PARSING_PROCESSOR);
        updateContext.addDeploymentProcessor(INIT_ME, new ParsedServiceDeploymentProcessor(), Phase.PARSED_SERVICE_DEPLOYMENT_PROCESSOR);
    }

    @Override
    protected SarSubsystemElement createSubsystemElement() {
        return new SarSubsystemElement();
    }
}
