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

package org.jboss.as.osgi.web;

import static org.jboss.as.web.WebMessages.MESSAGES;
import static org.jboss.as.web.WebSubsystemServices.JBOSS_WEB;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.osgi.OSGiConstants;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.deployment.WebDeploymentService.ContextActivator;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.deployment.interceptor.AbstractLifecycleInterceptor;
import org.jboss.osgi.deployment.interceptor.InvocationContext;
import org.jboss.osgi.deployment.interceptor.LifecycleInterceptor;
import org.jboss.osgi.deployment.interceptor.LifecycleInterceptorException;
import org.jboss.osgi.framework.Services;
import org.jboss.osgi.resolver.XBundle;
import org.jboss.osgi.resolver.XBundleRevision;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Start/stop an OSGi webapp context according to bundle lifecycle changes.
 *
 * @author Thomas.Diesler@jboss.com
 * @author David Bosschaert
 * @since 26-Jun-2012
 */
public class WebContextActivationProcessor implements DeploymentUnitProcessor {
    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        ContextActivator activator = depUnit.getAttachment(ContextActivator.ATTACHMENT_KEY);
        XBundle bundle = depUnit.getAttachment(OSGiConstants.BUNDLE_KEY);
        if (activator != null && bundle != null) {
            // Start the context when the bundle will get started automatically
            Deployment deployment = bundle.adapt(Deployment.class);
            if (deployment.isAutoStart()) {
                activator.startAsync();
            }

            // Add the {@link ContextActivator} to the {@link XBundleRevision}
            XBundleRevision brev = bundle.getBundleRevision();
            brev.addAttachment(ContextActivator.class, activator);
        }
    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
        ContextActivator activator = depUnit.getAttachment(ContextActivator.ATTACHMENT_KEY);
        XBundle bundle = depUnit.getAttachment(OSGiConstants.BUNDLE_KEY);
        if (activator != null && bundle != null) {
            bundle.adapt(Deployment.class).removeAttachment(ContextActivator.class);
        }
    }

    public static class WebContextLifecycleInterceptor extends AbstractLifecycleInterceptor implements Service<LifecycleInterceptor> {
        static final ServiceName JBOSS_WEB_LIFECYCLE_INTERCEPTOR = JBOSS_WEB.append("lifecycle-interceptor");

        private final InjectedValue<BundleContext> injectedSystemContext = new InjectedValue<BundleContext>();
        private ServiceRegistration registration;

        public static ServiceController<LifecycleInterceptor> addService(ServiceTarget serviceTarget, ServiceVerificationHandler verificationHandler) {
            WebContextLifecycleInterceptor service = new WebContextLifecycleInterceptor();
            ServiceBuilder<LifecycleInterceptor> builder = serviceTarget.addService(JBOSS_WEB_LIFECYCLE_INTERCEPTOR, service);
            builder.addDependency(Services.FRAMEWORK_ACTIVE, BundleContext.class, service.injectedSystemContext);
            builder.addListener(verificationHandler);
            builder.setInitialMode(Mode.PASSIVE);
            return builder.install();
        }

        @Override
        public void start(StartContext context) throws StartException {
            BundleContext syscontext = injectedSystemContext.getValue();
            registration = syscontext.registerService(LifecycleInterceptor.class.getName(), this, null);
        }

        @Override
        public void stop(StopContext context) {
            if (registration != null)
                registration.unregister();
        }

        @Override
        public void invoke(int state, InvocationContext context) {
            XBundle bundle = (XBundle) context.getBundle();
            XBundleRevision brev = bundle.getBundleRevision();

            WabServletContextFactory wscf = brev.getAttachment(WabServletContextFactory.class);
            if (wscf != null) {
                wscf.setBundleContext(bundle.getBundleContext());
            }

            ContextActivator activator = brev.getAttachment(ContextActivator.class);
            if (activator != null) {
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

        @Override
        public LifecycleInterceptor getValue() throws IllegalStateException, IllegalArgumentException {
            return this;
        }
    }
}
