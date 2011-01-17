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

package org.jboss.as.ee.container;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import javax.interceptor.InvocationContext;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

/**
 * Utility for getting annotated methods.
 *
 * @author John Bailey
 */
public class Util {
    public static Method getSingleAnnotatedMethod(final Class<?> type, final ClassInfo classInfo, final Class<? extends Annotation> annotationType, final boolean requireInvocationContext) {
        Method method = null;
        if(classInfo != null) {
            // Try to resolve with the help of the annotation index
            final Map<DotName, List<AnnotationInstance>> classAnnotations = classInfo.annotations();

            final List<AnnotationInstance> instances = classAnnotations.get(DotName.createSimple(annotationType.getName()));
            if (instances == null || instances.isEmpty()) {
                return null;
            }

            if (instances.size() > 1) {
                throw new IllegalArgumentException("Only one method may be annotated with " + annotationType + " per managed bean.");
            }

            final AnnotationTarget target = instances.get(0).target();
            if (!(target instanceof MethodInfo)) {
                throw new IllegalArgumentException(annotationType + " is only valid on method targets.");
            }

            final MethodInfo methodInfo = MethodInfo.class.cast(target);
            final Type[] args = methodInfo.args();
            try {
                switch (args.length) {
                    case 0:
                        if(requireInvocationContext) {
                            throw new IllegalArgumentException("Missing argument.  Methods annotated with " + annotationType + " must have either single InvocationContext argument.");
                        }
                        method = type.getDeclaredMethod(methodInfo.name());
                        break;
                    case 1:
                        if(!InvocationContext.class.getName().equals(args[0].name().toString())) {
                            throw new IllegalArgumentException("Invalid argument type.  Methods annotated with " + annotationType + " must have either single InvocationContext argument.");
                        }
                        method = type.getDeclaredMethod(methodInfo.name(), InvocationContext.class);
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid number of arguments for method " + methodInfo.name() + " annotated with " + annotationType + " on class " + type.getName());
                }
            } catch(NoSuchMethodException e) {
                throw new IllegalArgumentException("Failed to get " + annotationType + " method for type: " + type.getName(), e);
            }
        } else {
            // No index information.  Default to normal reflection
            for(Method typeMethod : type.getDeclaredMethods()) {
                if(typeMethod.isAnnotationPresent(annotationType)) {
                    method = typeMethod;
                    switch (method.getParameterTypes().length){
                        case 0:
                            if(requireInvocationContext) {
                                throw new IllegalArgumentException("Method " + method.getName() + " annotated with " + annotationType + " must have a single InvocationContext parameter");
                            }
                            break;
                        case 1:
                            if(!InvocationContext.class.equals(method.getParameterTypes()[0])) {
                                throw new IllegalArgumentException("Method " + method.getName() + " annotated with " + annotationType + " must have a single InvocationContext parameter.");
                            }
                        default:
                            throw new IllegalArgumentException("Methods " + method.getName() + " annotated with " + annotationType + " can only have a single InvocationContext parameter.");
                    }
                    break;
                }
            }
        }
        if(method != null) {
            method.setAccessible(true);
        }
        return method;
    }
}
