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

package org.jboss.as.connector.subsystems.datasources;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jboss.jca.common.CommonBundle;
import org.jboss.jca.common.api.metadata.common.CommonXaPool;
import org.jboss.jca.common.api.metadata.common.Recovery;
import org.jboss.jca.common.api.metadata.ds.DsSecurity;
import org.jboss.jca.common.api.metadata.ds.Statement;
import org.jboss.jca.common.api.metadata.ds.TimeOut;
import org.jboss.jca.common.api.metadata.ds.TransactionIsolation;
import org.jboss.jca.common.api.metadata.ds.Validation;
import org.jboss.jca.common.api.metadata.ds.v11.DsXaPool;
import org.jboss.jca.common.api.metadata.ds.v11.XaDataSource;
import org.jboss.jca.common.api.validator.ValidateException;
import org.jboss.jca.common.metadata.ds.DataSourceAbstractImpl;
import org.jboss.jca.common.metadata.ds.v11.XADataSourceImpl;
import org.jboss.logging.Messages;

/**
 * A modifiable DataSourceImpl to add connection properties
 *
 * @author <a href="stefano.maestri@jboss.com">Stefano Maestri</a>
 */
public class ModifiableXaDataSource extends DataSourceAbstractImpl implements XaDataSource {
    /**
     * The serialVersionUID
     */
    private static final long serialVersionUID = -1401087499308709724L;

    /**
     * The bundle
     */
    private static CommonBundle bundle = Messages.getBundle(CommonBundle.class);

    private final HashMap<String, String> xaDataSourceProperty;

    private String xaDataSourceClass;

    private final String driver;

    private final String newConnectionSql;

    private final DsXaPool xaPool;

    private final Recovery recovery;

    /**
     * Create a new XADataSourceImpl.
     *
     * @param transactionIsolation         transactionIsolation
     * @param timeOut                      timeOut
     * @param security                     security
     * @param statement                    statement
     * @param validation                   validation
     * @param urlDelimiter                 urlDelimiter
     * @param urlSelectorStrategyClassName urlSelectorStrategyClassName
     * @param useJavaContext               useJavaContext
     * @param poolName                     poolName
     * @param enabled                      enabled
     * @param jndiName                     jndiName
     * @param spy                          spy
     * @param useCcm                       useCcm
     * @param xaDataSourceProperty         xaDataSourceProperty
     * @param xaDataSourceClass            xaDataSourceClass
     * @param driver                       driver
     * @param newConnectionSql             newConnectionSql
     * @param xaPool                       xaPool
     * @param recovery                     recovery
     * @throws ValidateException ValidateException
     */
    public ModifiableXaDataSource(TransactionIsolation transactionIsolation, TimeOut timeOut, DsSecurity security,
                                  Statement statement, Validation validation, String urlDelimiter, String urlSelectorStrategyClassName,
                                  Boolean useJavaContext, String poolName, Boolean enabled, String jndiName, Boolean spy, Boolean useCcm,
                                  Map<String, String> xaDataSourceProperty, String xaDataSourceClass, String driver, String newConnectionSql,
                                  DsXaPool xaPool, Recovery recovery) throws ValidateException {
        super(transactionIsolation, timeOut, security, statement, validation, urlDelimiter,
                urlSelectorStrategyClassName, useJavaContext, poolName, enabled, jndiName, spy, useCcm);
        if (xaDataSourceProperty != null) {
            this.xaDataSourceProperty = new HashMap<String, String>(xaDataSourceProperty.size());
            this.xaDataSourceProperty.putAll(xaDataSourceProperty);
        } else {
            this.xaDataSourceProperty = new HashMap<String, String>(0);
        }
        this.xaDataSourceClass = xaDataSourceClass;
        this.driver = driver;
        this.newConnectionSql = newConnectionSql;
        this.xaPool = xaPool;
        this.recovery = recovery;
        this.validate();
    }

    /**
     * Get the xaDataSourceClass.
     *
     * @return the xaDataSourceClass.
     */
    @Override
    public final String getXaDataSourceClass() {
        return xaDataSourceClass;
    }

    /**
     * Get the driver.
     *
     * @return the driver.
     */
    @Override
    public final String getDriver() {
        return driver;
    }

    /**
     * Get the statement.
     *
     * @return the statement.
     */
    @Override
    public final Statement getStatement() {
        return statement;
    }

    /**
     * Get the urlDelimiter.
     *
     * @return the urlDelimiter.
     */
    @Override
    public final String getUrlDelimiter() {
        return urlDelimiter;
    }

    /**
     * Get the urlSelectorStrategyClassName.
     *
     * @return the urlSelectorStrategyClassName.
     */
    @Override
    public final String getUrlSelectorStrategyClassName() {
        return urlSelectorStrategyClassName;
    }

    /**
     * Get the newConnectionSql.
     *
     * @return the newConnectionSql.
     */
    @Override
    public final String getNewConnectionSql() {
        return newConnectionSql;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((driver == null) ? 0 : driver.hashCode());
        result = prime * result + ((newConnectionSql == null) ? 0 : newConnectionSql.hashCode());
        result = prime * result + ((xaDataSourceClass == null) ? 0 : xaDataSourceClass.hashCode());
        result = prime * result + ((xaDataSourceProperty == null) ? 0 : xaDataSourceProperty.hashCode());
        result = prime * result + ((xaPool == null) ? 0 : xaPool.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (!(obj instanceof ModifiableXaDataSource))
            return false;
        ModifiableXaDataSource other = (ModifiableXaDataSource) obj;
        if (driver == null) {
            if (other.driver != null)
                return false;
        } else if (!driver.equals(other.driver))
            return false;
        if (newConnectionSql == null) {
            if (other.newConnectionSql != null)
                return false;
        } else if (!newConnectionSql.equals(other.newConnectionSql))
            return false;
        if (xaDataSourceClass == null) {
            if (other.xaDataSourceClass != null)
                return false;
        } else if (!xaDataSourceClass.equals(other.xaDataSourceClass))
            return false;
        if (xaDataSourceProperty == null) {
            if (other.xaDataSourceProperty != null)
                return false;
        } else if (!xaDataSourceProperty.equals(other.xaDataSourceProperty))
            return false;
        if (xaPool == null) {
            if (other.xaPool != null)
                return false;
        } else if (!xaPool.equals(other.xaPool))
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("<xa-datasource");

        if (jndiName != null)
            sb.append(" ").append(XaDataSource.Attribute.JNDI_NAME).append("=\"").append(jndiName).append("\"");

        if (poolName != null)
            sb.append(" ").append(XaDataSource.Attribute.POOL_NAME).append("=\"").append(poolName).append("\"");

        if (enabled != null)
            sb.append(" ").append(XaDataSource.Attribute.ENABLED).append("=\"").append(enabled).append("\"");

        if (useJavaContext != null) {
            sb.append(" ").append(XaDataSource.Attribute.USE_JAVA_CONTEXT);
            sb.append("=\"").append(useJavaContext).append("\"");
        }

        if (spy)
            sb.append(" ").append(XaDataSource.Attribute.SPY).append("=\"").append(spy).append("\"");

        if (useCcm)
            sb.append(" ").append(XaDataSource.Attribute.USE_CCM).append("=\"").append(useCcm).append("\"");

        sb.append(">");

        if (xaDataSourceProperty != null && xaDataSourceProperty.size() > 0) {
            Iterator<Map.Entry<String, String>> it = xaDataSourceProperty.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> entry = it.next();
                sb.append("<").append(XaDataSource.Tag.XA_DATASOURCE_PROPERTY);
                sb.append(" name=\"").append(entry.getKey()).append("\">");
                sb.append(entry.getValue());
                sb.append("</").append(XaDataSource.Tag.XA_DATASOURCE_PROPERTY).append(">");
            }
        }

        if (xaDataSourceClass != null) {
            sb.append("<").append(XaDataSource.Tag.XA_DATASOURCE_CLASS).append(">");
            sb.append(xaDataSourceClass);
            sb.append("</").append(XaDataSource.Tag.XA_DATASOURCE_CLASS).append(">");
        }

        if (driver != null) {
            sb.append("<").append(XaDataSource.Tag.DRIVER).append(">");
            sb.append(driver);
            sb.append("</").append(XaDataSource.Tag.DRIVER).append(">");
        }

        if (urlDelimiter != null) {
            sb.append("<").append(XaDataSource.Tag.URL_DELIMITER).append(">");
            sb.append(urlDelimiter);
            sb.append("</").append(XaDataSource.Tag.URL_DELIMITER).append(">");
        }

        if (urlSelectorStrategyClassName != null) {
            sb.append("<").append(XaDataSource.Tag.URL_SELECTOR_STRATEGY_CLASS_NAME).append(">");
            sb.append(urlSelectorStrategyClassName);
            sb.append("</").append(XaDataSource.Tag.URL_SELECTOR_STRATEGY_CLASS_NAME).append(">");
        }

        if (newConnectionSql != null) {
            sb.append("<").append(XaDataSource.Tag.NEW_CONNECTION_SQL).append(">");
            sb.append(newConnectionSql);
            sb.append("</").append(XaDataSource.Tag.NEW_CONNECTION_SQL).append(">");
        }

        if (transactionIsolation != null) {
            sb.append("<").append(XaDataSource.Tag.TRANSACTION_ISOLATION).append(">");
            sb.append(transactionIsolation);
            sb.append("</").append(XaDataSource.Tag.TRANSACTION_ISOLATION).append(">");
        }

        if (xaPool != null)
            sb.append(xaPool);

        if (security != null)
            sb.append(security);

        if (validation != null)
            sb.append(validation);

        if (timeOut != null)
            sb.append(timeOut);

        if (statement != null)
            sb.append(statement);

        if (recovery != null)
            sb.append(recovery);

        sb.append("</xa-datasource>");

        return sb.toString();
    }

    /**
     * Get the xaDataSourceProperty.
     *
     * @return the xaDataSourceProperty.
     */
    @Override
    public final Map<String, String> getXaDataSourceProperty() {
        return Collections.unmodifiableMap(xaDataSourceProperty);
    }

    public final void addXaDataSourceProperty(String name, String value) {
        xaDataSourceProperty.put(name, value);
    }

    /**
     * Get the xaPool.
     *
     * @return the xaPool.
     */
    @Override
    public final DsXaPool getXaPool() {
        return xaPool;
    }

    @Override
    public void validate() throws ValidateException {
        if ((this.xaDataSourceClass == null || this.xaDataSourceClass.trim().length() == 0) &&
                (this.driver == null || this.driver.trim().length() == 0))
            throw new ValidateException(bundle.requiredElementMissing(XaDataSource.Tag.XA_DATASOURCE_CLASS.getLocalName(),
                    this.getClass().getCanonicalName()));
    }

    @Override
    public Recovery getRecovery() {
        return recovery;
    }

    /**
     * Set the xaDataSourceClass.
     *
     * @param xaDataSourceClass The xaDataSourceClass to set.
     */
    public final void forceXaDataSourceClass(String xaDataSourceClass) {
        this.xaDataSourceClass = xaDataSourceClass;
    }

    public final XaDataSource getUnModifiableInstance() throws ValidateException {

        return new XADataSourceImpl(transactionIsolation, timeOut, security,
                statement, validation, urlDelimiter, urlSelectorStrategyClassName,
                useJavaContext, poolName, enabled, jndiName, spy, useCcm,
                xaDataSourceProperty, xaDataSourceClass, driver, newConnectionSql,
                xaPool, recovery);

    }
}

