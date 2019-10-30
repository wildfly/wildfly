package org.jboss.as.test.integration.legacy.ejb.remote.client.api;

/**
 * @author Stuart Douglas
 */
public class NonSerialiableException  extends RuntimeException {

    public Object nonSerialiable = new Object();

}
