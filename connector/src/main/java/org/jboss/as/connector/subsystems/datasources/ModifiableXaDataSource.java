/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.datasources;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.jboss.jca.common.CommonBundle;
import org.jboss.jca.common.api.metadata.common.Recovery;
import org.jboss.jca.common.api.metadata.ds.DsSecurity;
import org.jboss.jca.common.api.metadata.ds.DsXaPool;
import org.jboss.jca.common.api.metadata.ds.Statement;
import org.jboss.jca.common.api.metadata.ds.TimeOut;
import org.jboss.jca.common.api.metadata.ds.TransactionIsolation;
import org.jboss.jca.common.api.metadata.ds.Validation;
import org.jboss.jca.common.api.metadata.ds.XaDataSource;
import org.jboss.jca.common.api.validator.ValidateException;
import org.jboss.jca.common.metadata.ds.XADataSourceImpl;
import org.jboss.logging.Messages;

/**
 * A modifiable DataSourceImpl to add connection properties
 *
 * @author <a href="stefano.maestri@jboss.com">Stefano Maestri</a>
 */
public class ModifiableXaDataSource extends XADataSourceImpl implements XaDataSource {
    /**
     * The serialVersionUID
     */
    private static final long serialVersionUID = -1401087499308709724L;

    /**
     * The bundle
     */
    private static CommonBundle bundle = Messages.getBundle(MethodHandles.lookup(), CommonBundle.class);


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
     * @param mcp mcp
     * @param enlistmentTrace enlistmentTrace
     * @param xaDataSourceProperty         xaDataSourceProperty
     * @param xaDataSourceClass            xaDataSourceClass
     * @param driver                       driver
     * @param newConnectionSql             newConnectionSql
     * @param xaPool                       xaPool
     * @param recovery                     recovery
     * @throws ValidateException ValidateException
     */
    public ModifiableXaDataSource(TransactionIsolation transactionIsolation, TimeOut timeOut, DsSecurity security,
                                  Statement statement, Validation validation, String urlDelimiter, String urlProperty, String urlSelectorStrategyClassName,
                                  Boolean useJavaContext, String poolName, Boolean enabled, String jndiName, Boolean spy, Boolean useCcm,
                                  final Boolean connectable, final Boolean tracking, String mcp, Boolean enlistmentTrace,
                                  Map<String, String> xaDataSourceProperty, String xaDataSourceClass, String driver, String newConnectionSql,
                                  DsXaPool xaPool, Recovery recovery) throws ValidateException {
        super(transactionIsolation, timeOut, security, statement, validation, urlDelimiter,
                urlProperty, urlSelectorStrategyClassName, useJavaContext, poolName, enabled, jndiName, spy, useCcm,
                connectable, tracking, mcp, enlistmentTrace,
                xaDataSourceProperty, xaDataSourceClass, driver, newConnectionSql,
                xaPool, recovery);
    }


    public final void addXaDataSourceProperty(String name, String value) {
        xaDataSourceProperty.put(name, value);
    }

    @Override
    public void validate() throws ValidateException {
        if ((this.xaDataSourceClass == null || this.xaDataSourceClass.trim().length() == 0) &&
                (this.driver == null || this.driver.trim().length() == 0))
            throw new ValidateException(bundle.requiredElementMissing(Tag.XA_DATASOURCE_CLASS.getLocalName(),
                    this.getClass().getCanonicalName()));
    }


    public final XaDataSource getUnModifiableInstance() throws ValidateException {

        return new XADataSourceImpl(transactionIsolation, timeOut, security,
                statement, validation, urlDelimiter, urlProperty, urlSelectorStrategyClassName,
                useJavaContext, poolName, enabled, jndiName, spy, useCcm, connectable, tracking, mcp, enlistmentTrace,
                xaDataSourceProperty, xaDataSourceClass, driver, newConnectionSql,
                getXaPool(), recovery);

    }
}

