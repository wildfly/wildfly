package org.jboss.as.test.iiop.basic;

import org.jboss.ejb.client.EJBHandle;

import javax.ejb.Handle;

/**
 * @author Stuart Douglas
 */
public class HandleWrapper {

    private final Handle handle;

    public HandleWrapper(Handle handle) {
        this.handle = handle;
    }

    public Handle getHandle() {
        return handle;
    }
}
