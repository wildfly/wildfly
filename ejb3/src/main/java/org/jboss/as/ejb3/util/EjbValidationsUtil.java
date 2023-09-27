/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;

/**
 * @author Romain Pelisse - romain@redhat.com
 */
public final class EjbValidationsUtil {

    private EjbValidationsUtil() {

    }

    /**
     * Verifies that the passed <code>mdbClass</code> meets the requirements set by the Enterprise Beans 3 spec about bean implementation
     * classes. The passed <code>mdbClass</code> must not be an interface and must be public and not final and not abstract. If
     * it fails any of these requirements then one of the {@code MdbValidityStatus} value is added to the returned collection.
     * An empty collection is returned if no violation is found.
     *
     * @param mdbClass The MDB class
     * @return a collection of violations represented by {@code MdbValidityStatus}
     */
    public static Collection<MdbValidityStatus> assertEjbClassValidity(final ClassInfo mdbClass) {
        Collection<MdbValidityStatus> mdbComplianceIssueList = new ArrayList<>(MdbValidityStatus.values().length);
        final String className = mdbClass.name().toString();
        verifyModifiers(className, mdbClass.flags(), mdbComplianceIssueList);
        for (MethodInfo method : mdbClass.methods()) {
            if ("onMessage".equals(method.name())) {
                verifyOnMessageMethod(className, method.flags(), mdbComplianceIssueList);
            }

            if ("finalize".equals(method.name())) {
                EjbLogger.DEPLOYMENT_LOGGER.mdbCantHaveFinalizeMethod(className);
                mdbComplianceIssueList.add(MdbValidityStatus.MDB_SHOULD_NOT_HAVE_FINALIZE_METHOD);
            }
        }
        return mdbComplianceIssueList;
    }

    private static void verifyModifiers(final String className, final short flags,
            final Collection<MdbValidityStatus> mdbComplianceIssueList) {
        // must *not* be an interface
        if (Modifier.isInterface(flags)) {
            EjbLogger.DEPLOYMENT_LOGGER.mdbClassCannotBeAnInterface(className);
            mdbComplianceIssueList.add(MdbValidityStatus.MDB_CANNOT_BE_AN_INTERFACE);
        }
        // bean class must be public, must *not* be abstract or final
        if (!Modifier.isPublic(flags) || Modifier.isAbstract(flags) || Modifier.isFinal(flags)) {
            EjbLogger.DEPLOYMENT_LOGGER.mdbClassMustBePublicNonAbstractNonFinal(className);
            mdbComplianceIssueList.add(MdbValidityStatus.MDB_CLASS_CANNOT_BE_PRIVATE_ABSTRACT_OR_FINAL);
        }
    }

    private static void verifyOnMessageMethod(final String className, final short methodsFlags,
            final Collection<MdbValidityStatus> mdbComplianceIssueList) {
        if (Modifier.isFinal(methodsFlags)) {
            EjbLogger.DEPLOYMENT_LOGGER.mdbOnMessageMethodCantBeFinal(className);
            mdbComplianceIssueList.add(MdbValidityStatus.MDB_ON_MESSAGE_METHOD_CANT_BE_FINAL);
        }
        if (Modifier.isStatic(methodsFlags)) {
            EjbLogger.DEPLOYMENT_LOGGER.mdbOnMessageMethodCantBeStatic(className);
            mdbComplianceIssueList.add(MdbValidityStatus.MDB_ON_MESSAGE_METHOD_CANT_BE_STATIC);
        }
        if (Modifier.isPrivate(methodsFlags)) {
            EjbLogger.DEPLOYMENT_LOGGER.mdbOnMessageMethodCantBePrivate(className);
            mdbComplianceIssueList.add(MdbValidityStatus.MDB_ON_MESSAGE_METHOD_CANT_BE_PRIVATE);
        }
    }

    public static void verifyEjbClassAndDefaultConstructor(final Constructor<?> ctor, Class<?> enclosingClass,
            boolean noInterface, String componentName, String componentClassname, int modifiers)
            throws DeploymentUnitProcessingException {
        if (ctor == null && noInterface) {
            // we only validate this for no interface views
            throw EjbLogger.ROOT_LOGGER.ejbMustHavePublicDefaultConstructor(componentName, componentClassname);
        }
        if (enclosingClass != null) {
            throw EjbLogger.ROOT_LOGGER.ejbMustNotBeInnerClass(componentName, componentClassname);
        }
        if (!Modifier.isPublic(modifiers)) {
            throw EjbLogger.ROOT_LOGGER.ejbMustBePublicClass(componentName, componentClassname);
        }
        if (Modifier.isFinal(modifiers)) {
            throw EjbLogger.ROOT_LOGGER.ejbMustNotBeFinalClass(componentName, componentClassname);
        }
    }

    public static boolean verifyEjbPublicMethodAreNotFinalNorStatic(Method[] methods, String classname) {
        boolean isEjbCompliant = true;
        for (Method method : methods) {
            if (Object.class != method.getDeclaringClass() && !EjbValidationsUtil.verifyMethodIsNotFinalNorStatic(method, classname))
                isEjbCompliant = false;
        }
        return isEjbCompliant;
    }

    public static boolean verifyMethodIsNotFinalNorStatic(Method method, String classname) {
        boolean isMethodCompliant = true;
        if (Modifier.isStatic(method.getModifiers()) || Modifier.isFinal(method.getModifiers())) {
            EjbLogger.ROOT_LOGGER.ejbMethodMustNotBeFinalNorStatic(classname, method.getName());
            isMethodCompliant = false;
        }
        return isMethodCompliant;
    }
}
