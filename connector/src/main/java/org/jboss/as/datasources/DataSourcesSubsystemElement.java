/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.datasources;

import java.util.List;
import java.util.Map.Entry;

import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.jca.common.api.metadata.common.CommonPool;
import org.jboss.jca.common.api.metadata.common.CommonSecurity;
import org.jboss.jca.common.api.metadata.common.CommonXaPool;
import org.jboss.jca.common.api.metadata.ds.DataSource;
import org.jboss.jca.common.api.metadata.ds.DataSources;
import org.jboss.jca.common.api.metadata.ds.TimeOut;
import org.jboss.jca.common.api.metadata.ds.Validation;
import org.jboss.jca.common.api.metadata.ds.XaDataSource;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;

/**
 * A DataSourcesSubsystemElement.
 * @author <a href="mailto:stefano.maestri@redhat.comdhat.com">Stefano Maestri</a>
 */
final class DataSourcesSubsystemElement extends AbstractSubsystemElement<DataSourcesSubsystemElement> {

    /** The serialVersionUID */
    private static final long serialVersionUID = 6451041006443208660L;

    private DataSources datasources;

    public DataSourcesSubsystemElement() {
        super(Namespace.CURRENT.getUriString());
    }

    @Override
    protected Class<DataSourcesSubsystemElement> getElementClass() {
        return DataSourcesSubsystemElement.class;
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        if (datasources != null && (datasources.getDataSource() != null || datasources.getXaDataSource() != null)) {
            streamWriter.writeStartElement(Element.DATASOURCES.getLocalName());
            if (datasources.getDataSource() != null) {

                for (DataSource ds : datasources.getDataSource()) {
                    writeDsElement(streamWriter, ds);
                }
            }
            if (datasources.getXaDataSource() != null) {
                for (XaDataSource ds : datasources.getXaDataSource()) {
                    writeXADsElement(streamWriter, ds);
                }
            }
            streamWriter.writeEndElement();
        }

        streamWriter.writeEndElement();
    }

    private void writeXADsElement(XMLExtendedStreamWriter streamWriter, XaDataSource ds) throws XMLStreamException {
        streamWriter.writeStartElement(DataSources.Tag.DATASOURCE.getLocalName());
        streamWriter.writeAttribute(XaDataSource.Attribute.JNDINAME.getLocalName(), ds.getJndiName());
        if (ds.isEnabled() != null)
            streamWriter.writeAttribute(XaDataSource.Attribute.ENABLED.getLocalName(), ds.isEnabled().toString());
        if (ds.isUseJavaContext() != null)
            streamWriter.writeAttribute(XaDataSource.Attribute.USEJAVACONTEXT.getLocalName(), ds.isUseJavaContext().toString());
        streamWriter.writeAttribute(XaDataSource.Attribute.POOL_NAME.getLocalName(), ds.getPoolName());

        if (ds.getXaDataSourceProperty() != null) {
            for (Entry<String, String> entry : ds.getXaDataSourceProperty().entrySet()) {
                writeXaProperty(streamWriter, entry);
            }
        }

        if (ds.getXaDataSourceClass() != null) {
            streamWriter.writeStartElement(XaDataSource.Tag.XADATASOURCECLASS.getLocalName());
            streamWriter.writeCharacters(ds.getXaDataSourceClass());
            streamWriter.writeEndElement();
        }

        if (ds.getModule() != null) {
            streamWriter.writeStartElement(XaDataSource.Tag.MODULE.getLocalName());
            streamWriter.writeCharacters(ds.getModule());
            streamWriter.writeEndElement();
        }

        if (ds.getUrlDelimiter() != null) {
            streamWriter.writeStartElement(XaDataSource.Tag.URLDELIMITER.getLocalName());
            streamWriter.writeCharacters(ds.getUrlDelimiter());
            streamWriter.writeEndElement();
        }

        if (ds.getUrlSelectorStrategyClassName() != null) {
            streamWriter.writeStartElement(XaDataSource.Tag.URLSELECTORSTRATEGYCLASSNAME.getLocalName());
            streamWriter.writeCharacters(ds.getUrlSelectorStrategyClassName());
            streamWriter.writeEndElement();
        }

        if (ds.getNewConnectionSql() != null) {
            streamWriter.writeStartElement(XaDataSource.Tag.NEWCONNECTIONSQL.getLocalName());
            streamWriter.writeCharacters(ds.getNewConnectionSql());
            streamWriter.writeEndElement();
        }

        if (ds.getTransactionIsolation() != null) {
            streamWriter.writeStartElement(XaDataSource.Tag.TRANSACTIONISOLATION.getLocalName());
            streamWriter.writeCharacters(ds.getTransactionIsolation().name());
            streamWriter.writeEndElement();
        }

        streamWriter.writeStartElement(XaDataSource.Tag.XA_POOL.getLocalName());
        writeXaPool(streamWriter, ds.getXaPool());
        streamWriter.writeEndElement();

        streamWriter.writeStartElement(XaDataSource.Tag.SECURITY.getLocalName());
        writeSecurity(streamWriter, ds.getSecurity());
        streamWriter.writeEndElement();

        streamWriter.writeStartElement(XaDataSource.Tag.VALIDATION.getLocalName());
        writeValidation(streamWriter, ds.getValidation());
        streamWriter.writeEndElement();

        streamWriter.writeStartElement(XaDataSource.Tag.TIMEOUT.getLocalName());
        writeTimeOut(streamWriter, ds.getTimeOut());
        streamWriter.writeEndElement();

        streamWriter.writeStartElement(XaDataSource.Tag.STATEMENT.getLocalName());
        writeTimeOut(streamWriter, ds.getTimeOut());
        streamWriter.writeEndElement();

        streamWriter.writeEndElement();

    }

    private void writeXaPool(XMLExtendedStreamWriter streamWriter, CommonXaPool xaPool) throws XMLStreamException {
        if (xaPool.getMinPoolSize() != null) {
            streamWriter.writeStartElement(CommonXaPool.Tag.MIN_POOL_SIZE.getLocalName());
            streamWriter.writeCharacters(xaPool.getMinPoolSize().toString());
            streamWriter.writeEndElement();
        }

        if (xaPool.getMaxPoolSize() != null) {
            streamWriter.writeStartElement(CommonXaPool.Tag.MAXPOOLSIZE.getLocalName());
            streamWriter.writeCharacters(xaPool.getMaxPoolSize().toString());
            streamWriter.writeEndElement();
        }

        if (xaPool.isPrefill() != null) {
            streamWriter.writeStartElement(CommonXaPool.Tag.PREFILL.getLocalName());
            streamWriter.writeCharacters(xaPool.isPrefill().toString());
            streamWriter.writeEndElement();
        }

        if (xaPool.isUseStrictMin() != null) {
            streamWriter.writeStartElement(CommonXaPool.Tag.USE_STRICT_MIN.getLocalName());
            streamWriter.writeCharacters(xaPool.isUseStrictMin().toString());
            streamWriter.writeEndElement();
        }

        if (xaPool.isSameRmOverride() != null) {
            streamWriter.writeStartElement(CommonXaPool.Tag.ISSAMERMOVERRIDEVALUE.getLocalName());
            streamWriter.writeCharacters(xaPool.isSameRmOverride().toString());
            streamWriter.writeEndElement();
        }

        if (xaPool.isInterleaving() != null) {
            streamWriter.writeStartElement(CommonXaPool.Tag.INTERLEAVING.getLocalName());
            streamWriter.writeCharacters(xaPool.isInterleaving().toString());
            streamWriter.writeEndElement();
        }

        if (xaPool.isNoTxSeparatePool() != null) {
            streamWriter.writeStartElement(CommonXaPool.Tag.NO_TX_SEPARATE_POOLS.getLocalName());
            streamWriter.writeCharacters(xaPool.isNoTxSeparatePool().toString());
            streamWriter.writeEndElement();
        }

        if (xaPool.isPadXid() != null) {
            streamWriter.writeStartElement(CommonXaPool.Tag.PAD_XID.getLocalName());
            streamWriter.writeCharacters(xaPool.isPadXid().toString());
            streamWriter.writeEndElement();
        }

        if (xaPool.isWrapXaDataSource() != null) {
            streamWriter.writeStartElement(CommonXaPool.Tag.WRAP_XA_RESOURCE.getLocalName());
            streamWriter.writeCharacters(xaPool.isWrapXaDataSource().toString());
            streamWriter.writeEndElement();
        }

    }

    private void writeDsElement(XMLExtendedStreamWriter streamWriter, DataSource ds) throws XMLStreamException {
        streamWriter.writeStartElement(DataSources.Tag.DATASOURCE.getLocalName());
        streamWriter.writeAttribute(DataSource.Attribute.JNDINAME.getLocalName(), ds.getJndiName());
        if (ds.isEnabled() != null)
            streamWriter.writeAttribute(DataSource.Attribute.ENABLED.getLocalName(), ds.isEnabled().toString());
        if (ds.isUseJavaContext() != null)
            streamWriter.writeAttribute(DataSource.Attribute.USEJAVACONTEXT.getLocalName(), ds.isUseJavaContext().toString());
        streamWriter.writeAttribute(DataSource.Attribute.POOL_NAME.getLocalName(), ds.getPoolName());

        if (ds.getConnectionUrl() != null) {
            streamWriter.writeStartElement(DataSource.Tag.CONNECTIONURL.getLocalName());
            streamWriter.writeCharacters(ds.getConnectionUrl());
            streamWriter.writeEndElement();
        }

        if (ds.getDriverClass() != null) {
            streamWriter.writeStartElement(DataSource.Tag.DRIVERCLASS.getLocalName());
            streamWriter.writeCharacters(ds.getDriverClass());
            streamWriter.writeEndElement();
        }

        if (ds.getModule() != null) {
            streamWriter.writeStartElement(DataSource.Tag.MODULE.getLocalName());
            streamWriter.writeCharacters(ds.getModule());
            streamWriter.writeEndElement();
        }

        if (ds.getConnectionProperties() != null) {
            for (Entry<String, String> entry : ds.getConnectionProperties().entrySet()) {
                writeProperty(streamWriter, entry);
            }
        }

        if (ds.getNewConnectionSql() != null) {
            streamWriter.writeStartElement(DataSource.Tag.NEWCONNECTIONSQL.getLocalName());
            streamWriter.writeCharacters(ds.getNewConnectionSql());
            streamWriter.writeEndElement();
        }

        if (ds.getTransactionIsolation() != null) {
            streamWriter.writeStartElement(DataSource.Tag.TRANSACTIONISOLATION.getLocalName());
            streamWriter.writeCharacters(ds.getTransactionIsolation().name());
            streamWriter.writeEndElement();
        }

        if (ds.getUrlDelimiter() != null) {
            streamWriter.writeStartElement(DataSource.Tag.URLDELIMITER.getLocalName());
            streamWriter.writeCharacters(ds.getUrlDelimiter());
            streamWriter.writeEndElement();
        }

        if (ds.getUrlSelectorStrategyClassName() != null) {
            streamWriter.writeStartElement(DataSource.Tag.URLSELECTORSTRATEGYCLASSNAME.getLocalName());
            streamWriter.writeCharacters(ds.getUrlSelectorStrategyClassName());
            streamWriter.writeEndElement();
        }

        streamWriter.writeStartElement(DataSource.Tag.POOL.getLocalName());
        writePool(streamWriter, ds.getPool());
        streamWriter.writeEndElement();

        streamWriter.writeStartElement(DataSource.Tag.SECURITY.getLocalName());
        writeSecurity(streamWriter, ds.getSecurity());
        streamWriter.writeEndElement();

        streamWriter.writeStartElement(DataSource.Tag.VALIDATION.getLocalName());
        writeValidation(streamWriter, ds.getValidation());
        streamWriter.writeEndElement();

        streamWriter.writeStartElement(DataSource.Tag.TIMEOUT.getLocalName());
        writeTimeOut(streamWriter, ds.getTimeOut());
        streamWriter.writeEndElement();

        streamWriter.writeStartElement(DataSource.Tag.STATEMENT.getLocalName());
        writeTimeOut(streamWriter, ds.getTimeOut());
        streamWriter.writeEndElement();

        streamWriter.writeEndElement();

    }

    private void writePool(XMLExtendedStreamWriter streamWriter, CommonPool pool) throws XMLStreamException {
        if (pool.getMinPoolSize() != null) {
            streamWriter.writeStartElement(CommonPool.Tag.MIN_POOL_SIZE.getLocalName());
            streamWriter.writeCharacters(pool.getMinPoolSize().toString());
            streamWriter.writeEndElement();
        }

        if (pool.getMaxPoolSize() != null) {
            streamWriter.writeStartElement(CommonPool.Tag.MAXPOOLSIZE.getLocalName());
            streamWriter.writeCharacters(pool.getMaxPoolSize().toString());
            streamWriter.writeEndElement();
        }

        if (pool.isPrefill() != null) {
            streamWriter.writeStartElement(CommonPool.Tag.PREFILL.getLocalName());
            streamWriter.writeCharacters(pool.isPrefill().toString());
            streamWriter.writeEndElement();
        }

        if (pool.isUseStrictMin() != null) {
            streamWriter.writeStartElement(CommonPool.Tag.USE_STRICT_MIN.getLocalName());
            streamWriter.writeCharacters(pool.isUseStrictMin().toString());
            streamWriter.writeEndElement();
        }

    }

    private void writeValidation(XMLExtendedStreamWriter streamWriter, Validation validation) throws XMLStreamException {
        if (validation.getValidConnectionCheckerClassName() != null) {
            streamWriter.writeStartElement(Validation.Tag.VALIDCONNECTIONCHECKERCLASSNAME.getLocalName());
            streamWriter.writeCharacters(validation.getValidConnectionCheckerClassName());
            streamWriter.writeEndElement();
        }

        if (validation.getCheckValidConnectionSql() != null) {
            streamWriter.writeStartElement(Validation.Tag.CHECKVALIDCONNECTIONSQL.getLocalName());
            streamWriter.writeCharacters(validation.getCheckValidConnectionSql());
            streamWriter.writeEndElement();
        }

        if (validation.isValidateOnMatch() != null) {
            streamWriter.writeStartElement(Validation.Tag.VALIDATEONMATCH.getLocalName());
            streamWriter.writeCharacters(validation.isValidateOnMatch().toString());
            streamWriter.writeEndElement();
        }

        if (validation.isBackgroundValidation() != null) {
            streamWriter.writeStartElement(Validation.Tag.BACKGROUNDVALIDATION.getLocalName());
            streamWriter.writeCharacters(validation.isBackgroundValidation().toString());
            streamWriter.writeEndElement();
        }

        if (validation.getBackgroundValidationMinutes() != null) {
            streamWriter.writeStartElement(Validation.Tag.BACKGROUNDVALIDATIONMINUTES.getLocalName());
            streamWriter.writeCharacters(validation.getBackgroundValidationMinutes().toString());
            streamWriter.writeEndElement();
        }

        if (validation.isUseFastFail() != null) {
            streamWriter.writeStartElement(Validation.Tag.USEFASTFAIL.getLocalName());
            streamWriter.writeCharacters(validation.isUseFastFail().toString());
            streamWriter.writeEndElement();
        }

        if (validation.getStaleConnectionCheckerClassName() != null) {
            streamWriter.writeStartElement(Validation.Tag.STALECONNECTIONCHECKERCLASSNAME.getLocalName());
            streamWriter.writeCharacters(validation.getStaleConnectionCheckerClassName());
            streamWriter.writeEndElement();
        }

        if (validation.getExceptionSorterClassName() != null) {
            streamWriter.writeStartElement(Validation.Tag.EXCEPTIONSORTERCLASSNAME.getLocalName());
            streamWriter.writeCharacters(validation.getExceptionSorterClassName().toString());
            streamWriter.writeEndElement();
        }

    }

    private void writeTimeOut(XMLExtendedStreamWriter streamWriter, TimeOut timeOut) throws XMLStreamException {
        if (timeOut.getBlockingTimeoutMillis() != null) {
            streamWriter.writeStartElement(TimeOut.Tag.BLOCKINGTIMEOUTMILLIS.getLocalName());
            streamWriter.writeCharacters(timeOut.getBlockingTimeoutMillis().toString());
            streamWriter.writeEndElement();
        }

        if (timeOut.getIdleTimeoutMinutes() != null) {
            streamWriter.writeStartElement(TimeOut.Tag.IDLETIMEOUTMINUTES.getLocalName());
            streamWriter.writeCharacters(timeOut.getIdleTimeoutMinutes().toString());
            streamWriter.writeEndElement();
        }

        if (timeOut.isSetTxQueryTimeout() != null) {
            streamWriter.writeStartElement(TimeOut.Tag.SETTXQUERYTIMEOUT.getLocalName());
            streamWriter.writeCharacters(timeOut.isSetTxQueryTimeout().toString());
            streamWriter.writeEndElement();
        }

        if (timeOut.getQueryTimeout() != null) {
            streamWriter.writeStartElement(TimeOut.Tag.QUERYTIMEOUT.getLocalName());
            streamWriter.writeCharacters(timeOut.getQueryTimeout().toString());
            streamWriter.writeEndElement();
        }

        if (timeOut.getUseTryLock() != null) {
            streamWriter.writeStartElement(TimeOut.Tag.USETRYLOCK.getLocalName());
            streamWriter.writeCharacters(timeOut.getUseTryLock().toString());
            streamWriter.writeEndElement();
        }

        if (timeOut.getAllocationRetry() != null) {
            streamWriter.writeStartElement(TimeOut.Tag.ALLOCATIONRETRY.getLocalName());
            streamWriter.writeCharacters(timeOut.getAllocationRetry().toString());
            streamWriter.writeEndElement();
        }

        if (timeOut.getAllocationRetryWaitMillis() != null) {
            streamWriter.writeStartElement(TimeOut.Tag.ALLOCATIONRETRYWAITMILLIS.getLocalName());
            streamWriter.writeCharacters(timeOut.getAllocationRetryWaitMillis().toString());
            streamWriter.writeEndElement();
        }

        if (timeOut.getXaResourceTimeout() != null) {
            streamWriter.writeStartElement(TimeOut.Tag.XARESOURCETIMEOUT.getLocalName());
            streamWriter.writeCharacters(timeOut.getXaResourceTimeout().toString());
            streamWriter.writeEndElement();
        }
    }

    private void writeSecurity(XMLExtendedStreamWriter streamWriter, CommonSecurity security) throws XMLStreamException {
        if (security.getUserName() != null) {
            streamWriter.writeStartElement(CommonSecurity.Tag.USERNAME.getLocalName());
            streamWriter.writeCharacters(security.getUserName());
            streamWriter.writeEndElement();
        }

        if (security.getPassword() != null) {
            streamWriter.writeStartElement(CommonSecurity.Tag.PASSWORD.getLocalName());
            streamWriter.writeCharacters(security.getPassword());
            streamWriter.writeEndElement();
        }

    }

    private void writeProperty(XMLExtendedStreamWriter streamWriter, Entry<String, String> entry) throws XMLStreamException {
        streamWriter.writeStartElement(DataSource.Tag.CONNECTIONPROPERTY.getLocalName());
        streamWriter.writeAttribute("name", entry.getKey());
        streamWriter.writeCharacters(entry.getValue());
        streamWriter.writeEndElement();

    }

    private void writeXaProperty(XMLExtendedStreamWriter streamWriter, Entry<String, String> entry) throws XMLStreamException {
        streamWriter.writeStartElement(XaDataSource.Tag.XADATASOURCEPROPERTY.getLocalName());
        streamWriter.writeAttribute("name", entry.getKey());
        streamWriter.writeCharacters(entry.getValue());
        streamWriter.writeEndElement();

    }

    @Override
    protected void getUpdates(final List<? super AbstractSubsystemUpdate<DataSourcesSubsystemElement, ?>> objects) {
        // empty
    }

    @Override
    protected boolean isEmpty() {
        return true;
    }

    @Override
    protected DataSourcesAdd getAdd() {
        final DataSourcesAdd add = new DataSourcesAdd();
        add.setDatasources(datasources);
        return add;
    }

    @Override
    protected <P> void applyRemove(final UpdateContext updateContext, final UpdateResultHandler<? super Void, P> resultHandler,
            final P param) {
        // requires restart
    }

    public DataSources getDatasources() {
        return datasources;
    }

    public void setDatasources(DataSources datasources) {
        this.datasources = datasources;
    }
}
