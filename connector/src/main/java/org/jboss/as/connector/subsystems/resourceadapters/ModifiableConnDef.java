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

import org.jboss.jca.common.api.metadata.common.Pool;
import org.jboss.jca.common.api.metadata.common.Recovery;
import org.jboss.jca.common.api.metadata.common.Security;
import org.jboss.jca.common.api.metadata.common.TimeOut;
import org.jboss.jca.common.api.metadata.common.Validation;
import org.jboss.jca.common.api.metadata.common.XaPool;
import org.jboss.jca.common.api.metadata.resourceadapter.ConnectionDefinition;
import org.jboss.jca.common.api.validator.ValidateException;


public class ModifiableConnDef implements ConnectionDefinition {
    /**
     * The serialVersionUID
     */
    private static final long serialVersionUID = -7109775624169563102L;

    private final ConcurrentHashMap<String, String> configProperties;

    private final String className;

    private final String jndiName;

    private final String poolName;

    private final Boolean enabled;

    private final Boolean useJavaContext;

    private final Boolean useCcm;

    private final Pool pool;

    private final TimeOut timeOut;

    private final Validation validation;

    private final Security security;

    private final Recovery recovery;

    private final Boolean sharable;

    private final Boolean enlistment;

    private final Boolean connectable;

    private final Boolean tracking;

    private final Boolean enlistmentTrace;

    private final String mcp;


    /**
     * Create a new ConnectionDefinition.
     *
     * @param configProperties configProperties
     * @param className        className
     * @param jndiName         jndiName
     * @param poolName         poolName
     * @param enabled          enabled
     * @param useJavaContext   useJavaContext
     * @param useCcm           useCcm
     * @param pool             pool
     * @param timeOut          timeOut
     * @param validation       validation
     * @param security         security
     * @param recovery         recovery
     */
    public ModifiableConnDef(Map<String, String> configProperties, String className, String jndiName,
                             String poolName, Boolean enabled, Boolean useJavaContext, Boolean useCcm, Pool pool, TimeOut timeOut,
                             Validation validation, Security security, Recovery recovery, Boolean sharable, Boolean enlistment,
                             final Boolean connectable, final Boolean tracking, final String mcp, Boolean enlistmentTrace) throws ValidateException {
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
        this.useCcm = useCcm;
        this.pool = pool;
        this.timeOut = timeOut;
        this.validation = validation;
        this.security = security;
        this.recovery = recovery;
        this.sharable = sharable;
        this.enlistment = enlistment;
        this.connectable = connectable;
        this.tracking = tracking;
        this.mcp = mcp;
        this.enlistmentTrace = enlistmentTrace;

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
     * Get the poolName.
     *
     * @return the poolName.
     */
    @Override
    public final String getPoolName() {
        return poolName;
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

    /**
     * Get the useCcm.
     *
     * @return the useCcm.
     */
    @Override
    public final Boolean isUseCcm() {
        return useCcm;
    }

    /**
     * Get the pool.
     *
     * @return the pool.
     */
    @Override
    public final Pool getPool() {
        return pool;
    }

    /**
     * Get the timeOut.
     *
     * @return the timeOut.
     */
    @Override
    public final TimeOut getTimeOut() {
        return timeOut;
    }

    /**
     * Get the validation.
     *
     * @return the validation.
     */
    @Override
    public final Validation getValidation() {
        return validation;
    }

    /**
     * Get the security.
     *
     * @return the security.
     */
    @Override
    public final Security getSecurity() {
        return security;
    }

    @Override
    public final Boolean isXa() {
        return (pool instanceof XaPool);
    }

    /**
     * Get the recovery.
     *
     * @return the recovery.
     */
    @Override
    public final Recovery getRecovery() {
        return recovery;
    }

    @Override
    public Boolean isSharable() {
        return sharable;
    }

    @Override
    public Boolean isEnlistment() {
        return enlistment;
    }

    @Override
    public Boolean isConnectable() {
        return connectable;
    }

    @Override
    public Boolean isTracking() {
        return tracking;
    }

    @Override
    public String getMcp() {
        return mcp;
    }

    @Override
    public Boolean isEnlistmentTrace() {
        return enlistmentTrace;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((className == null) ? 0 : className.hashCode());
        result = prime * result + ((configProperties == null) ? 0 : configProperties.hashCode());
        result = prime * result + ((enabled == null) ? 0 : enabled.hashCode());
        result = prime * result + ((jndiName == null) ? 0 : jndiName.hashCode());
        result = prime * result + ((pool == null) ? 0 : pool.hashCode());
        result = prime * result + ((poolName == null) ? 0 : poolName.hashCode());
        result = prime * result + ((recovery == null) ? 0 : recovery.hashCode());
        result = prime * result + ((security == null) ? 0 : security.hashCode());
        result = prime * result + ((timeOut == null) ? 0 : timeOut.hashCode());
        result = prime * result + ((useJavaContext == null) ? 0 : useJavaContext.hashCode());
        result = prime * result + ((useCcm == null) ? 0 : useCcm.hashCode());
        result = prime * result + ((validation == null) ? 0 : validation.hashCode());
        result = prime * result + ((isXa() == null) ? 0 : isXa().hashCode());

        result = prime * result + ((sharable == null) ? 0 : sharable.hashCode());
        result = prime * result + ((enlistment == null) ? 0 : enlistment.hashCode());

        result = prime * result + ((connectable == null) ? 0 : connectable.hashCode());
        result = prime * result + ((tracking == null) ? 0 : tracking.hashCode());

        result = prime * result + ((mcp == null) ? 0 : mcp.hashCode());
        result = prime * result + ((enlistmentTrace == null) ? 0 : enlistmentTrace.hashCode());

        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof ModifiableConnDef))
            return false;

        ModifiableConnDef other = (ModifiableConnDef) obj;
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
        if (pool == null) {
            if (other.pool != null)
                return false;
        } else if (!pool.equals(other.pool))
            return false;
        if (poolName == null) {
            if (other.poolName != null)
                return false;
        } else if (!poolName.equals(other.poolName))
            return false;
        if (recovery == null) {
            if (other.recovery != null)
                return false;
        } else if (!recovery.equals(other.recovery))
            return false;
        if (security == null) {
            if (other.security != null)
                return false;
        } else if (!security.equals(other.security))
            return false;
        if (timeOut == null) {
            if (other.timeOut != null)
                return false;
        } else if (!timeOut.equals(other.timeOut))
            return false;
        if (useJavaContext == null) {
            if (other.useJavaContext != null)
                return false;
        } else if (!useJavaContext.equals(other.useJavaContext))
            return false;
        if (useCcm == null) {
            if (other.useCcm != null)
                return false;
        } else if (!useCcm.equals(other.useCcm))
            return false;
        if (validation == null) {
            if (other.validation != null)
                return false;
        } else if (!validation.equals(other.validation))
            return false;
        if (isXa() == null) {
            if (other.isXa() != null)
                return false;
        } else if (!isXa().equals(other.isXa()))
            return false;

        if (sharable == null) {
            if (other.sharable != null)
                return false;
        } else if (!sharable.equals(other.sharable))
            return false;
        if (enlistment == null) {
            if (other.enlistment != null)
                return false;
        } else if (!enlistment.equals(other.enlistment))
            return false;

        if (connectable == null) {
            if (other.connectable != null)
                return false;
        } else if (!connectable.equals(other.connectable))
            return false;
        if (tracking == null) {
            if (other.tracking != null)
                return false;
        } else if (!tracking.equals(other.tracking))
            return false;
        if (mcp == null) {
            if (other.mcp != null)
                return false;
        } else if (!mcp.equals(other.mcp))
            return false;
        if (enlistmentTrace == null) {
            if (other.enlistmentTrace != null)
                return false;
        } else if (!enlistmentTrace.equals(other.enlistmentTrace))
            return false;

        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(1024);

        sb.append("<connection-definition");

        if (className != null)
            sb.append(" ").append(ConnectionDefinition.Attribute.CLASS_NAME).append("=\"").append(className).append("\"");

        if (jndiName != null)
            sb.append(" ").append(ConnectionDefinition.Attribute.JNDI_NAME).append("=\"").append(jndiName).append("\"");

        if (enabled != null)
            sb.append(" ").append(ConnectionDefinition.Attribute.ENABLED).append("=\"").append(enabled).append("\"");

        if (useJavaContext != null) {
            sb.append(" ").append(ConnectionDefinition.Attribute.USE_JAVA_CONTEXT);
            sb.append("=\"").append(useJavaContext).append("\"");
        }

        if (poolName != null)
            sb.append(" ").append(ConnectionDefinition.Attribute.POOL_NAME).append("=\"").append(poolName).append("\"");

        if (useCcm != null)
            sb.append(" ").append(ConnectionDefinition.Attribute.USE_CCM).append("=\"").append(useCcm).append("\"");

        if (sharable != null)
            sb.append(" ").append(ConnectionDefinition.Attribute.SHARABLE).append("=\"").append(sharable).append("\"");

        if (enlistment != null)
            sb.append(" ").append(ConnectionDefinition.Attribute.ENLISTMENT).append("=\"").append(enlistment).append("\"");

        if (connectable != null)
            sb.append(" ").append(ConnectionDefinition.Attribute.CONNECTABLE).append("=\"").
                    append(connectable).append("\"");

        if (tracking != null)
            sb.append(" ").append(ConnectionDefinition.Attribute.TRACKING).append("=\"").append(tracking).append("\"");

        if (mcp != null)
            sb.append(" ").append(ConnectionDefinition.Attribute.MCP).append("=\"").append(mcp).append("\"");

        if (enlistmentTrace != null)
            sb.append(" ").append(ConnectionDefinition.Attribute.ENLISTMENT_TRACE).append("=\"")
                    .append(enlistmentTrace).append("\"");

        sb.append(">");

        if (configProperties != null && configProperties.size() > 0) {
            Iterator<Map.Entry<String, String>> it = configProperties.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> entry = it.next();

                sb.append("<").append(ConnectionDefinition.Tag.CONFIG_PROPERTY);
                sb.append(" name=\"").append(entry.getKey()).append("\">");
                sb.append(entry.getValue());
                sb.append("</").append(ConnectionDefinition.Tag.CONFIG_PROPERTY).append(">");
            }
        }

        if (pool != null)
            sb.append(pool);

        if (security != null)
            sb.append(security);

        if (timeOut != null)
            sb.append(timeOut);

        if (validation != null)
            sb.append(validation);

        if (recovery != null)
            sb.append(recovery);

        sb.append("</connection-definition>");

        return sb.toString();
    }
}
