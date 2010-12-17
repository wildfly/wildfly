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

package org.jboss.as.server.deployment.scanner;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author John Bailey
 */
public class DeploymentScannerSubsystemElement extends AbstractSubsystemElement<DeploymentScannerSubsystemElement> {

    public static final String DEFAULT_STANDALONE_PATH = "standalone/deployments";

    private final Map<String, DeploymentScannerElement> scanners = new LinkedHashMap<String, DeploymentScannerElement>();

    public DeploymentScannerSubsystemElement() {
        super(Namespace.CURRENT.getUriString());
    }

    /** {@inheritDoc} */
    @Override
    protected Class<DeploymentScannerSubsystemElement> getElementClass() {
        return DeploymentScannerSubsystemElement.class;
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        for(DeploymentScannerElement scannerElement : scanners.values()) {
            streamWriter.writeStartElement(Namespace.CURRENT.getUriString(), Element.DEPLOYMENT_SCANNER.getLocalName());
            scannerElement.writeContent(streamWriter);
        }
        streamWriter.writeEndElement();
    }

    /** {@inheritDoc} */
    protected void getUpdates(List<? super AbstractSubsystemUpdate<DeploymentScannerSubsystemElement, ?>> list) {
        // none
    }

    /** {@inheritDoc} */
    protected boolean isEmpty() {
        return true;
    }

    protected DeploymentScannerSubsystemAdd getAdd() {
        return new DeploymentScannerSubsystemAdd();
    }

    protected <P> void applyRemove(final UpdateContext updateContext, final UpdateResultHandler<? super Void, P> resultHandler, final P param) {
        // require a restart!
    }

    void addScanner(final DeploymentScannerElement scanner) {
        scanners.put(scanner.getPath(), scanner);
    }

    DeploymentScannerElement getScanner(final String path) {
        return scanners.get(path);
    }

    DeploymentScannerElement removeScanner(final String path) {
        return scanners.remove(path);
    }
}
