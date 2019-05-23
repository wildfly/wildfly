package org.jboss.as.test.integration.ejb.remote.client.api.moreconnection;

import javax.ejb.Remote;

@Remote
public interface HelloBeanRemote {

    public final String VALUE = "Hello, test is OK.";

    public String hello();

}
