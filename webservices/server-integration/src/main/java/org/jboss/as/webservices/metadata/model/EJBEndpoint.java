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
import org.jboss.as.ejb3.security.service.EJBViewMethodSecurityAttributesService;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class EJBEndpoint extends AbstractEndpoint {

   private final SessionBeanComponentDescription ejbMD;
   private final Set<String> declaredSecurityRoles;
   private final String authMethod;
   private final boolean secureWsdlAccess;
   private final String transportGuarantee;
   private final String realmName;

   public EJBEndpoint(final SessionBeanComponentDescription ejbMD, final ServiceName viewName, final Set<String> declaredSecurityRoles, final String authMethod, final String realmName, final boolean secureWsdlAccess, final String transportGuarantee) {
       super(ejbMD.getComponentName(), ejbMD.getComponentClassName(), viewName);
       this.ejbMD = ejbMD;
       this.declaredSecurityRoles = declaredSecurityRoles;
       this.authMethod = authMethod;
       this.realmName = realmName;
       this.secureWsdlAccess = secureWsdlAccess;
       this.transportGuarantee = transportGuarantee;
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

   public Set<String> getDeclaredSecurityRoles() {
       return declaredSecurityRoles;
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

   public String getRealmName() {
       return realmName;
   }

   public ServiceName getEJBViewMethodSecurityAttributesService() {
       return EJBViewMethodSecurityAttributesService.getServiceName(ejbMD.getApplicationName(), ejbMD.getModuleName(), ejbMD.getEJBName(), getClassName());
   }

}
