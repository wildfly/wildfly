/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.utils;

import org.jboss.as.ee.component.FixedInjectionSource;
import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.ee.component.LookupInjectionSource;
import org.jboss.as.ee.component.OptionalLookupInjectionSource;
import org.jboss.as.naming.ImmediateManagedReference;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;

import javax.naming.Context;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Locale;

import static org.jboss.as.ee.EeMessages.MESSAGES;

/**
 * Utility class for injection framework.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author Eduardo Martins
 */
public final class InjectionUtils {

    private InjectionUtils() {
        // forbidden instantiation
    }

    public static AccessibleObject getInjectionTarget(final String injectionTargetClassName, final String injectionTargetName, final ClassLoader classLoader, final DeploymentReflectionIndex deploymentReflectionIndex) throws DeploymentUnitProcessingException {
        final Class<?> injectionTargetClass;
        try {
            injectionTargetClass = classLoader.loadClass(injectionTargetClassName);
        } catch (ClassNotFoundException e) {
            throw MESSAGES.cannotLoad(e, injectionTargetClassName);
        }
        final ClassReflectionIndex<?> index = deploymentReflectionIndex.getClassIndex(injectionTargetClass);
        String methodName = "set" + injectionTargetName.substring(0, 1).toUpperCase(Locale.ENGLISH) + injectionTargetName.substring(1);

        boolean methodFound = false;
        Method method = null;
        Field field = null;
        Class<?> current = injectionTargetClass;
        while (current != Object.class && current != null && !methodFound) {
            final Collection<Method> methods = index.getAllMethods(methodName);
            for (Method m : methods) {
                if (m.getParameterTypes().length == 1) {
                    if (m.isBridge() || m.isSynthetic()) {
                        continue;
                    }
                    if (methodFound) {
                        throw MESSAGES.multipleSetterMethodsFound(injectionTargetName, injectionTargetClassName);
                    }
                    methodFound = true;
                    method = m;
                }
            }
            current = current.getSuperclass();
        }
        if (method == null) {
            current = injectionTargetClass;
            while (current != Object.class && current != null && field == null) {
                field = index.getField(injectionTargetName);
                if (field != null) {
                    break;
                }
                current = current.getSuperclass();
            }
        }
        if (field == null && method == null) {
            throw MESSAGES.cannotResolveInjectionPoint(injectionTargetName, injectionTargetClassName);
        }

        return field != null ? field : method;
    }

    private static final String JAVAX_NAMING_CONTEXT_INJECTION_TYPE = Context.class.getName();
    private static final String JAVA_NET_URL_INJECTION_TYPE = URL.class.getName();

    /**
     * Retrieves the injection source for a specific jndi name and target type
     * @param name the jndi name for the source
     * @param type the type of the injection target
     * @return
     */
    public static InjectionSource getInjectionSource(final String name, String type) throws DeploymentUnitProcessingException {
        if(type.equals(JAVA_NET_URL_INJECTION_TYPE)) {
            final ManagedReferenceFactory managedReferenceFactory = new ManagedReferenceFactory() {
                @Override
                public ManagedReference getReference() {
                    try {
                        return new ImmediateManagedReference(new URL(name));
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            try {
                return new FixedInjectionSource(managedReferenceFactory,new URL(name));
            } catch (MalformedURLException e) {
                throw MESSAGES.cannotParseResourceRefUri(e, name);
            }
        } else {
            return JAVAX_NAMING_CONTEXT_INJECTION_TYPE.equals(type) ? new OptionalLookupInjectionSource(name) : new LookupInjectionSource(name);
        }
    }

}
