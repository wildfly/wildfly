package org.jboss.as.ee.component;

import org.jboss.as.naming.ConstructorManagedReferenceFactory;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * @author Stuart Douglas
 */
public class ReflectiveClassIntrospector implements EEClassIntrospector, Service<EEClassIntrospector> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ee", "reflectiveClassIntrospector");

    @Override
    public ManagedReferenceFactory createFactory(Class<?> clazz) {
        try {
            return new ConstructorManagedReferenceFactory(clazz.getDeclaredConstructor());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void start(StartContext startContext) throws StartException {
    }

    @Override
    public void stop(StopContext stopContext) {
    }

    @Override
    public EEClassIntrospector getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }
}
