/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.weld.deployment.processors;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.InterceptorDescription;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.WeldDeploymentMarker;
import org.jboss.as.weld.ejb.Jsr299BindingsInterceptor;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.logging.Logger;

import javax.interceptor.InvocationContext;

/**
 * Deployment processor that integrates weld interceptors with EJB's.
 *
 * @author Stuart Douglas
 */
public class WeldEjbInterceptorIntegrationProcessor implements DeploymentUnitProcessor {


    private static final Logger log = Logger.getLogger("org.jboss.as.weld");

    private static final String CDI_INTERCEPTOR = Jsr299BindingsInterceptor.class.getName();

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        if (!WeldDeploymentMarker.isPartOfWeldDeployment(deploymentUnit)) {
            return;
        }

        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);

        //Add the interceptor to the interceptor chain.
        //This is not ideal, but as the interceptor is not indexed
        //we cannot rely on the annotation processor to process it

        final EEModuleClassDescription cdiInterceptorClass = moduleDescription.getOrAddClassByName(CDI_INTERCEPTOR);

        cdiInterceptorClass.setPostConstructMethod(MethodIdentifier.getIdentifier(void.class, "doPostConstruct", InvocationContext.class));
        cdiInterceptorClass.setPreDestroyMethod(MethodIdentifier.getIdentifier(void.class, "doPreDestroy", InvocationContext.class));
        cdiInterceptorClass.setAroundInvokeMethod( MethodIdentifier.getIdentifier(Object.class, "doAroundInvoke", InvocationContext.class));

        for (final ComponentDescription component : moduleDescription.getComponentDescriptions()) {
            if (component instanceof SessionBeanComponentDescription) {
                component.addClassInterceptor(new InterceptorDescription(CDI_INTERCEPTOR));
            }
        }

    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}