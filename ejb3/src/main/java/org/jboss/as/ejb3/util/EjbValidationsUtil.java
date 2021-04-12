/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * Returns true if the passed <code>mdbClass</code> meets the requirements set by the Enterprise Beans 3 spec about bean implementation
     * classes. The passed <code>mdbClass</code> must not be an interface and must be public and not final and not abstract. If
     * it passes these requirements then this method returns true. Else it returns false.
     *
     * @param mdbClass The MDB class
     * @return
     * @throws DeploymentUnitProcessingException
     */
    public static Collection<MdbValidityStatus> assertEjbClassValidity(final ClassInfo mdbClass)
            throws DeploymentUnitProcessingException {
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
            if (Modifier.isPublic(method.getModifiers()) && !EjbValidationsUtil.verifyMethodIsNotFinalNorStatic(method, classname))
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

    public static List<Method> getBusinessMethods(Class<?> componentClass) {
        Method[] componentDeclaredMethods = componentClass.getDeclaredMethods();
        List<Method> businessMethodList = new ArrayList<>(componentDeclaredMethods.length);
        if (componentDeclaredMethods.length > 0) {
            Map<String, List<Method>> interfacesMethodIndexedByName = indexAllMethodsFromInterfaces(componentClass
                    .getInterfaces());
            if (!interfacesMethodIndexedByName.isEmpty()) {
                for (Method method : componentClass.getDeclaredMethods()) {
                    if (interfacesMethodIndexedByName.containsKey(method.getName())) {
                        addMethodIfMatchesAnInterfaceMethod(interfacesMethodIndexedByName.get(method.getName()), businessMethodList, method);
                    }
                }
            }
        }
        return businessMethodList;
    }

    private static void addMethodIfMatchesAnInterfaceMethod(List<Method> businessMethods, List<Method> businessMethodList,
            Method method) {
        for (Method biznessMethod : businessMethods) {
            if ( hasSameSignature(biznessMethod, method) )
                businessMethodList.add(method);
        }
    }

    private static boolean hasSameSignature(Method left, Method right) {
        return (left.getName().equals(right.getName()) && left.getReturnType().equals(right.getReturnType())
                && left.getParameterCount() == right.getParameterCount() && left.getParameters().equals(right.getParameters()));
    }

    private static Map<String, List<Method>> indexAllMethodsFromInterfaces(Class<?>[] classes) {
        Map<String, List<Method>> businessMethods = new HashMap<>();
        for (@SuppressWarnings("rawtypes")
        Class clazz : classes)
            for (Method m : clazz.getDeclaredMethods())
                addMethodToListIfMatchesInterfacesMethod(m, businessMethods);
        return businessMethods;
    }

    private static void addMethodToListIfMatchesInterfacesMethod(Method m, Map<String, List<Method>> map) {
        if (map.containsKey(m.getName()))
            map.get(m.getName()).add(m);
        else {
            List<Method> methods = new ArrayList<>(1);
            methods.add(m);
            map.put(m.getName(), methods);
        }
    }
}
