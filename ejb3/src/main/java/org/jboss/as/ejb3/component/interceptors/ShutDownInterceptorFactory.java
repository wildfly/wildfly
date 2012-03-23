package org.jboss.as.ejb3.component.interceptors;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import org.jboss.as.ejb3.EjbMessages;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;

/**
 * A per component interceptor that allows the EJB to shutdown gracefully.
 *
 * @author Stuart Douglas
 */
public class ShutDownInterceptorFactory implements InterceptorFactory {

    private static final int SHUTDOWN_FLAG = 1 << 31;
    private static final int INVERSE_SHUTDOWN_FLAG = ~SHUTDOWN_FLAG;

    private static final AtomicIntegerFieldUpdater<ShutDownInterceptorFactory> updater = AtomicIntegerFieldUpdater.newUpdater(ShutDownInterceptorFactory.class, "invocationCount");

    @SuppressWarnings("unused")
    private volatile int invocationCount;

    private final Object lock = new Object();

    private Interceptor interceptor = new Interceptor() {
        @Override
        public Object processInvocation(InterceptorContext context) throws Exception {

            int value;
            int oldValue;
            do {
                oldValue = invocationCount;
                if ((oldValue & SHUTDOWN_FLAG) != 0) {
                    throw EjbMessages.MESSAGES.componentIsShuttingDown();
                }
                value = oldValue + 1;
            } while (!updater.compareAndSet(ShutDownInterceptorFactory.this, oldValue, value));
            try {
                return context.proceed();
            } finally {
                do {
                    oldValue = invocationCount;
                    boolean shutDown = (oldValue & SHUTDOWN_FLAG) != 0;
                    int oldCount = oldValue & INVERSE_SHUTDOWN_FLAG;
                    value = oldCount - 1;
                    if(shutDown) {
                        value = value | SHUTDOWN_FLAG;
                    }
                } while (!updater.compareAndSet(ShutDownInterceptorFactory.this, oldValue, value));
                //if the count is zero and the component is shutting down
                if (value == SHUTDOWN_FLAG) {
                    synchronized (lock) {
                        lock.notifyAll();
                    }
                }
            }
        }
    };

    @Override
    public Interceptor create(InterceptorFactoryContext context) {
        return interceptor;
    }

    /**
     * Upon calling this method the EJB will be set to a shutdown state, and no further invocations will be allowed.
     * It will then wait for all active invocation to finish and then return.
     */
    public void shutdown() {
        int value;
        int oldValue;
        //set the shutdown bit
        do {
            oldValue = invocationCount;
            value = SHUTDOWN_FLAG | oldValue;
            //the component has already been shutdown
            if (oldValue == value) {
                return;
            }
        } while (!updater.compareAndSet(this, oldValue, value));

        synchronized (lock) {
            value = invocationCount;
            while (value != SHUTDOWN_FLAG) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                value = invocationCount;
                if((value & SHUTDOWN_FLAG) == 0) {
                    return; //component has been restarted
                }
            }
        }
    }

    public void start() {
        int value;
        int oldValue;
        //set the shutdown bit
        do {
            oldValue = invocationCount;
            value = INVERSE_SHUTDOWN_FLAG & oldValue;
            //the component has already been started
            if (oldValue == value) {
                return;
            }
        } while (!updater.compareAndSet(this, oldValue, value));
        synchronized (lock) {
            lock.notifyAll();
        }
    }
}
