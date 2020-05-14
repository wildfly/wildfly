package org.jboss.as.test.integration.ejb.stateless.callerprincipal;

public interface TestEJB2Int {

    boolean isCallerInRole(String role) throws Exception;

}

