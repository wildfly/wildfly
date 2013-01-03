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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;

/**
 * Uses the annotation index to check whether there is a class annotated
 * with JUnit @RunWith, or extending from the TestNG Arquillian runner.
 * In which case an {@link ArquillianConfig} service is created.
 *
 * @author Thomas.Diesler@jboss.com
 */
public class ArquillianConfigBuilder {

    private static final Logger log = Logger.getLogger("org.jboss.as.arquillian");

    /*
     * Note: Do not put direct class references on JUnit or TestNG here; this
     * must be compatible with both without resulting in NCDFE
     *
     * AS7-1303
     */

    private static final String CLASS_NAME_JUNIT_RUNNER = "org.junit.runner.RunWith";

    private static final String CLASS_NAME_TESTNG_RUNNER = "org.jboss.arquillian.testng.Arquillian";

    private static final AttachmentKey<Set<String>> CLASSES = AttachmentKey.create(Set.class);

    ArquillianConfigBuilder(DeploymentUnit deploymentUnit) {
    }

    static ArquillianConfig processDeployment(ArquillianService arqService, DeploymentUnit depUnit) {

        // Get Test Class Names
        final Set<String> testClasses = depUnit.getAttachment(CLASSES);

        // No tests found
        if (testClasses == null || testClasses.isEmpty()) {
            return null;
        }

        // FIXME: Why do we get another service started event from a deployment INSTALLED service?
        ArquillianConfig arqConfig = new ArquillianConfig(arqService, depUnit, testClasses);
        ServiceController<?> service = arqService.getServiceContainer().getService(arqConfig.getServiceName());
        if (service != null) {

            final CountDownLatch latch = new CountDownLatch(1);
            service.setMode(ServiceController.Mode.REMOVE);
            service.addListener(new AbstractServiceListener<Object>() {
                @Override
                public void listenerAdded(ServiceController<? extends Object> serviceController) {
                    if(serviceController.getState() == ServiceController.State.REMOVED) {
                        latch.countDown();
                    }
                }

                @Override
                public void transition(ServiceController<? extends Object> serviceController, ServiceController.Transition transition) {
                    if(serviceController.getState() == ServiceController.State.REMOVED) {
                        latch.countDown();
                    }
                }
            });
            try {
                latch.await(20, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        depUnit.putAttachment(ArquillianConfig.KEY, arqConfig);
        return arqConfig;
    }

    static void handleParseAnnotations(final DeploymentUnit deploymentUnit) {

        final CompositeIndex compositeIndex = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        if(compositeIndex == null) {
            log.warnf("Cannot find composite annotation index in: %s", deploymentUnit);
            return;
        }

        // Got JUnit?
        final DotName runWithName = DotName.createSimple(CLASS_NAME_JUNIT_RUNNER);
        final List<AnnotationInstance> runWithList = compositeIndex.getAnnotations(runWithName);

        // Got TestNG?
        final DotName testNGClassName = DotName.createSimple(CLASS_NAME_TESTNG_RUNNER);
        final Set<ClassInfo> testNgTests = compositeIndex.getAllKnownSubclasses(testNGClassName);

        // Get Test Class Names
        final Set<String> testClasses = new HashSet<String>();
        // JUnit
        for (AnnotationInstance instance : runWithList) {
            final AnnotationTarget target = instance.target();
            if (target instanceof ClassInfo) {
                final ClassInfo classInfo = (ClassInfo) target;
                final String testClassName = classInfo.name().toString();
                testClasses.add(testClassName);
            }
        }
        // TestNG
        for(final ClassInfo classInfo : testNgTests){
            testClasses.add(classInfo.name().toString());
        }
        deploymentUnit.putAttachment(CLASSES, testClasses);
    }
}
