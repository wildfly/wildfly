package org.jboss.as.test.integration.ejb.stateless.callerprincipal;

public interface TestEJB3Remote {

   boolean isCallerInRole(String role) throws Exception;

}