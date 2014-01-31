package org.wildfly.extension.undertow.deployment;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import org.jboss.common.beans.property.BeanUtils;
import org.wildfly.extension.undertow.UndertowMessages;
import org.wildfly.security.manager.WildFlySecurityManager;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Properties;

/**
 * Handler wrapper that create a new instance of the specified {@link HandlerWrapper} or
 * {@link HttpHandler} class, and configures it via the specified properties.
 *
 * @author Stuart Douglas
 */
public class ConfiguredHandlerWrapper implements HandlerWrapper {

    private final Class<?> handlerClass;
    private final Map<String, String> properties;

    public ConfiguredHandlerWrapper(Class<?> handlerClass, Map<String, String> properties) {
        this.handlerClass = handlerClass;
        this.properties = properties;
    }

    @Override
    public HttpHandler wrap(HttpHandler handler) {
        try {
            Object instance;
            if (HttpHandler.class.isAssignableFrom(handlerClass)) {
                Constructor<?> ctor = handlerClass.getConstructor(HttpHandler.class);
                instance = ctor.newInstance(handler);
            } else if (HandlerWrapper.class.isAssignableFrom(handlerClass)) {
                instance = handlerClass.newInstance();
            } else {
                throw UndertowMessages.MESSAGES.handlerWasNotAHandlerOrWrapper(handlerClass);
            }
            Properties p = new Properties();
            p.putAll(properties);

            ClassLoader oldCl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
            try {
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(BeanUtils.class);
                BeanUtils.mapJavaBeanProperties(instance, p);
            } finally {
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(oldCl);
            }
            if (HttpHandler.class.isAssignableFrom(handlerClass)) {
                return (HttpHandler) instance;
            } else {
                return ((HandlerWrapper) instance).wrap(handler);
            }
        } catch (Exception e) {
            throw UndertowMessages.MESSAGES.failedToConfigureHandler(handlerClass, e);
        }
    }
}
