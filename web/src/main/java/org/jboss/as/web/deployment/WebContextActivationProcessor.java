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

package org.jboss.as.web.deployment;

import static org.jboss.as.web.WebMessages.MESSAGES;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.deployment.WebDeploymentService.ContextActivator;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.deployment.interceptor.AbstractLifecycleInterceptor;
import org.jboss.osgi.deployment.interceptor.InvocationContext;
import org.jboss.osgi.deployment.interceptor.LifecycleInterceptor;
import org.jboss.osgi.deployment.interceptor.LifecycleInterceptorException;
import org.jboss.osgi.resolver.XBundle;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Start/stop an OSGi webapp context according to bundle lifecycle changes.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 26-Jun-2012
 */
public class WebContextActivationProcessor implements DeploymentUnitProcessor {

    static final AttachmentKey<ServiceRegistration> REGISTRATION_KEY = AttachmentKey.create(ServiceRegistration.class);

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        ContextActivator activator = depUnit.getAttachment(ContextActivator.ATTACHMENT_KEY);
        XBundle bundle = depUnit.getAttachment(Attachments.INSTALLED_BUNDLE_KEY);
        if (activator != null && bundle != null) {

            // Start the context when the bundle will get started automatically
            Deployment dep = bundle.adapt(Deployment.class);
            if (dep.isAutoStart()) {
                activator.startAsync();
            }

            // Register the bundle lifecycle interceptor
            BundleContext context = depUnit.getAttachment(Attachments.SYSTEM_CONTEXT_KEY);
            LifecycleInterceptor interceptor = new WebContextLifecycleInterceptor(bundle, activator);
            ServiceRegistration registration = context.registerService(LifecycleInterceptor.class.getName(), interceptor, null);
            depUnit.putAttachment(REGISTRATION_KEY, registration);
        }
    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
        ServiceRegistration registration = depUnit.getAttachment(REGISTRATION_KEY);
        if (registration != null) {
            registration.unregister();
        }
    }

    static class WebContextLifecycleInterceptor extends AbstractLifecycleInterceptor {

        private final ContextActivator activator;
        private final XBundle bundle;

        WebContextLifecycleInterceptor(XBundle bundle, ContextActivator activator) {
            this.activator = activator;
            this.bundle = bundle;
        }

        @Override
        public void invoke(int state, InvocationContext context) {

            if (bundle != context.getBundle())
                return;

            switch (state) {
                case Bundle.ACTIVE:
                    try {
                        if (!activator.start(4, TimeUnit.SECONDS)) {
                            throw new LifecycleInterceptorException(MESSAGES.startContextFailed());
                        }
                    } catch (TimeoutException ex) {
                        throw new LifecycleInterceptorException(ex.getMessage(), ex);
                    }
                    break;
                case Bundle.RESOLVED:
                    activator.stop(4, TimeUnit.SECONDS);
                    break;
            }
        }
    }
}
