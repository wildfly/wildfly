package org.jboss.as.ee.concurrent.handle;

import java.io.ObjectStreamException;

/**
 * A context handle without invocation context to set.
 * @author Eduardo Martins
 */
public class NullContextHandle implements SetupContextHandle, ResetContextHandle {

    public static final NullContextHandle INSTANCE = new NullContextHandle();

    private NullContextHandle() {

    }

    @Override
    public ResetContextHandle setup() throws IllegalStateException {
        return this;
    }

    @Override
    public void reset() {

    }

    @Override
    public String getFactoryName() {
        return "NULL";
    }

    protected Object readResolve() throws ObjectStreamException {
        return INSTANCE;
    }
}
