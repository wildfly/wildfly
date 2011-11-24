package org.jboss.as.test.integration.ejb.interceptor.environment;

import javax.ejb.Remote;

@Remote
public interface MyTestRemoteB {
    boolean doit();
}
