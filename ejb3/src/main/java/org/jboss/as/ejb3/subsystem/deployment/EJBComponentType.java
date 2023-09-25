/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem.deployment;

import java.util.HashMap;
import java.util.Map;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.messagedriven.MessageDrivenComponent;
import org.jboss.as.ejb3.component.messagedriven.MessageDrivenComponentDescription;
import org.jboss.as.ejb3.component.pool.PooledComponent;
import org.jboss.as.ejb3.component.singleton.SingletonComponent;
import org.jboss.as.ejb3.component.singleton.SingletonComponentDescription;
import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.ejb3.component.stateful.StatefulSessionComponent;
import org.jboss.as.ejb3.component.stateless.StatelessComponentDescription;
import org.jboss.as.ejb3.component.stateless.StatelessSessionComponent;
import org.jboss.as.ejb3.pool.Pool;

/**
 * Enumeration of types of manageable Jakarta Enterprise Beans components.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public enum EJBComponentType {

    MESSAGE_DRIVEN("message-driven-bean", MessageDrivenComponent.class, MessageDrivenComponentDescription.class),
    SINGLETON("singleton-bean", SingletonComponent.class, SingletonComponentDescription.class),
    STATELESS("stateless-session-bean", StatelessSessionComponent.class, StatelessComponentDescription.class),
    STATEFUL("stateful-session-bean", StatefulSessionComponent.class, StatefulComponentDescription.class);

    private static final Map<Class<?>, EJBComponentType> typeByDescriptionClass;

    static {
        typeByDescriptionClass = new HashMap<Class<?>, EJBComponentType>();
        for (EJBComponentType type : values()) {
            typeByDescriptionClass.put(type.componentDescriptionClass, type);
        }
    }

    private final String resourceType;
    private final Class<? extends EJBComponent> componentClass;
    private final Class<? extends EJBComponentDescription> componentDescriptionClass;

    private EJBComponentType(final String resourceType, final Class<? extends EJBComponent> componentClass,
                             final Class<? extends EJBComponentDescription> componentDescriptionClass) {
        this.resourceType = resourceType;
        this.componentClass = componentClass;
        this.componentDescriptionClass = componentDescriptionClass;
    }

    public String getResourceType() {
         return resourceType;
    }

    public Class<? extends EJBComponent> getComponentClass() {
        return componentClass;
    }

    public Class<? extends EJBComponentDescription> getComponentDescriptionClass() {
        return componentDescriptionClass;
    }

    public boolean hasPool() {
        switch (this) {
            case STATEFUL:
            case SINGLETON:
                return false;
            default:
                return true;
        }
    }

    public boolean hasTimer() {
        switch (this) {
            case STATELESS:
            case SINGLETON:
            case MESSAGE_DRIVEN:
                return true;
            default:
                return false;
        }

    }

    public Pool<?> getPool(EJBComponent component) {
        return pooledComponent(component).getPool();
    }

    public AbstractEJBComponentRuntimeHandler<?> getRuntimeHandler() {
        switch (this) {
            case MESSAGE_DRIVEN:
                return MessageDrivenBeanRuntimeHandler.INSTANCE;
            case SINGLETON:
                return SingletonBeanRuntimeHandler.INSTANCE;
            case STATELESS:
                return StatelessSessionBeanRuntimeHandler.INSTANCE;
            case STATEFUL:
                return StatefulSessionBeanRuntimeHandler.INSTANCE;
            default:
                // Bug
                throw EjbLogger.ROOT_LOGGER.unknownComponentType(this);
        }
    }

    public static EJBComponentType getComponentType(ComponentConfiguration componentConfiguration) {
        final ComponentDescription description = componentConfiguration.getComponentDescription();
        EJBComponentType type = typeByDescriptionClass.get(description.getClass());
        if (type != null) {
            return type;
        }
        // Check for subclass
        for(Map.Entry<Class<?>, EJBComponentType> entry : typeByDescriptionClass.entrySet()) {
            if(entry.getKey().isAssignableFrom(description.getClass())) {
                return entry.getValue();
            }
        }
        throw EjbLogger.ROOT_LOGGER.unknownComponentDescriptionType(description.getClass());
    }

    protected PooledComponent<?> pooledComponent(final EJBComponent component) {
        switch (this) {
            case MESSAGE_DRIVEN:
                return MessageDrivenComponent.class.cast(component);
            case STATELESS:
                return StatelessSessionComponent.class.cast(component);
            case SINGLETON:
            case STATEFUL:
                throw EjbLogger.ROOT_LOGGER.invalidComponentType(this.getComponentClass().getSimpleName());
            default:
                // Bug
                throw EjbLogger.ROOT_LOGGER.unknownComponentType(this);
        }
    }
}
