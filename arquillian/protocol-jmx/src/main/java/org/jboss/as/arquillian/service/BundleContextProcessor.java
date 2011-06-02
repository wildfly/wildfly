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

package org.jboss.as.arquillian.service;

import static org.jboss.osgi.framework.Services.FRAMEWORK_ACTIVE;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import org.jboss.arquillian.testenricher.osgi.BundleContextAssociation;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.osgi.framework.FutureServiceValue;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;
import org.osgi.service.startlevel.StartLevel;

/**
 * Uses the annotation index to check whether there is an injection point for {@link BundleContext}
 *
 * @author Thomas.Diesler@jboss.com
 */
public class BundleContextProcessor extends ArquillianDeploymentProcessor<BundleContext>{

    private static final Logger log = Logger.getLogger("org.jboss.as.arquillian");

    private BundleContext bundleContext;

    BundleContextProcessor(ServiceContainer serviceContainer, DeploymentUnit deploymentUnit) {
        super(serviceContainer, deploymentUnit);
    }

    @Override
    BundleContext getValue() {
        return bundleContext;
    }

    @Override
    BundleContextProcessor process() {

        final CompositeIndex compositeIndex = getDeploymentUnit().getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        if(compositeIndex == null) {
            log.infof("Cannot find composite annotation index in: %s", getDeploymentUnit());
            return null;
        }

        final DotName dotName = DotName.createSimple(Inject.class.getName());
        final List<AnnotationInstance> annotationList = compositeIndex.getAnnotations(dotName);
        if (annotationList.isEmpty()) {
            return this;
        }

        for (AnnotationInstance instance : annotationList) {
            final AnnotationTarget target = instance.target();
            if (target instanceof FieldInfo) {
                final FieldInfo fieldInfo = (FieldInfo) target;
                ClassInfo declaringClass = fieldInfo.declaringClass();
                DotName name = declaringClass.name();
                if ("org.osgi.framework.BundleContext".equals(name.toString())) {
                    Framework framework = awaitActiveOSGiFramework();
                    bundleContext = framework.getBundleContext();
                    BundleContextAssociation.setBundleContext(bundleContext);
                }
            }
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    private Framework awaitActiveOSGiFramework() {
        ServiceController<Framework> controller = (ServiceController<Framework>) getServiceContainer().getRequiredService(FRAMEWORK_ACTIVE);
        Future<Framework> future = new FutureServiceValue<Framework>(controller);
        Framework framework;
        try {
            framework = future.get(10, TimeUnit.SECONDS);
            BundleContext context = framework.getBundleContext();
            ServiceReference sref = context.getServiceReference(StartLevel.class.getName());
            StartLevel startLevel = (StartLevel) context.getService(sref);
            if (startLevel.getStartLevel() < 5) {
                final CountDownLatch latch = new CountDownLatch(1);
                context.addFrameworkListener(new FrameworkListener() {
                    public void frameworkEvent(FrameworkEvent event) {
                        if (event.getType() == FrameworkEvent.STARTLEVEL_CHANGED) {
                            latch.countDown();
                        }
                    }
                });
                startLevel.setStartLevel(5);
                if (latch.await(20, TimeUnit.SECONDS) == false)
                    throw new TimeoutException("Timeout waiting for STARTLEVEL_CHANGED event");
            }
        } catch (RuntimeException rte) {
            throw rte;
        } catch (Exception ex) {
            throw new IllegalStateException("Error starting framework", ex);
        }
        return framework;
    }
}
