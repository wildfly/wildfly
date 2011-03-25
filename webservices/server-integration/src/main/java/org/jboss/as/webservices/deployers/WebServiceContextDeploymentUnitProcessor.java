/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.webservices.deployers;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.BindingDescription;
import org.jboss.as.ee.component.LazyBindingSourceDescription;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ValueManagedObject;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.wsf.common.injection.ThreadLocalAwareWebServiceContext;

/**
 * DeploymentUnitProcessor that adds a lazy binding source description handler that can resolve it.
 *
 * @author Stuart Douglas
 */
public class WebServiceContextDeploymentUnitProcessor implements DeploymentUnitProcessor{


    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        phaseContext.addToAttachmentList(Attachments.LAZY_BINDING_SOURCES_HANDLERS, WebServiceContextLookup.INSTANCE);
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    private static class WebServiceContextLookup implements LazyBindingSourceDescription.LazyBingingSourceDescriptionHandler {

        public static final WebServiceContextLookup INSTANCE = new WebServiceContextLookup();

        private WebServiceContextLookup() {

        }

        @Override
        public boolean getResourceValue(BindingDescription referenceDescription, ServiceBuilder<?> serviceBuilder, DeploymentPhaseContext phaseContext, Injector<ManagedReferenceFactory> injector) {
            if(referenceDescription.getBindingType().equals("javax.xml.ws.WebServiceContext")) {
                injector.inject(new ValueManagedObject(new ImmediateValue<Object>(ThreadLocalAwareWebServiceContext.getInstance())));
                return true;
            }
            return false;
        }
    }
}
