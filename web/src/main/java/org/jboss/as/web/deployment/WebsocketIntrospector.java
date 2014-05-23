package org.jboss.as.web.deployment;

import org.apache.tomcat.websocket.ClassIntrospecter;
import org.apache.tomcat.websocket.InstanceFactory;
import org.apache.tomcat.websocket.InstanceHandle;
import org.jboss.as.ee.component.EEClassIntrospector;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;

/**
 * @author Stuart Douglas
 */
public class WebsocketIntrospector implements ClassIntrospecter {

    private final EEClassIntrospector classIntrospector;

    public WebsocketIntrospector(EEClassIntrospector classIntrospector) {
        this.classIntrospector = classIntrospector;
    }

    @Override
    public InstanceFactory createInstanceFactory(Class<?> clazz) throws NoSuchMethodException {
        final ManagedReferenceFactory factory = classIntrospector.createFactory(clazz);
        return new InstanceFactory() {
            @Override
            public InstanceHandle createInstance() throws InstantiationException {
                final ManagedReference reference = factory.getReference();
                return new InstanceHandle() {
                    @Override
                    public Object getInstance() {
                        return reference.getInstance();
                    }

                    @Override
                    public void release() {
                        reference.release();
                    }
                };
            }
        };
    }
}
