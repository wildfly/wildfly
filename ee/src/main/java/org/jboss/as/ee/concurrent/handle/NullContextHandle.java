package org.jboss.as.ee.concurrent.handle;

import java.io.ObjectStreamException;

/**
 * A context handle without invocation context to set.
 * @author Eduardo Martins
 */
public class NullContextHandle implements ContextHandle {

    public static final NullContextHandle INSTANCE = new NullContextHandle();

    private NullContextHandle() {

    }

    @Override
    public void setup() throws IllegalStateException {

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
