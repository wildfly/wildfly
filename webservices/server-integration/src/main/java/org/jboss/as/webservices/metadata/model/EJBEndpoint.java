/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
       return ejbMD.getResolvedSecurityDomain();
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
