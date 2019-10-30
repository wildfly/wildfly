package org.jboss.as.test.integration.legacy.ejb.remote.client.api;

import javax.ejb.Stateless;

/**
 * @author Stuart Douglas
 */
@Stateless
public class NonSerializableResponseEjb implements NonSerialiazableResponseRemote {


    @Override
    public Object nonSerializable() {
        throw new NonSerialiableException();
    }

    @Override
    public String serializable() {
        return "hello";
    }
}
