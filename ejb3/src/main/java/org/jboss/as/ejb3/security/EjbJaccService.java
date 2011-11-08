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

package org.jboss.as.ejb3.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.security.jacc.EJBMethodPermission;
import javax.security.jacc.EJBRoleRefPermission;
import javax.security.jacc.PolicyConfiguration;
import javax.security.jacc.PolicyContextException;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleConfiguration;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ejb3.EJBMethodIdentifier;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.EJBViewDescription;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.security.service.JaccService;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.metadata.ejb.spec.MethodInterfaceType;

/**
 * A service that creates JACC permissions for a ejb deployment
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 * @author Scott.Stark@jboss.org
 * @author Anil.Saldhana@jboss.org
 */
public class EjbJaccService extends JaccService<EEModuleConfiguration> {

    public EjbJaccService(String contextId, EEModuleConfiguration metaData, Boolean standalone) {
        super(contextId, metaData, standalone);
    }

    /** {@inheritDoc} */
    @Override
    public void createPermissions(EEModuleConfiguration metaData, PolicyConfiguration policyConfiguration)
            throws PolicyContextException {
        Collection<ComponentConfiguration> configurations = metaData.getComponentConfigurations();
        if (configurations != null) {
            for (ComponentConfiguration configuration : configurations) {
                ComponentDescription componentDescription = configuration.getComponentDescription();
                if (componentDescription instanceof EJBComponentDescription) {
                    EJBComponentDescription component = EJBComponentDescription.class.cast(componentDescription);
                    String ejbClassName = component.getEJBClassName();
                    String ejbName = component.getEJBName();
                    // Process the exclude-list and method-permission
                    // check class level
                    boolean denyOnAllViews = true;
                    boolean permitOnAllViews = true;
                    List<EJBMethodPermission> permissions = new ArrayList<EJBMethodPermission>();
                    List<EJBMethodPermission> uncheckedPermissions = new ArrayList<EJBMethodPermission>();
                    for (ViewDescription view : component.getViews()) {
                        String viewClassName = view.getViewClassName();
                        if (!component.isDenyAllApplicableToClass(viewClassName, ejbClassName)) {
                            denyOnAllViews = false;
                        } else {
                            EJBViewDescription ejbView = EJBViewDescription.class.cast(view);
                            MethodInterfaceType type = getMethodInterfaceType(ejbView.getMethodIntf());
                            EJBMethodPermission p = new EJBMethodPermission(ejbName, null, type.name(), null);
                            permissions.add(p);
                        }
                        if (!component.isPermitAllApplicableToClass(viewClassName, ejbClassName)) {
                            permitOnAllViews = false;
                        } else {
                            EJBViewDescription ejbView = EJBViewDescription.class.cast(view);
                            MethodInterfaceType type = getMethodInterfaceType(ejbView.getMethodIntf());
                            EJBMethodPermission p = new EJBMethodPermission(ejbName, null, type.name(), null);
                            uncheckedPermissions.add(p);
                        }
                        Set<String> roles = component.getRolesAllowedForClass(viewClassName, ejbClassName);
                        for (String role : roles) {
                            policyConfiguration.addToRole(role, new EJBMethodPermission(ejbName, null, null, null));
                        }

                        // check method level
                        Collection<EJBMethodIdentifier> methods = component.getDenyAllMethodsForView(viewClassName);
                        for (EJBMethodIdentifier method : methods) {
                            MethodIdentifier identifier = method.getMethodIdentifier();
                            EJBViewDescription ejbView = EJBViewDescription.class.cast(view);
                            MethodInterfaceType type = getMethodInterfaceType(ejbView.getMethodIntf());
                            EJBMethodPermission p = new EJBMethodPermission(ejbName, identifier.getName(), type.name(),
                                    identifier.getParameterTypes());
                            policyConfiguration.addToExcludedPolicy(p);
                        }
                        methods = component.getPermitAllMethodsForView(viewClassName);
                        for (EJBMethodIdentifier method : methods) {
                            MethodIdentifier identifier = method.getMethodIdentifier();
                            EJBViewDescription ejbView = EJBViewDescription.class.cast(view);
                            MethodInterfaceType type = getMethodInterfaceType(ejbView.getMethodIntf());
                            EJBMethodPermission p = new EJBMethodPermission(ejbName, identifier.getName(), type.name(),
                                    identifier.getParameterTypes());
                            policyConfiguration.addToUncheckedPolicy(p);
                        }
                        Map<EJBMethodIdentifier, Set<String>> rolesMap = component.getRolesAllowed(viewClassName);
                        for (Entry<EJBMethodIdentifier, Set<String>> entry : rolesMap.entrySet()) {
                            MethodIdentifier identifier = entry.getKey().getMethodIdentifier();
                            EJBViewDescription ejbView = EJBViewDescription.class.cast(view);
                            MethodInterfaceType type = getMethodInterfaceType(ejbView.getMethodIntf());
                            for (String role : entry.getValue()) {
                                EJBMethodPermission p = new EJBMethodPermission(ejbName, identifier.getName(), type.name(),
                                        identifier.getParameterTypes());
                                policyConfiguration.addToRole(role, p);
                            }
                        }
                    }
                    // if deny is on all views, we add permission with null as the interface
                    if (denyOnAllViews) {
                        permissions = new ArrayList<EJBMethodPermission>();
                        permissions.add(new EJBMethodPermission(ejbName, null, null, null));
                    }

                    // add exclude-list permissions
                    for (EJBMethodPermission ejbMethodPermission : permissions) {
                        policyConfiguration.addToExcludedPolicy(ejbMethodPermission);
                    }

                    // if permit is on all views, we add permission with null as the interface
                    if (permitOnAllViews) {
                        uncheckedPermissions = new ArrayList<EJBMethodPermission>();
                        uncheckedPermissions.add(new EJBMethodPermission(ejbName, null, null, null));
                    }

                    // add method-permission permissions
                    for (EJBMethodPermission ejbMethodPermission : uncheckedPermissions) {
                        policyConfiguration.addToUncheckedPolicy(ejbMethodPermission);
                    }

                    // Process the security-role-ref
                    Map<String, Collection<String>> securityRoles = component.getSecurityRoleLinks();
                    for (Entry<String, Collection<String>> entry : securityRoles.entrySet()) {
                        String roleName = entry.getKey();
                        for (String roleLink : entry.getValue()) {
                            EJBRoleRefPermission p = new EJBRoleRefPermission(ejbName, roleName);
                            policyConfiguration.addToRole(roleLink, p);
                        }
                    }

                    /*
                     * Special handling of stateful session bean getEJBObject due how the stateful session handles acquire the
                     * proxy by sending an invocation to the ejb container.
                     */
                    if (component instanceof SessionBeanComponentDescription) {
                        SessionBeanComponentDescription session = SessionBeanComponentDescription.class.cast(component);
                        if (session.isStateful()) {
                            EJBMethodPermission p = new EJBMethodPermission(ejbName, "getEJBObject", "Home", null);
                            policyConfiguration.addToUncheckedPolicy(p);
                        }
                    }
                }
            }
        }
    }

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

}
