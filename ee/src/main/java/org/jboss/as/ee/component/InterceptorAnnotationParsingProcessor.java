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

package org.jboss.as.ee.component;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import java.util.List;
import java.util.Map;

/**
 * Deployment processor responsible for analyzing each attached {@link AbstractComponentDescription} instance to
 * configure interceptor AroundInvoke methods.
 * <p/>
 * This class does not process the <code>@Interceptors</code> annotation, this is handled by {@link ComponentInterceptorAnnotationParsingProcessor}
 *
 * @author John Bailey
 * @author Stuart Douglas
 */
public class InterceptorAnnotationParsingProcessor extends AbstractComponentConfigProcessor {

    private static final DotName AROUND_INVOKE_ANNOTATION_NAME = DotName.createSimple(AroundInvoke.class.getName());
    /**
     * {@inheritDoc} *
     */
    protected void processComponentConfig(final DeploymentUnit deploymentUnit, final DeploymentPhaseContext phaseContext, final CompositeIndex index, final AbstractComponentDescription componentDescription) throws DeploymentUnitProcessingException {
        processClass(index, componentDescription, DotName.createSimple(componentDescription.getComponentClassName()), componentDescription.getComponentClassName(), true);
        for(InterceptorDescription descriptor : componentDescription.getAllInterceptors().values()) {
            processClass(index,DotName.createSimple(descriptor.getInterceptorClassName()),descriptor, componentDescription.getComponentClassName());
        }
    }

    private void processClass(final CompositeIndex index, final AbstractComponentDescription componentDescription, final DotName className, final String actualClassName, boolean declaredOnTargetClass) {
        final ClassInfo classInfo = index.getClassByName(className);
        if(classInfo == null) {
            return;
        }

        final DotName superName = classInfo.superName();
        if(superName != null) {
            processClass(index, componentDescription, superName,actualClassName,declaredOnTargetClass);
        }

        final InterceptorMethodDescription aroundInvokeMethod = getAroundInvokeMethod(classInfo,actualClassName,declaredOnTargetClass);
        if (aroundInvokeMethod != null) {
            componentDescription.addAroundInvokeMethod(aroundInvokeMethod);
        }
    }

    private void processClass(final CompositeIndex index,DotName className, final InterceptorDescription interceptorDescription, final String actualClassName) {
        final ClassInfo classInfo = index.getClassByName(className);
        if(classInfo == null) {
            return;
        }

        final DotName superName = classInfo.superName();
        if(superName != null) {
            processClass(index,superName, interceptorDescription,actualClassName);
        }

        final InterceptorMethodDescription aroundInvokeMethod = getAroundInvokeMethod(classInfo,actualClassName,false);
        if (aroundInvokeMethod != null) {
            interceptorDescription.addAroundInvokeMethod(aroundInvokeMethod);
        }
    }

    private InterceptorMethodDescription getAroundInvokeMethod(final ClassInfo classInfo, final  String actualClass, boolean declaredOnTargetClass) {
        if (classInfo == null) {
            return null; // No index info
        }

        // Try to resolve with the help of the annotation index
        final Map<DotName, List<AnnotationInstance>> classAnnotations = classInfo.annotations();

        final List<AnnotationInstance> instances = classAnnotations.get(AROUND_INVOKE_ANNOTATION_NAME);
        if (instances == null || instances.isEmpty()) {
            return null;
        }

        if (instances.size() > 1) {
            throw new IllegalArgumentException("Only one method may be annotated with @AroundInvoke per bean.");
        }

        final AnnotationTarget target = instances.get(0).target();
        if (!(target instanceof MethodInfo)) {
            throw new IllegalArgumentException("@AroundInvoke is only valid on method targets.");
        }

        final MethodInfo methodInfo = MethodInfo.class.cast(target);

        validateArgumentType(classInfo,methodInfo);
        return InterceptorMethodDescription.create(classInfo.name().toString(),actualClass, methodInfo,declaredOnTargetClass);
    }


    private void validateArgumentType(final ClassInfo classInfo, final MethodInfo methodInfo) {
        final Type[] args = methodInfo.args();
        switch (args.length) {
            case 0:
                throw new IllegalArgumentException("Invalid argument signature.  Methods annotated with " + AROUND_INVOKE_ANNOTATION_NAME + " must have a single InvocationContext argument.");
            case 1:
                if (!InvocationContext.class.getName().equals(args[0].name().toString())) {
                    throw new IllegalArgumentException("Invalid argument type.  Methods annotated with " + AROUND_INVOKE_ANNOTATION_NAME + " must have a single InvocationContext argument.");
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid number of arguments for method " + methodInfo.name() + " annotated with " + AROUND_INVOKE_ANNOTATION_NAME + " on class " + classInfo.name());
        }
        if(!methodInfo.returnType().name().toString().equals(Object.class.getName())) {
              throw new IllegalArgumentException("@AroundInvoke methods must have an Object return type for " + methodInfo.name() + " annotated with " + AROUND_INVOKE_ANNOTATION_NAME + " on class " + classInfo.name());
        }
    }
}
