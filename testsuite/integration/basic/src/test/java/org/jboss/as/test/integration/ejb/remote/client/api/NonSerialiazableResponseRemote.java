package org.jboss.as.test.integration.ejb.remote.client.api;

import jakarta.ejb.Remote;

/**
 * @author Stuart Douglas
 */
@Remote
public interface NonSerialiazableResponseRemote {

    Object nonSerializable();

    String serializable();

}
