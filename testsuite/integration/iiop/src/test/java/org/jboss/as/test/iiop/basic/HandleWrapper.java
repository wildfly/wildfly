package org.jboss.as.test.iiop.basic;

import java.io.Serializable;

import javax.ejb.Handle;

/**
 * @author Stuart Douglas
 */
public class HandleWrapper implements Serializable{

    private static final long serialVersionUID = 1L;
    private final Handle handle;

    public HandleWrapper(Handle handle) {
        this.handle = handle;
    }

    public Handle getHandle() {
        return handle;
    }
}
