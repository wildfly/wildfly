package org.jboss.as.test.integration.naming.remote.multiple;

import jakarta.ejb.Remote;

@Remote
public interface MyEjb {
    String doIt();
}
