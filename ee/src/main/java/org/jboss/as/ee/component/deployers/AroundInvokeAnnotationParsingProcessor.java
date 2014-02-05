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

package org.jboss.as.ee.component.deployers;

import java.util.List;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.ee.component.Attachments;
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

/**
 * Deployment processor responsible for finding @AroundInvoke annotated methods in classes within a deployment.
 *
 * @author John Bailey
 * @author Stuart Douglas
 */
public class AroundInvokeAnnotationParsingProcessor implements DeploymentUnitProcessor {

    private static final DotName AROUND_INVOKE_ANNOTATION_NAME = DotName.createSimple(AroundInvoke.class.getName());

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final CompositeIndex index = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.COMPOSITE_ANNOTATION_INDEX);

        final List<AnnotationInstance> aroundInvokes = index.getAnnotations(AROUND_INVOKE_ANNOTATION_NAME);
        for (AnnotationInstance annotation : aroundInvokes) {
            processAroundInvoke(eeModuleDescription, annotation.target());
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {
    }

    private void processAroundInvoke(final EEModuleDescription eeModuleDescription, final AnnotationTarget target) throws DeploymentUnitProcessingException {
        if (!(target instanceof MethodInfo)) {
            throw EeLogger.ROOT_LOGGER.methodOnlyAnnotation(AROUND_INVOKE_ANNOTATION_NAME);
        }
        final MethodInfo methodInfo = MethodInfo.class.cast(target);
        final ClassInfo classInfo = methodInfo.declaringClass();
        final EEModuleClassDescription classDescription = eeModuleDescription.addOrGetLocalClassDescription(classInfo.name().toString());
        final List<AnnotationInstance> classAroundInvokes = classInfo.annotations().get(AROUND_INVOKE_ANNOTATION_NAME);
        if(classAroundInvokes.size() > 1) {
           throw EeLogger.ROOT_LOGGER.aroundInvokeAnnotationUsedTooManyTimes(classInfo.name(), classAroundInvokes.size());
        }
        validateArgumentType(classInfo, methodInfo);
        InterceptorClassDescription.Builder builder = InterceptorClassDescription.builder(classDescription.getInterceptorClassDescription());
        builder.setAroundInvoke(MethodIdentifier.getIdentifier(Object.class, methodInfo.name(), InvocationContext.class));
        classDescription.setInterceptorClassDescription(builder.build());
    }

    private void validateArgumentType(final ClassInfo classInfo, final MethodInfo methodInfo) {
        final Type[] args = methodInfo.args();
        switch (args.length) {
            case 0:
                throw new IllegalArgumentException(EeLogger.ROOT_LOGGER.invalidSignature(methodInfo.name(), AROUND_INVOKE_ANNOTATION_NAME, classInfo.name(), "Object methodName(InvocationContext ctx)"));
            case 1:
                if (!InvocationContext.class.getName().equals(args[0].name().toString())) {
                    throw new IllegalArgumentException(EeLogger.ROOT_LOGGER.invalidSignature(methodInfo.name(), AROUND_INVOKE_ANNOTATION_NAME, classInfo.name(), "Object methodName(InvocationContext ctx)"));
                }
                break;
            default:
                throw new IllegalArgumentException(EeLogger.ROOT_LOGGER.invalidNumberOfArguments(methodInfo.name(), AROUND_INVOKE_ANNOTATION_NAME, classInfo.name()));
        }
        if (!methodInfo.returnType().name().toString().equals(Object.class.getName())) {
            throw EeLogger.ROOT_LOGGER.invalidReturnType(Object.class.getName(), methodInfo.name(), AROUND_INVOKE_ANNOTATION_NAME, classInfo.name());
        }
    }
}
