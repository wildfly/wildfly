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

package org.jboss.as.ee.component;

import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndexUtil;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.msc.value.Value;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;

import static org.jboss.as.server.deployment.Attachments.MODULE;
import static org.jboss.as.server.deployment.Attachments.REFLECTION_INDEX;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class MethodInjectionTarget extends InjectionTarget {

    public MethodInjectionTarget(final String className, final String name, final String paramType) {
        super(className, name, paramType);
    }

    public InterceptorFactory createInjectionInterceptorFactory(final Object targetContextKey, final Object valueContextKey, final Value<ManagedReferenceFactory> factoryValue, final DeploymentUnit deploymentUnit) throws DeploymentUnitProcessingException {
        final String name = getName();
        final String className = getClassName();
        final String paramType = getDeclaredValueClassName();
        final Module module = deploymentUnit.getAttachment(MODULE);
        final ModuleClassLoader classLoader = module.getClassLoader();
        final DeploymentReflectionIndex reflectionIndex = deploymentUnit.getAttachment(REFLECTION_INDEX);
        final ClassReflectionIndex<?> classIndex;
        try {
            classIndex = reflectionIndex.getClassIndex(Class.forName(className, false, classLoader));
        } catch (ClassNotFoundException e) {
            throw new DeploymentUnitProcessingException(e);
        }
        Collection<Method> methods;
        if (paramType != null) {
            // find the methods with the specific name and the param types
            methods = ClassReflectionIndexUtil.findMethods(reflectionIndex, classIndex, name, paramType);
        } else {
            // find all the methods with the specific name and which accept just 1 parameter.
            methods = ClassReflectionIndexUtil.findAllMethods(reflectionIndex, classIndex, name, 1);
        }
        Iterator<Method> iterator = methods.iterator();
        if (!iterator.hasNext()) {
            throw new DeploymentUnitProcessingException("No matching method found for method " + name +
                    paramType != null ? "(" + paramType + ")" : "" +
                    " on " + className);
        }
        Method method = iterator.next();
        if (iterator.hasNext()) {
            throw new DeploymentUnitProcessingException("More than one matching method found for method '" + name +
                    paramType != null ? "(" + paramType + ")" : "" +
                    " on " + className);
        }
        return new ManagedReferenceMethodInjectionInterceptorFactory(targetContextKey, valueContextKey, factoryValue, method);
    }
}
