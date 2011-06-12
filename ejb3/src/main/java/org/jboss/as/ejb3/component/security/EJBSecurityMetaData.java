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
import org.jboss.as.ejb3.component.EJBComponentDescription;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: Jaikiran Pai
 */
public class EJBSecurityMetaData {

    private final String ejbName;

    private final String ejbClassName;

    private final String securityDomain;

    private final String runAsRole;

    private final Set<String> declaredRoles;

    public EJBSecurityMetaData(final ComponentConfiguration componentConfiguration) {
        if (componentConfiguration.getComponentDescription() instanceof EJBComponentDescription == false) {
            throw new IllegalArgumentException(componentConfiguration.getComponentName()  + " is not an EJB component");
        }
        final EJBComponentDescription ejbComponentDescription = (EJBComponentDescription) componentConfiguration.getComponentDescription();
        this.ejbClassName = ejbComponentDescription.getEJBClassName();
        this.ejbName = ejbComponentDescription.getEJBName();
        this.runAsRole = ejbComponentDescription.getRunAs();
        this.securityDomain = ejbComponentDescription.getSecurityDomain();
        final Set<String> roles = ejbComponentDescription.getDeclaredRoles();
        this.declaredRoles = roles == null ? Collections.<String>emptySet() : Collections.unmodifiableSet(roles);

    }

    /**
     * Returns the roles that have been declared by the bean corresponding to this security metadata
     * @return
     */
    public Set<String> getDeclaredRoles() {
        return declaredRoles;
    }

    /**
     * Returns the run-as role associated with this bean
     * @return
     */
    public String getRunAs() {
        return this.runAsRole;
    }

    public String getSecurityDomain() {
        return this.securityDomain;
    }

    // TODO: Need to revisit this API. This is supposed to be usable from a interceptor for getting the
    // allowed roles on a method invocation.
    public Set<String> getAllowedRoles(final String viewClassName, final Method method) {
        return null;
    }
}
