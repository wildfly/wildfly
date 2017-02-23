/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.security;

import static java.security.AccessController.doPrivileged;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.Policy;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.security.jacc.EJBMethodPermission;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.metadata.ejb.spec.MethodInterfaceType;
import org.jboss.metadata.javaee.spec.SecurityRolesMetaData;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class JaccInterceptor implements Interceptor {

    private final String viewClassName;
    private final Method viewMethod;

    public JaccInterceptor(String viewClassName, Method viewMethod) {
        this.viewClassName = viewClassName;
        this.viewMethod = viewMethod;
    }

    @Override
    public Object processInvocation(InterceptorContext context) throws Exception {
        Component component = context.getPrivateData(Component.class);

        if (component instanceof EJBComponent == false) {
            throw EjbLogger.ROOT_LOGGER.unexpectedComponent(component, EJBComponent.class);
        }

        Method invokedMethod = context.getMethod();
        ComponentView componentView = context.getPrivateData(ComponentView.class);
        String viewClassOfInvokedMethod = componentView.getViewClass().getName();

        // shouldn't really happen if the interceptor was setup correctly. But let's be safe and do a check
        if (!viewClassName.equals(viewClassOfInvokedMethod) || !viewMethod.equals(invokedMethod)) {
            throw EjbLogger.ROOT_LOGGER.failProcessInvocation(getClass().getName(), invokedMethod, viewClassOfInvokedMethod, viewMethod, viewClassName);
        }

        EJBComponent ejbComponent = (EJBComponent) component;

        if(WildFlySecurityManager.isChecking()) {
            try {
                AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
                    hasPermission(ejbComponent, componentView, invokedMethod);
                    return null;
                });
            } catch (PrivilegedActionException e) {
                throw e.getException();
            }
        } else {
            hasPermission(ejbComponent, componentView, invokedMethod);
        }

        // successful authorization, let the invocation proceed
        return context.proceed();
    }

    private EJBMethodPermission createEjbMethodPermission(Method invokedMethod, EJBComponent ejbComponent, MethodInterfaceType methodIntfType) {
        return new EJBMethodPermission(ejbComponent.getComponentName(), methodIntfType.name(), invokedMethod);
    }

    private void hasPermission(EJBComponent ejbComponent, ComponentView componentView, Method method) {
        MethodInterfaceType methodIntfType = getMethodInterfaceType(componentView.getPrivateData(MethodIntf.class));
        EJBMethodPermission permission = createEjbMethodPermission(method, ejbComponent, methodIntfType);
        ProtectionDomain domain = new ProtectionDomain (componentView.getProxyClass().getProtectionDomain().getCodeSource(), null, null, getGrantedRoles(ejbComponent));
        Policy policy = WildFlySecurityManager.isChecking() ? doPrivileged((PrivilegedAction<Policy>) Policy::getPolicy) : Policy.getPolicy();
        if (!policy.implies(domain, permission)) {
            throw EjbLogger.ROOT_LOGGER.invocationOfMethodNotAllowed(method,ejbComponent.getComponentName());
        }
    }

    /**
     * <p>
     * Gets the {@code MethodInterfaceType} that corresponds to the specified {@code MethodIntf}.
     * </p>
     *
     * @param viewType the {@code MethodIntf} type to be converted.
     * @return the converted type or {@code null} if the type cannot be converted.
     */
    protected MethodInterfaceType getMethodInterfaceType(MethodIntf viewType) {
        switch (viewType) {
            case HOME:
                return MethodInterfaceType.Home;
            case LOCAL_HOME:
                return MethodInterfaceType.LocalHome;
            case SERVICE_ENDPOINT:
                return MethodInterfaceType.ServiceEndpoint;
            case LOCAL:
                return MethodInterfaceType.Local;
            case REMOTE:
                return MethodInterfaceType.Remote;
            case TIMER:
                return MethodInterfaceType.Timer;
            case MESSAGE_ENDPOINT:
                return MethodInterfaceType.MessageEndpoint;
            default:
                return null;
        }
    }

    /**
     * Returns an array of {@link Principal} representing the roles associated with the identity
     * invoking the EJB. This method will check performs checks against run as identities in order to
     * resolve the correct set of roles to be granted.
     *
     * @param ejbComponent the ejb component
     * @return an array of {@link Principal} representing the roles associated with the identity
     */
    public static Principal[] getGrantedRoles(EJBComponent ejbComponent) {
        EJBSecurityMetaData securityMetaData = ejbComponent.getSecurityMetaData();

        if (securityMetaData == null) {
            return new Principal[] {};
        }

        Set<String> roles = new HashSet<>();

        SecurityIdentity identity = ejbComponent.getIncomingRunAsIdentity();

        if (identity == null) {
            identity = ejbComponent.getSecurityDomain().getCurrentSecurityIdentity();
        }

        identity.getRoles("ejb").forEach(roles::add);

        SecurityRolesMetaData securityRoles = securityMetaData.getSecurityRoles();

        if (securityRoles != null && securityRoles.getPrincipalVersusRolesMap() != null) {
            Map<String, Set<String>> principalVersusRolesMap = securityRoles.getPrincipalVersusRolesMap();
            roles.addAll(principalVersusRolesMap.getOrDefault(identity.getPrincipal().getName(), Collections.emptySet()));
        }

        return roles.stream().map((Function<String, Principal>) roleName -> (Principal) () -> roleName).toArray(Principal[]::new);
    }
}
