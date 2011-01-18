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

package org.jboss.as.security;

import javax.naming.Context;
import javax.naming.Reference;

import org.jboss.as.model.AbstractSubsystemAdd;
import org.jboss.as.model.BootUpdateContext;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.as.naming.service.JavaContextService;
import org.jboss.as.security.context.SecurityDomainObjectFactory;
import org.jboss.as.security.processors.SecurityDependencyProcessor;
import org.jboss.as.security.service.JaasBinderService;
import org.jboss.as.security.service.SecurityBootstrapService;
import org.jboss.as.server.deployment.Phase;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.Values;

/**
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public final class SecuritySubsystemAdd extends AbstractSubsystemAdd<SecuritySubsystemElement> {

    private static final long serialVersionUID = 5985062059276472263L;

    /**
     * Create a new instance
     */
    protected SecuritySubsystemAdd() {
        super(Namespace.SECURITY_1_0.getUriString());
    }

    /** {@inheritDoc} */
    protected <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        final ServiceTarget target = updateContext.getServiceTarget();

        // add bootstrap service
        final SecurityBootstrapService bootstrapService = new SecurityBootstrapService();
        target.addService(SecurityBootstrapService.SERVICE_NAME, bootstrapService).addListener(
                new UpdateResultHandler.ServiceStartListener<P>(resultHandler, param)).setInitialMode(
                ServiceController.Mode.ACTIVE).install();

        // add service to bind SecurityDomainObjectFactory to JNDI
        final Reference reference = SecurityDomainObjectFactory.createReference("JSM");
        final JaasBinderService binderService = new JaasBinderService(Values.immediateValue(reference));
        target.addService(JaasBinderService.SERVICE_NAME, binderService).addDependency(JavaContextService.SERVICE_NAME,
                Context.class, binderService.getContextInjector()).setInitialMode(ServiceController.Mode.ACTIVE).install();
    }

    /** {@inheritDoc} */
    protected void applyUpdateBootAction(BootUpdateContext updateContext) {
        updateContext.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_MODULE, new SecurityDependencyProcessor());
        super.applyUpdateBootAction(updateContext);
    }

    /** {@inheritDoc} */
    protected SecuritySubsystemElement createSubsystemElement() {
        final SecuritySubsystemElement element = new SecuritySubsystemElement();
        return element;
    }

}
