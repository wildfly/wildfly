/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.component;

import static org.jboss.as.server.deployment.Attachments.MODULE;
import static org.jboss.as.server.deployment.Attachments.REFLECTION_INDEX;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.msc.value.Value;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Eduardo Martins
 */
public final class FieldInjectionTarget extends InjectionTarget {


    /**
     *
     * @param className
     * @param name
     * @param fieldType
     */
    public FieldInjectionTarget(final String className, final String name, final String fieldType) {
        super(className, name, fieldType);
    }

    @Override
    public boolean isStatic(DeploymentUnit deploymentUnit) throws DeploymentUnitProcessingException {
        return Modifier.isStatic(getField(deploymentUnit).getModifiers());
    }

    public InterceptorFactory createInjectionInterceptorFactory(final Object targetContextKey, final Object valueContextKey, final Value<ManagedReferenceFactory> factoryValue, final DeploymentUnit deploymentUnit, final boolean optional) throws DeploymentUnitProcessingException {
        return new ManagedReferenceFieldInjectionInterceptorFactory(targetContextKey, valueContextKey, factoryValue, getField(deploymentUnit), optional);
    }

    private Field getField(final DeploymentUnit deploymentUnit) throws DeploymentUnitProcessingException {
        final String name = getName();
        final String className = getClassName();
        final Module module = deploymentUnit.getAttachment(MODULE);
        final ModuleClassLoader classLoader = module.getClassLoader();
        final DeploymentReflectionIndex reflectionIndex = deploymentUnit.getAttachment(REFLECTION_INDEX);
        final ClassReflectionIndex classIndex;
        try {
            classIndex = reflectionIndex.getClassIndex(Class.forName(className, false, classLoader));
        } catch (ClassNotFoundException e) {
            throw new DeploymentUnitProcessingException(e);
        }
        final Field field = classIndex.getField(name);
        if (field == null) {
            throw EeLogger.ROOT_LOGGER.fieldNotFound(name);
        }
        return field;
    }
}
