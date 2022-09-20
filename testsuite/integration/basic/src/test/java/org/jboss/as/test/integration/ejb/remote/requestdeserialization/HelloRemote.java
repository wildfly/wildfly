package org.jboss.as.test.integration.ejb.remote.requestdeserialization;

import jakarta.ejb.Remote;

@Remote
public interface HelloRemote {

    Response sayHello(Request request);

}
