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

package org.jboss.as.ejb3.component.security;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ejb3.EJBMethodIdentifier;
import org.jboss.as.ejb3.component.EJBComponentDescription;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: Jaikiran Pai
 */
public class EJBSecurityMetaData {

    private final String ejbName;

    private final String ejbClassName;

    private final String securityDomain;

    private final String runAsRole;

    private final Set<String> declaredRoles;

    private final Map<String, Collection<Method>> accessDeniedMethodsOnView = new HashMap<String, Collection<Method>>();

    private final Map<String, Map<Method, Set<String>>> rolesAllowedForMethodsPerView = new HashMap<String, Map<Method, Set<String>>>();

    public EJBSecurityMetaData(final ComponentConfiguration componentConfiguration) {
        if (componentConfiguration.getComponentDescription() instanceof EJBComponentDescription == false) {
            throw new IllegalArgumentException(componentConfiguration.getComponentName() + " is not an EJB component");
        }
        final EJBComponentDescription ejbComponentDescription = (EJBComponentDescription) componentConfiguration.getComponentDescription();
        this.ejbClassName = ejbComponentDescription.getEJBClassName();
        this.ejbName = ejbComponentDescription.getEJBName();
        this.runAsRole = ejbComponentDescription.getRunAs();
        this.securityDomain = ejbComponentDescription.getSecurityDomain();
        final Set<String> roles = ejbComponentDescription.getDeclaredRoles();
        this.declaredRoles = roles == null ? Collections.<String>emptySet() : Collections.unmodifiableSet(roles);
        // process @DenyAll/exclude-list
        this.processDenyAll(componentConfiguration);
        // process @RolesAllowed/method-permission
        this.processRolesAllowed(componentConfiguration);

    }

    /**
     * Returns the roles that have been declared by the bean corresponding to this security metadata
     *
     * @return
     */
    public Set<String> getDeclaredRoles() {
        return declaredRoles;
    }

    /**
     * Returns the run-as role associated with this bean
     *
     * @return
     */
    public String getRunAs() {
        return this.runAsRole;
    }

    public String getSecurityDomain() {
        return this.securityDomain;
    }

    public Set<String> getAllowedRoles(final String viewClassName, final Method method) {
        final Map<Method, Set<String>> rolesAllowedPerView = this.rolesAllowedForMethodsPerView.get(viewClassName);
        if (rolesAllowedPerView == null) {
            return Collections.emptySet();
        }
        final Set<String> allowedRoles = rolesAllowedPerView.get(method);
        return allowedRoles == null ? Collections.<String>emptySet() : allowedRoles;
    }

    public boolean isMethodAccessDenied(final String viewClassName, final Method method) {
        final Collection<Method> accessDeniedMethods = this.accessDeniedMethodsOnView.get(viewClassName);
        if (accessDeniedMethods == null) {
            return false;
        }
        return accessDeniedMethods.contains(method);
    }

    private void processDenyAll(final ComponentConfiguration componentConfiguration) {
        List<ViewConfiguration> views = componentConfiguration.getViews();
        if (views == null || views.isEmpty()) {
            return;
        }
        final EJBComponentDescription ejbComponentDescription = (EJBComponentDescription) componentConfiguration.getComponentDescription();
        for (ViewConfiguration view : views) {
            final String viewClassName = view.getViewClass().getName();
            Collection<EJBMethodIdentifier> denyAllMethodsForView = ejbComponentDescription.getDenyAllMethodsForView(viewClassName);
            if (denyAllMethodsForView == null) {
                denyAllMethodsForView = Collections.emptySet();
            }
            final Method[] viewMethods = view.getProxyFactory().getCachedMethods();
            for (final Method viewMethod : viewMethods) {
                // TODO: proxy factory exposes non-public methods, is this a bug in the no-interface view?
                if (!Modifier.isPublic(viewMethod.getModifiers())) {
                    continue;
                }
                // find the component method corresponding to this view method
                final Method componentMethod = this.findComponentMethod(componentConfiguration, viewMethod);
                final EJBMethodIdentifier ejbMethodIdentifier = EJBMethodIdentifier.fromMethod(componentMethod);
                if (denyAllMethodsForView.contains(ejbMethodIdentifier)) {
                    this.applyDenyAll(viewClassName, componentMethod);
                    continue;
                }
                // check on class level

                final Class<?> declaringClass = componentMethod.getDeclaringClass();
                if (ejbComponentDescription.isDenyAllApplicableToClass(viewClassName, declaringClass.getName())) {
                    this.applyDenyAll(viewClassName, componentMethod);
                    continue;
                }
            }
        }

    }

    private void applyDenyAll(final String viewClassName, final Method componentMethod) {
        Collection<Method> accessDeniedMethods = this.accessDeniedMethodsOnView.get(viewClassName);
        if (accessDeniedMethods == null) {
            accessDeniedMethods = new HashSet<Method>();
            this.accessDeniedMethodsOnView.put(viewClassName, accessDeniedMethods);
        }
        accessDeniedMethods.add(componentMethod);
    }

    private void processRolesAllowed(final ComponentConfiguration componentConfiguration) {

        List<ViewConfiguration> views = componentConfiguration.getViews();
        if (views == null || views.isEmpty()) {
            return;
        }
        final EJBComponentDescription ejbComponentDescription = (EJBComponentDescription) componentConfiguration.getComponentDescription();
        for (ViewConfiguration view : views) {
            final String viewClassName = view.getViewClass().getName();
            final Method[] viewMethods = view.getProxyFactory().getCachedMethods();
            for (final Method viewMethod : viewMethods) {
                // TODO: proxy factory exposes non-public methods, is this a bug in the no-interface view?
                if (!Modifier.isPublic(viewMethod.getModifiers())) {
                    continue;
                }
                // find the component method corresponding to this view method
                final Method componentMethod = this.findComponentMethod(componentConfiguration, viewMethod);
                final EJBMethodIdentifier ejbMethodIdentifier = EJBMethodIdentifier.fromMethod(componentMethod);
                final Collection<String> rolesAllowed = ejbComponentDescription.getRolesAllowed(viewClassName, ejbMethodIdentifier);
                if (!rolesAllowed.isEmpty()) {
                    this.addRolesAllowed(viewClassName, componentMethod, rolesAllowed);
                    continue;
                }
                // there were no method level @RolesAllowed, so check on class level
                final Class<?> declaringClass = componentMethod.getDeclaringClass();
                final Collection<String> classLevelRolesAllowed = ejbComponentDescription.getRolesAllowedForClass(viewClassName, declaringClass.getName());
                if (!classLevelRolesAllowed.isEmpty()) {
                    this.addRolesAllowed(viewClassName, componentMethod, classLevelRolesAllowed);
                    continue;
                }
            }
        }

    }

    private void addRolesAllowed(final String viewClassName, final Method componentMethod, final Collection<String> roles) {
        Map<Method, Set<String>> perViewRoles = this.rolesAllowedForMethodsPerView.get(viewClassName);
        if (perViewRoles == null) {
            perViewRoles = new HashMap<Method, Set<String>>();
            this.rolesAllowedForMethodsPerView.put(viewClassName, perViewRoles);
        }
        Set<String> rolesAllowedForMethod = perViewRoles.get(componentMethod);
        if (rolesAllowedForMethod == null) {
            rolesAllowedForMethod = new HashSet<String>();
            perViewRoles.put(componentMethod, rolesAllowedForMethod);
        }
        rolesAllowedForMethod.addAll(roles);
    }

    private Method findComponentMethod(final ComponentConfiguration componentConfiguration, final Method viewMethod) {
        final Class<?> componentClass = componentConfiguration.getComponentClass();
        try {
            return componentClass.getMethod(viewMethod.getName(), viewMethod.getParameterTypes());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Method named " + viewMethod.getName() + " with params " + viewMethod.getParameterTypes()
                    + " not found on component class " + componentClass);
        }

    }
}
