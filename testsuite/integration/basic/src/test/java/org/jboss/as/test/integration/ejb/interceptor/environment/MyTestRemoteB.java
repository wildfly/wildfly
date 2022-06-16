package org.jboss.as.test.integration.ejb.interceptor.environment;

import jakarta.ejb.Remote;

@Remote
public interface MyTestRemoteB {
    boolean doit();
}
