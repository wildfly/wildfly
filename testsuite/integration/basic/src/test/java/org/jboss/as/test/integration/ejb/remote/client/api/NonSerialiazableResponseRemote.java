package org.jboss.as.test.integration.ejb.remote.client.api;

import javax.ejb.Remote;

/**
 * @author Stuart Douglas
 */
@Remote
public interface NonSerialiazableResponseRemote {

    Object nonSerializable();

    String serializable();

}
