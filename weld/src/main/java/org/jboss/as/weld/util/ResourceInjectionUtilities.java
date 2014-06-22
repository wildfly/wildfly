/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.weld.util;

import java.beans.Introspector;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.InjectionPoint;

import org.jboss.as.weld.logging.WeldLogger;
import org.jboss.weld.injection.ParameterInjectionPoint;

public class ResourceInjectionUtilities {

    private ResourceInjectionUtilities() {
    }

    private static final String RESOURCE_LOOKUP_PREFIX = "java:comp/env";

    public static String getResourceName(String jndiName, String mappedName) {
        if (mappedName != null) {
            return mappedName;
        } else if (jndiName != null) {
            return jndiName;
        } else {
            throw WeldLogger.ROOT_LOGGER.cannotDetermineResourceName();
        }
    }

    public static String getResourceName(InjectionPoint injectionPoint) {
        Resource resource = getResourceAnnotated(injectionPoint).getAnnotation(Resource.class);
        String mappedName = resource.mappedName();
        if (!mappedName.equals("")) {
            return mappedName;
        }
        String name = resource.name();
        if (!name.equals("")) {
            //see if this is a prefixed name
            //and if so just return it
            int firstSlash = name.indexOf("/");
            int colon = name.indexOf(":");
            if(colon != -1) {
                if(firstSlash == -1 || colon < firstSlash) {
                    return name;
                }
            }

            return RESOURCE_LOOKUP_PREFIX + "/" + name;
        }
        String propertyName;
        if (injectionPoint.getMember() instanceof Field) {
            propertyName = injectionPoint.getMember().getName();
        } else if (injectionPoint.getMember() instanceof Method) {
            propertyName = getPropertyName((Method) injectionPoint.getMember());
            if (propertyName == null) {
                throw WeldLogger.ROOT_LOGGER.injectionPointNotAJavabean((Method) injectionPoint.getMember());
            }
        } else {
            throw WeldLogger.ROOT_LOGGER.cannotInject(injectionPoint);
        }
        String className = injectionPoint.getMember().getDeclaringClass().getName();
        return RESOURCE_LOOKUP_PREFIX + "/" + className + "/" + propertyName;
    }


    public static String getEjbBindLocation(InjectionPoint injectionPoint) {
        EJB ejb = getResourceAnnotated(injectionPoint).getAnnotation(EJB.class);
        String mappedName = ejb.mappedName();
        if (!mappedName.equals("")) {
            return mappedName;
        }
        String name = ejb.name();
        if (!name.equals("")) {
            return RESOURCE_LOOKUP_PREFIX + "/" + name;
        }
        String propertyName;
        if (injectionPoint.getMember() instanceof Field) {
            propertyName = injectionPoint.getMember().getName();
        } else if (injectionPoint.getMember() instanceof Method) {
            propertyName = getPropertyName((Method) injectionPoint.getMember());
            if (propertyName == null) {
                throw WeldLogger.ROOT_LOGGER.injectionPointNotAJavabean((Method) injectionPoint.getMember());
            }
        } else {
            throw WeldLogger.ROOT_LOGGER.cannotInject(injectionPoint);
        }
        String className = injectionPoint.getMember().getDeclaringClass().getName();
        return RESOURCE_LOOKUP_PREFIX + "/" + className + "/" + propertyName;
    }

    public static String getPropertyName(Method method) {
        String methodName = method.getName();
        if (methodName.matches("^(get).*") && method.getParameterTypes().length == 0) {
            return Introspector.decapitalize(methodName.substring(3));
        } else if (methodName.matches("^(is).*") && method.getParameterTypes().length == 0) {
            return Introspector.decapitalize(methodName.substring(2));
        } else if (methodName.matches("^(set).*") && method.getParameterTypes().length == 1) {
            return Introspector.decapitalize(methodName.substring(3));
        }
        return null;
    }

    public static String getPropertyName(Member member) {
        if (member instanceof Method) {
            return getPropertyName((Method) member);
        }
        return member.getName();
    }

    public static Annotated getResourceAnnotated(InjectionPoint injectionPoint) {

        if(injectionPoint instanceof ParameterInjectionPoint) {
            return ((ParameterInjectionPoint<?, ?>)injectionPoint).getAnnotated().getDeclaringCallable();
        }
        return injectionPoint.getAnnotated();
    }

}
