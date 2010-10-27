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

package org.jboss.as.resourceadapters;

import java.util.List;
import java.util.Map.Entry;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.jca.common.api.metadata.common.CommonAdminObject;
import org.jboss.jca.common.api.metadata.common.CommonConnDef;
import org.jboss.jca.common.api.metadata.common.CommonPool;
import org.jboss.jca.common.api.metadata.common.CommonSecurity;
import org.jboss.jca.common.api.metadata.common.CommonTimeOut;
import org.jboss.jca.common.api.metadata.common.CommonValidation;
import org.jboss.jca.common.api.metadata.common.CommonXaPool;
import org.jboss.jca.common.api.metadata.resourceadapter.ResourceAdapter;
import org.jboss.jca.common.api.metadata.resourceadapter.ResourceAdapters;
import org.jboss.jca.common.metadata.common.CommonPoolImpl;
import org.jboss.jca.common.metadata.common.CommonXaPoolImpl;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * A DataSourcesSubsystemElement.
 * @author <a href="mailto:stefano.maestri@redhat.comdhat.com">Stefano Maestri</a>
 */
final class ResourceAdaptersSubsystemElement extends AbstractSubsystemElement<ResourceAdaptersSubsystemElement> {

    /** The serialVersionUID */
    private static final long serialVersionUID = 6451041006443208660L;

    private ResourceAdapters resourceAdapters;

    public ResourceAdaptersSubsystemElement() {
        super(Namespace.CURRENT.getUriString());
    }

    @Override
    protected Class<ResourceAdaptersSubsystemElement> getElementClass() {
        return ResourceAdaptersSubsystemElement.class;
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {

        if (resourceAdapters != null && resourceAdapters.getResourceAdapters() != null) {
            streamWriter.writeStartElement(Element.RESOURCE_ADAPTERS.getLocalName());
            for (ResourceAdapter ra : resourceAdapters.getResourceAdapters()) {
                writeRaElement(streamWriter, ra);
            }
            streamWriter.writeEndElement();
        }
        streamWriter.writeEndElement();
    }

    private void writeRaElement(XMLExtendedStreamWriter streamWriter, ResourceAdapter ra) throws XMLStreamException {
        streamWriter.writeStartElement(ResourceAdapters.Tag.RESOURCE_ADPTER.getLocalName());

        streamWriter.writeStartElement(ResourceAdapter.Tag.ARCHIVE.getLocalName());
        streamWriter.writeCharacters(ra.getArchive());
        streamWriter.writeEndElement();

        if (ra.getBeanValidationGroups() != null) {
            streamWriter.writeStartElement(ResourceAdapter.Tag.BEAN_VALIDATION_GROUPS.getLocalName());
            for (String beanValidationGroup : ra.getBeanValidationGroups()) {
                streamWriter.writeStartElement(ResourceAdapter.Tag.BEAN_VALIDATION_GROUP.getLocalName());
                streamWriter.writeCharacters(beanValidationGroup);
                streamWriter.writeEndElement();
            }
            streamWriter.writeEndElement();
        }

        if (ra.getBootstrapContext() != null) {
            streamWriter.writeStartElement(ResourceAdapter.Tag.BOOTSTRAP_CONTEXT.getLocalName());
            streamWriter.writeCharacters(ra.getBootstrapContext());
            streamWriter.writeEndElement();
        }

        if (ra.getConfigProperties() != null) {
            for (Entry<String, String> entry : ra.getConfigProperties().entrySet()) {
                writeConfigProperty(streamWriter, entry);
            }
        }

        if (ra.getTransactionSupport() != null) {
            streamWriter.writeStartElement(ResourceAdapter.Tag.TRANSACTION_SUPPORT.getLocalName());
            streamWriter.writeCharacters(ra.getTransactionSupport().name());
            streamWriter.writeEndElement();
        }

        if (ra.getConnectionDefinitions() != null) {
            streamWriter.writeStartElement(ResourceAdapter.Tag.CONNECTION_DEFINITIONS.getLocalName());
            for (CommonConnDef conDef : ra.getConnectionDefinitions()) {
                writeConDef(streamWriter, conDef);
            }
            streamWriter.writeEndElement();
        }

        if (ra.getAdminObjects() != null) {
            streamWriter.writeStartElement(ResourceAdapter.Tag.ADMIN_OBJECTS.getLocalName());
            for (CommonAdminObject adminObject : ra.getAdminObjects()) {
                writeAdminObject(streamWriter, adminObject);
            }
            streamWriter.writeEndElement();
        }
        streamWriter.writeEndElement();

    }

    private void writeAdminObject(XMLExtendedStreamWriter streamWriter, CommonAdminObject adminObject)
            throws XMLStreamException {
        streamWriter.writeStartElement(ResourceAdapter.Tag.ADMIN_OBJECT.getLocalName());
        streamWriter.writeAttribute(CommonAdminObject.Attribute.CLASS_NAME.getLocalName(), adminObject.getClassName());
        streamWriter.writeAttribute(CommonAdminObject.Attribute.JNDINAME.getLocalName(), adminObject.getJndiName());
        if (adminObject.isEnabled() != null)
            streamWriter.writeAttribute(CommonAdminObject.Attribute.ENABLED.getLocalName(), adminObject.isEnabled().toString());
        if (adminObject.isUseJavaContext() != null)
            streamWriter.writeAttribute(CommonAdminObject.Attribute.USEJAVACONTEXT.getLocalName(), adminObject
                    .isUseJavaContext().toString());
        streamWriter.writeAttribute(CommonAdminObject.Attribute.POOL_NAME.getLocalName(), adminObject.getPoolName());

        if (adminObject.getConfigProperties() != null) {
            for (Entry<String, String> entry : adminObject.getConfigProperties().entrySet()) {
                writeConfigProperty(streamWriter, entry);
            }
        }

    }

    private void writeConDef(XMLExtendedStreamWriter streamWriter, CommonConnDef conDef) throws XMLStreamException {
        streamWriter.writeStartElement(ResourceAdapter.Tag.CONNECTION_DEFINITION.getLocalName());
        streamWriter.writeAttribute(CommonConnDef.Attribute.CLASS_NAME.getLocalName(), conDef.getClassName());
        streamWriter.writeAttribute(CommonConnDef.Attribute.JNDINAME.getLocalName(), conDef.getJndiName());
        if (conDef.isEnabled() != null)
            streamWriter.writeAttribute(CommonConnDef.Attribute.ENABLED.getLocalName(), conDef.isEnabled().toString());
        if (conDef.isUseJavaContext() != null)
            streamWriter.writeAttribute(CommonConnDef.Attribute.USEJAVACONTEXT.getLocalName(), conDef.isUseJavaContext()
                    .toString());
        if (conDef.getPoolName() != null)
            streamWriter.writeAttribute(CommonConnDef.Attribute.POOL_NAME.getLocalName(), conDef.getPoolName());

        if (conDef.getConfigProperties() != null) {
            for (Entry<String, String> entry : conDef.getConfigProperties().entrySet()) {
                writeConfigProperty(streamWriter, entry);
            }
        }

        if (conDef.getPool() != null) {
            if (conDef.getPool() instanceof CommonPoolImpl) {
                streamWriter.writeStartElement(CommonConnDef.Tag.POOL.getLocalName());
                CommonPoolImpl pool = (CommonPoolImpl) conDef.getPool();
                writeCommonPool(streamWriter, pool);
                streamWriter.writeEndElement();
            } else if (conDef.getPool() instanceof CommonXaPoolImpl) {
                streamWriter.writeStartElement(CommonConnDef.Tag.XA_POOL.getLocalName());
                CommonXaPoolImpl pool = (CommonXaPoolImpl) conDef.getPool();
                writeCommonPool(streamWriter, pool);

                if (pool.isSameRmOverride() != null) {
                    streamWriter.writeStartElement(CommonXaPool.Tag.ISSAMERMOVERRIDEVALUE.getLocalName());
                    streamWriter.writeCharacters(pool.isSameRmOverride().toString());
                    streamWriter.writeEndElement();
                }

                if (pool.isInterleaving() != null) {
                    streamWriter.writeStartElement(CommonXaPool.Tag.INTERLEAVING.getLocalName());
                    streamWriter.writeCharacters(pool.isInterleaving().toString());
                    streamWriter.writeEndElement();
                }

                if (pool.isNoTxSeparatePool() != null) {
                    streamWriter.writeStartElement(CommonXaPool.Tag.NO_TX_SEPARATE_POOLS.getLocalName());
                    streamWriter.writeCharacters(pool.isNoTxSeparatePool().toString());
                    streamWriter.writeEndElement();
                }

                if (pool.isPadXid() != null) {
                    streamWriter.writeStartElement(CommonXaPool.Tag.PAD_XID.getLocalName());
                    streamWriter.writeCharacters(pool.isPadXid().toString());
                    streamWriter.writeEndElement();
                }

                if (pool.isWrapXaDataSource() != null) {
                    streamWriter.writeStartElement(CommonXaPool.Tag.WRAP_XA_RESOURCE.getLocalName());
                    streamWriter.writeCharacters(pool.isWrapXaDataSource().toString());
                    streamWriter.writeEndElement();
                }

                streamWriter.writeEndElement();
            }
        }

        if (conDef.getSecurity() != null) {
            streamWriter.writeStartElement(CommonConnDef.Tag.SECURITY.getLocalName());
            writeSecurity(streamWriter, conDef.getSecurity());
            streamWriter.writeEndElement();
        }

        if (conDef.getTimeOut() != null) {
            streamWriter.writeStartElement(CommonConnDef.Tag.TIMEOUT.getLocalName());
            writeTimeOut(streamWriter, conDef.getTimeOut());
            streamWriter.writeEndElement();
        }

        if (conDef.getValidation() != null) {
            streamWriter.writeStartElement(CommonConnDef.Tag.VALIDATION.getLocalName());
            writeValidation(streamWriter, conDef.getValidation());
            streamWriter.writeEndElement();
        }

        streamWriter.writeEndElement();

    }

    private void writeValidation(XMLExtendedStreamWriter streamWriter, CommonValidation validation) throws XMLStreamException {
        if (validation.isBackgroundValidation() != null) {
            streamWriter.writeStartElement(CommonValidation.Tag.BACKGROUNDVALIDATION.getLocalName());
            streamWriter.writeCharacters(validation.isBackgroundValidation().toString());
            streamWriter.writeEndElement();
        }

        if (validation.getBackgroundValidationMinutes() != null) {
            streamWriter.writeStartElement(CommonValidation.Tag.BACKGROUNDVALIDATIONMINUTES.getLocalName());
            streamWriter.writeCharacters(validation.getBackgroundValidationMinutes().toString());
            streamWriter.writeEndElement();
        }

        if (validation.isUseFastFail() != null) {
            streamWriter.writeStartElement(CommonValidation.Tag.USEFASTFAIL.getLocalName());
            streamWriter.writeCharacters(validation.isUseFastFail().toString());
            streamWriter.writeEndElement();
        }

    }

    private void writeTimeOut(XMLExtendedStreamWriter streamWriter, CommonTimeOut timeOut) throws XMLStreamException {
        if (timeOut.getBlockingTimeoutMillis() != null) {
            streamWriter.writeStartElement(CommonTimeOut.Tag.BLOCKINGTIMEOUTMILLIS.getLocalName());
            streamWriter.writeCharacters(timeOut.getBlockingTimeoutMillis().toString());
            streamWriter.writeEndElement();
        }

        if (timeOut.getIdleTimeoutMinutes() != null) {
            streamWriter.writeStartElement(CommonTimeOut.Tag.IDLETIMEOUTMINUTES.getLocalName());
            streamWriter.writeCharacters(timeOut.getIdleTimeoutMinutes().toString());
            streamWriter.writeEndElement();
        }

        if (timeOut.getAllocationRetry() != null) {
            streamWriter.writeStartElement(CommonTimeOut.Tag.ALLOCATIONRETRY.getLocalName());
            streamWriter.writeCharacters(timeOut.getAllocationRetry().toString());
            streamWriter.writeEndElement();
        }

        if (timeOut.getAllocationRetryWaitMillis() != null) {
            streamWriter.writeStartElement(CommonTimeOut.Tag.ALLOCATIONRETRYWAITMILLIS.getLocalName());
            streamWriter.writeCharacters(timeOut.getAllocationRetryWaitMillis().toString());
            streamWriter.writeEndElement();
        }

        if (timeOut.getXaResourceTimeout() != null) {
            streamWriter.writeStartElement(CommonTimeOut.Tag.XARESOURCETIMEOUT.getLocalName());
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

    private void writeCommonPool(XMLExtendedStreamWriter streamWriter, CommonPoolImpl pool) throws XMLStreamException {
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

    private void writeConfigProperty(XMLExtendedStreamWriter streamWriter, Entry<String, String> entry)
            throws XMLStreamException {
        streamWriter.writeStartElement(ResourceAdapter.Tag.CONFIG_PROPERTY.getLocalName());
        streamWriter.writeAttribute("name", entry.getKey());
        streamWriter.writeCharacters(entry.getValue());
        streamWriter.writeEndElement();

    }

    @Override
    protected void getUpdates(final List<? super AbstractSubsystemUpdate<ResourceAdaptersSubsystemElement, ?>> objects) {
        // empty
    }

    @Override
    protected boolean isEmpty() {
        return true;
    }

    @Override
    protected ResourceAdaptersAdd getAdd() {
        final ResourceAdaptersAdd add = new ResourceAdaptersAdd();
        add.setResourceAdapters(resourceAdapters);
        return add;
    }

    @Override
    protected <P> void applyRemove(final UpdateContext updateContext, final UpdateResultHandler<? super Void, P> resultHandler,
            final P param) {
        // requires restart
    }

    public ResourceAdapters getResourceAdapters() {
        return resourceAdapters;
    }

    public void setResourceAdapters(ResourceAdapters resourceAdapters) {
        this.resourceAdapters = resourceAdapters;
    }
}
