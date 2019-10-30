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

package org.jboss.as.ejb3.deployment.processors;

import java.util.List;

import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.interceptor.InvocationContext;

import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.interceptors.InterceptorClassDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import static org.jboss.as.ee.logging.EeLogger.ROOT_LOGGER;

/**
 * Deployment processor responsible for finding @PostConstruct and @PreDestroy annotated methods.
 *
 * @author Stuart Douglas
 */
public class PassivationAnnotationParsingProcessor implements DeploymentUnitProcessor {
    private static final DotName POST_ACTIVATE = DotName.createSimple(PostActivate.class.getName());
    private static final DotName PRE_PASSIVATE = DotName.createSimple(PrePassivate.class.getName());
    private static DotName[] PASSIVATION_ANNOTATIONS = {POST_ACTIVATE, PRE_PASSIVATE};

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final EEApplicationClasses applicationClasses = deploymentUnit.getAttachment(Attachments.EE_APPLICATION_CLASSES_DESCRIPTION);
        final CompositeIndex index = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.COMPOSITE_ANNOTATION_INDEX);

        for (DotName annotationName : PASSIVATION_ANNOTATIONS) {
            final List<AnnotationInstance> lifecycles = index.getAnnotations(annotationName);
            for (AnnotationInstance annotation : lifecycles) {
                processPassivation(eeModuleDescription, annotation.target(), annotationName);
            }
        }
    }

    public void undeploy(DeploymentUnit context) {
    }

    private void processPassivation(final EEModuleDescription eeModuleDescription, final AnnotationTarget target, final DotName annotationType) throws DeploymentUnitProcessingException {
        if (!(target instanceof MethodInfo)) {
            throw EeLogger.ROOT_LOGGER.methodOnlyAnnotation(annotationType);
        }
        final MethodInfo methodInfo = MethodInfo.class.cast(target);
        final ClassInfo classInfo = methodInfo.declaringClass();
        final EEModuleClassDescription classDescription = eeModuleDescription.addOrGetLocalClassDescription(classInfo.name().toString());

        final Type[] args = methodInfo.args();
        if (args.length > 1) {
            ROOT_LOGGER.warn(EeLogger.ROOT_LOGGER.invalidNumberOfArguments(methodInfo.name(), annotationType, classInfo.name()));
            return;
        } else if (args.length == 1 && !args[0].name().toString().equals(InvocationContext.class.getName())) {
            ROOT_LOGGER.warn(EeLogger.ROOT_LOGGER.invalidSignature(methodInfo.name(), annotationType, classInfo.name(), "void methodName(InvocationContext ctx)"));
            return;
        }

        final MethodIdentifier methodIdentifier;
        if (args.length == 0) {
            methodIdentifier = MethodIdentifier.getIdentifier(Void.TYPE, methodInfo.name());
        } else {
            methodIdentifier = MethodIdentifier.getIdentifier(Void.TYPE, methodInfo.name(), InvocationContext.class);
        }
        final InterceptorClassDescription.Builder builder = InterceptorClassDescription.builder(classDescription.getInterceptorClassDescription());
        if (annotationType == POST_ACTIVATE) {
            builder.setPostActivate(methodIdentifier);
        } else {
            builder.setPrePassivate(methodIdentifier);
        }
        classDescription.setInterceptorClassDescription(builder.build());
    }
}
