/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.connector.subsystems.resourceadapters;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.jca.common.api.metadata.resourceadapter.AdminObject;
import org.jboss.jca.common.api.validator.ValidateException;


public class ModifiableAdminObject implements AdminObject {
    /**
     * The serialVersionUID
     */
    private static final long serialVersionUID = 8137442556861441967L;

    private final ConcurrentHashMap<String, String> configProperties;

    private final String className;

    private final String jndiName;

    private final String poolName;

    private final Boolean enabled;

    private final Boolean useJavaContext;


    /**
     * Create a new AdminObjectImpl.
     *
     * @param configProperties configProperties
     * @param className        className
     * @param jndiName         jndiName
     * @param poolName         poolName
     * @param enabled          enabled
     * @param useJavaContext   useJavaContext
     */
    public ModifiableAdminObject(Map<String, String> configProperties, String className, String jndiName,
                                 String poolName, Boolean enabled, Boolean useJavaContext) throws ValidateException {
        super();
        if (configProperties != null) {
            this.configProperties = new ConcurrentHashMap<String, String>(configProperties.size());
            this.configProperties.putAll(configProperties);
        } else {
            this.configProperties = new ConcurrentHashMap<String, String>(0);
        }
        this.className = className;
        this.jndiName = jndiName;
        this.poolName = poolName;
        this.enabled = enabled;
        this.useJavaContext = useJavaContext;
    }

    /**
     * Get the configProperties.
     *
     * @return the configProperties.
     */
    @Override
    public final Map<String, String> getConfigProperties() {
        return Collections.unmodifiableMap(configProperties);
    }

    public String addConfigProperty(String key, String value) {
        return configProperties.put(key, value);
    }

    /**
     * Get the className.
     *
     * @return the className.
     */
    @Override
    public final String getClassName() {
        return className;
    }

    /**
     * Get the jndiName.
     *
     * @return the jndiName.
     */
    @Override
    public final String getJndiName() {
        return jndiName;
    }

    /**
     * Get the enabled.
     *
     * @return the enabled.
     */
    @Override
    public final Boolean isEnabled() {
        return enabled;
    }

    /**
     * Get the useJavaContext.
     *
     * @return the useJavaContext.
     */
    @Override
    public final Boolean isUseJavaContext() {
        return useJavaContext;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((className == null) ? 0 : className.hashCode());
        result = prime * result + ((configProperties == null) ? 0 : configProperties.hashCode());
        result = prime * result + ((enabled == null) ? 0 : enabled.hashCode());
        result = prime * result + ((jndiName == null) ? 0 : jndiName.hashCode());
        result = prime * result + ((poolName == null) ? 0 : poolName.hashCode());
        result = prime * result + ((useJavaContext == null) ? 0 : useJavaContext.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof ModifiableAdminObject))
            return false;
        ModifiableAdminObject other = (ModifiableAdminObject) obj;
        if (className == null) {
            if (other.className != null)
                return false;
        } else if (!className.equals(other.className))
            return false;
        if (configProperties == null) {
            if (other.configProperties != null)
                return false;
        } else if (!configProperties.equals(other.configProperties))
            return false;
        if (enabled == null) {
            if (other.enabled != null)
                return false;
        } else if (!enabled.equals(other.enabled))
            return false;
        if (jndiName == null) {
            if (other.jndiName != null)
                return false;
        } else if (!jndiName.equals(other.jndiName))
            return false;
        if (poolName == null) {
            if (other.poolName != null)
                return false;
        } else if (!poolName.equals(other.poolName))
            return false;
        if (useJavaContext == null) {
            if (other.useJavaContext != null)
                return false;
        } else if (!useJavaContext.equals(other.useJavaContext))
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(1024);

        sb.append("<admin-object");

        if (className != null)
            sb.append(" ").append(AdminObject.Attribute.CLASS_NAME).append("=\"").append(className).append("\"");

        if (jndiName != null)
            sb.append(" ").append(AdminObject.Attribute.JNDI_NAME).append("=\"").append(jndiName).append("\"");

        if (enabled != null)
            sb.append(" ").append(AdminObject.Attribute.ENABLED).append("=\"").append(enabled).append("\"");

        if (useJavaContext != null) {
            sb.append(" ").append(AdminObject.Attribute.USE_JAVA_CONTEXT);
            sb.append("=\"").append(useJavaContext).append("\"");
        }

        if (poolName != null)
            sb.append(" ").append(AdminObject.Attribute.POOL_NAME).append("=\"").append(poolName).append("\"");

        sb.append(">");

        if (configProperties != null && configProperties.size() > 0) {
            Iterator<Map.Entry<String, String>> it = configProperties.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> entry = it.next();

                sb.append("<").append(AdminObject.Tag.CONFIG_PROPERTY);
                sb.append(" name=\"").append(entry.getKey()).append("\">");
                sb.append(entry.getValue());
                sb.append("</").append(AdminObject.Tag.CONFIG_PROPERTY).append(">");
            }
        }

        sb.append("</admin-object>");

        return sb.toString();
    }

    /**
     * Get the poolName.
     *
     * @return the poolName.
     */
    @Override
    public final String getPoolName() {
        return poolName;
    }

}
