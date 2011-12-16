/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.webservices.metadata.model;

import java.util.Set;

import org.jboss.as.ee.component.DeploymentDescriptorEnvironment;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class EJBEndpoint extends AbstractEndpoint {

   public static final String EJB_COMPONENT_VIEW_NAME = EJBEndpoint.class.getPackage().getName() + "EjbComponentViewName";
   private final SessionBeanComponentDescription ejbMD;
   private final ServiceName viewName;
   private final Set<String> securityRoles;
   private final String authMethod;
   private final boolean secureWsdlAccess;
   private final String transportGuarantee;

   public EJBEndpoint(final SessionBeanComponentDescription ejbMD, final ServiceName viewName, final Set<String> securityRoles, final String authMethod, final boolean secureWsdlAccess, final String transportGuarantee) {
       super(ejbMD.getComponentName(), ejbMD.getComponentClassName());
       this.ejbMD = ejbMD;
       this.viewName = viewName;
       this.securityRoles = securityRoles;
       this.authMethod = authMethod;
       this.secureWsdlAccess = secureWsdlAccess;
       this.transportGuarantee = transportGuarantee;
   }

   public ServiceName getComponentViewName() {
       return viewName;
   }

   public ServiceName getContextServiceName() {
       return ejbMD.getContextServiceName();
   }

   public DeploymentDescriptorEnvironment getDeploymentDescriptorEnvironment() {
       return ejbMD.getDeploymentDescriptorEnvironment();
   }

   public String getSecurityDomain() {
       return ejbMD.getSecurityDomain();
   }

   public Set<String> getSecurityRoles() {
       return securityRoles;
   }

   public String getAuthMethod() {
       return authMethod;
   }

   public boolean isSecureWsdlAccess() {
       return secureWsdlAccess;
   }

   public String getTransportGuarantee() {
       return transportGuarantee;
   }

}
