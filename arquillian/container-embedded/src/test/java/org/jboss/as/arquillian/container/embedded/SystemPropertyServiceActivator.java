package org.jboss.as.arquillian.container.embedded;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * @author Stuart Douglas
 */
public class SystemPropertyServiceActivator implements ServiceActivator {

    public static final String TEST_PROPERTY = "test-property";
    public static final String VALUE = "set";

    @Override
    public void activate(ServiceActivatorContext serviceActivatorContext) throws ServiceRegistryException {
        serviceActivatorContext.getServiceTarget().addService(ServiceName.of("test-service"), new Service<Object>() {
            @Override
            public void start(StartContext context) throws StartException {
                System.setProperty(TEST_PROPERTY, VALUE);
            }

            @Override
            public void stop(StopContext context) {
                System.clearProperty(TEST_PROPERTY);
            }

            @Override
            public Object getValue() throws IllegalStateException, IllegalArgumentException {
                return this;
            }
        }).install();
    }
}
