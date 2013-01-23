package org.jboss.as.test.integration.ejb.injection.implicitDependsOn;

import javax.ejb.Stateless;

/**
 *
 *
 * @author Stuart Douglas
 */
@Stateless
public class ServiceEjb {

   public String sayHello() {
       return "hello";
   }

}
