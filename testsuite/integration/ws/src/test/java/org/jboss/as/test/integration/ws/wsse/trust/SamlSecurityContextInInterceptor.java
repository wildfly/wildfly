/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.wsse.trust;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.security.SimplePrincipal;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.security.DefaultSecurityContext;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.ws.security.wss4j.PolicyBasedWSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.security.SecurityDomainContext;

import javax.security.auth.Subject;
import java.security.Principal;
import java.util.Collections;

public class SamlSecurityContextInInterceptor extends WSS4JInInterceptor {

   public SamlSecurityContextInInterceptor() {
      super();
      getAfter().add(PolicyBasedWSS4JInInterceptor.class.getName());
   }

   @Override
   public void handleMessage(SoapMessage message) throws Fault {
      final SecurityContext securityContext = message.get(SecurityContext.class);
      final Principal principal = securityContext.getUserPrincipal();
      final String name = principal.getName();
      final Endpoint endpoint = message.getExchange().get(Endpoint.class);
      final SecurityDomainContext securityDomainContext = endpoint.getSecurityDomainContext();
      Principal simplePrincipal = new SimplePrincipal(name);
      Subject subject = new Subject(false, Collections.singleton(simplePrincipal), Collections.emptySet(), Collections.emptySet());
      securityDomainContext.pushSubjectContext(subject, simplePrincipal, null);
      message.put(SecurityContext.class, new DefaultSecurityContext(simplePrincipal, subject));
   }

}
