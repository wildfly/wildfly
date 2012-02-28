package org.jboss.as.arquillian.container;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.container.spi.context.ContainerContext;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.spi.TestEnricher;
import org.jboss.as.arquillian.api.ContainerResource;

/**
 * Test enricher that allows for injection of remote JNDI context into @RunAsClient test cases.
 *
 * @author Stuart Douglas
 */
public class ContainerResourceTestEnricher implements TestEnricher {

    @Inject
    private Instance<ContainerRegistry> containerRegistry;

    @Inject
    private Instance<ContainerContext> containerContext;

    @Inject
    private Instance<ManagementClient> managementClient;

    /* (non-Javadoc)
    * @see org.jboss.arquillian.spi.TestEnricher#enrich(java.lang.Object)
    */
    public void enrich(Object testCase) {
        for (Field field : SecurityActions.getFieldsWithAnnotation(testCase.getClass(), ContainerResource.class)) {
            Object value = null;
            try {
                Annotation[] qualifiers = filterAnnotations(field.getAnnotations());
                // null value will throw exception in lookup
                value = lookup(field.getType(), field.getAnnotation(ContainerResource.class), qualifiers);
            } catch (Exception e) {
                throw new RuntimeException("Could not lookup value for field " + field, e);
            }
            try {
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                field.set(testCase, value);
            } catch (Exception e) {
                throw new RuntimeException("Could not set value on field " + field + " using " + value);
            }
        }
    }

    /* (non-Javadoc)
    * @see org.jboss.arquillian.spi.TestEnricher#resolve(java.lang.reflect.Method)
    */
    public Object[] resolve(Method method) {
        Object[] values = new Object[method.getParameterTypes().length];
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            ContainerResource resource = getResourceAnnotation(method.getParameterAnnotations()[i]);
            if (resource != null) {
                Annotation[] qualifiers = filterAnnotations(method.getParameterAnnotations()[i]);
                values[i] = lookup(method.getParameterTypes()[i], resource, qualifiers);
            }
        }
        return values;
    }

    /**
     * @param type
     * @param resource
     * @return
     * @throws IllegalArgumentException If no ResourceProvider found for Type
     * @throws RuntimeException         If ResourceProvider return null
     */
    private Object lookup(Class<?> type, ContainerResource resource, Annotation... qualifiers) {
        final Container container;
        final List<Container> containers = containerRegistry.get().getContainers();
        if (resource.value().isEmpty()) {
            if (containers.size() > 1) {
                throw new RuntimeException("@ContainerResource did not specify a server and more than one server exists in the deployment");
            }
            container = containers.get(0);
        } else {
            container = containerRegistry.get().getContainer(resource.value());
            if (container == null) {
                throw new RuntimeException("@ContainerResource specified non existent server " + resource.value());
            }
        }
        try {
            containerContext.get().activate(container.getName());
            if (Context.class.isAssignableFrom(type)) {
                return lookupContext(type, resource, qualifiers);
            } else if (ManagementClient.class.isAssignableFrom(type)) {
                return managementClient.get();
            } else {
                throw new RuntimeException("@ContainerResource an unknown type " + resource.value());

            }
        } finally {
            containerContext.get().deactivate();
        }
    }

    /**
     * @param type
     * @param resource
     * @return
     * @throws IllegalArgumentException If no ResourceProvider found for Type
     * @throws RuntimeException         If ResourceProvider return null
     */
    private Object lookupContext(Class<?> type, ContainerResource resource, Annotation... qualifiers) {
        try {
            final Properties env = new Properties();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
            env.put(Context.PROVIDER_URL, managementClient.get().getRemoteEjbURL().toString());
            env.put("jboss.naming.client.connect.options.org.xnio.Options.SASL_POLICY_NOPLAINTEXT", "false");
            env.put("jboss.naming.client.security.callback.handler.class", Authentication.CallbackHandler.class.getName());
            return new InitialContext(env);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    private ContainerResource getResourceAnnotation(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType() == ContainerResource.class) {
                return (ContainerResource) annotation;
            }
        }
        return null;
    }

    /**
     * @param annotations
     * @return
     */
    private Annotation[] filterAnnotations(Annotation[] annotations) {
        if (annotations == null) {
            return new Annotation[0];
        }
        List<Annotation> filtered = new ArrayList<Annotation>();
        for (Annotation annotation : annotations) {
            if (annotation.annotationType() != ContainerResource.class) {
                filtered.add(annotation);
            }
        }
        return filtered.toArray(new Annotation[0]);
    }

    private static interface ContainerResourceProvider {
        Object lookup(Class<?> type, ContainerResource resource, Annotation... qualifiers);
    }
}
