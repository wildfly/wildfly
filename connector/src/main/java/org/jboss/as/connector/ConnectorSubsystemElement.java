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

package org.jboss.as.connector;

import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractSubsystemAdd;
import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * A ConnectorSubsystemElement.
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 */
final class ConnectorSubsystemElement extends AbstractSubsystemElement<ConnectorSubsystemElement> {

    /** The serialVersionUID */
    private static final long serialVersionUID = 6451041006443208660L;

    private boolean archiveValidation = true;
    private boolean archiveValidationFailOnError = true;
    private boolean archiveValidationFailOnWarn = false;
    private boolean beanValidation = true;

    private String longRunningThreadPool;

    private String shortRunningThreadPool;

    public ConnectorSubsystemElement() {
        super(Namespace.CONNECTOR_1_0.getUriString());
    }

    @Override
    protected Class<ConnectorSubsystemElement> getElementClass() {
        return ConnectorSubsystemElement.class;
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeEmptyElement(Element.ARCHIVE_VALIDATION.getLocalName());
        streamWriter.writeAttribute(Attribute.ENABLED.getLocalName(), Boolean.toString(archiveValidation));
        streamWriter.writeAttribute(Attribute.FAIL_ON_WARN.getLocalName(), Boolean.toString(archiveValidationFailOnWarn));
        streamWriter.writeAttribute(Attribute.FAIL_ON_ERROR.getLocalName(), Boolean.toString(archiveValidationFailOnError));

        streamWriter.writeEmptyElement(Element.BEAN_VALIDATION.getLocalName());
        streamWriter.writeAttribute(Attribute.ENABLED.getLocalName(), Boolean.toString(beanValidation));

        streamWriter.writeEmptyElement(Element.DEFAULT_WORKMANAGER.getLocalName());
        streamWriter.writeAttribute(Attribute.SHORT_RUNNING_THREAD_POOL.getLocalName(), shortRunningThreadPool);
        streamWriter.writeAttribute(Attribute.LONG_RUNNING_THREAD_POOL.getLocalName(), longRunningThreadPool);

        streamWriter.writeEndElement();
    }

    @Override
    protected void getUpdates(final List<? super AbstractSubsystemUpdate<ConnectorSubsystemElement, ?>> objects) {
        // empty
    }

    @Override
    protected boolean isEmpty() {
        return true;
    }

    @Override
    protected AbstractSubsystemAdd<ConnectorSubsystemElement> getAdd() {
        final ConnectorSubsystemAdd add = new ConnectorSubsystemAdd();
        add.setArchiveValidation(archiveValidation);
        add.setArchiveValidationFailOnError(archiveValidationFailOnError);
        add.setArchiveValidationFailOnWarn(archiveValidationFailOnWarn);
        add.setBeanValidation(beanValidation);
        add.setLongRunningThreadPool(longRunningThreadPool);
        add.setShortRunningThreadPool(shortRunningThreadPool);
        return add;
    }

    @Override
    protected <P> void applyRemove(final UpdateContext updateContext, final UpdateResultHandler<? super Void, P> resultHandler,
            final P param) {
        // requires restart
    }

    public boolean isArchiveValidation() {
        return archiveValidation;
    }

    public void setArchiveValidation(boolean archiveValidation) {
        this.archiveValidation = archiveValidation;
    }

    public boolean isArchiveValidationFailOnError() {
        return archiveValidationFailOnError;
    }

    public void setArchiveValidationFailOnError(boolean archiveValidationFailOnError) {
        this.archiveValidationFailOnError = archiveValidationFailOnError;
    }

    public boolean isArchiveValidationFailOnWarn() {
        return archiveValidationFailOnWarn;
    }

    public void setArchiveValidationFailOnWarn(boolean archiveValidationFailOnWarn) {
        this.archiveValidationFailOnWarn = archiveValidationFailOnWarn;
    }

    public boolean isBeanValidation() {
        return beanValidation;
    }

    public void setBeanValidation(boolean beanValidation) {
        this.beanValidation = beanValidation;
    }

    public String getLongRunningThreadPool() {
        return longRunningThreadPool;
    }

    public void setLongRunningThreadPool(String longRunningThreadPool) {
        this.longRunningThreadPool = longRunningThreadPool;
    }

    public String getShortRunningThreadPool() {
        return shortRunningThreadPool;
    }

    public void setShortRunningThreadPool(String shortRunningThreadPool) {
        this.shortRunningThreadPool = shortRunningThreadPool;
    }
}
