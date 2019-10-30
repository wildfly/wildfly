package org.wildfly.extension.messaging.activemq;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.wildfly.extension.messaging.activemq.jms.WildFlyBindingRegistry;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AS7BindingRegistryTestCase {

    private ServiceContainer container;

    @Before
    public void setUp() {
        container = ServiceContainer.Factory.create("test");
    }

    @After
    public void tearDown() {
        if (container != null) {
            container.shutdown();
            try {
                container.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            finally {
                container = null;
            }
        }
    }

    /*
     * https://issues.jboss.org/browse/AS7-4269
     */
    @Test
    public void bindUnbindBind() throws Exception {
        WildFlyBindingRegistry registry = new WildFlyBindingRegistry(container);

        Object obj = new Object();
        String name = UUID.randomUUID().toString();
        assertNull(getBinderServiceFor(name));

        assertTrue(registry.bind(name, obj));
        assertNotNull(getBinderServiceFor(name));

        registry.unbind(name);
        assertNull(getBinderServiceFor(name));

        assertTrue(registry.bind(name, obj));
        assertNotNull(getBinderServiceFor(name));
    }

    private ServiceController<?> getBinderServiceFor(String name) {
        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(name);
        return container.getService(bindInfo.getBinderServiceName());
    }
}
