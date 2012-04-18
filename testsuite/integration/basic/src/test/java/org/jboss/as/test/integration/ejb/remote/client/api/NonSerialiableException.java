package org.jboss.as.test.integration.ejb.remote.client.api;

/**
 * @author Stuart Douglas
 */
public class NonSerialiableException  extends RuntimeException {

    public Object nonSerialiable = new Object();

}
