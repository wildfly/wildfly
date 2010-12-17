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

import javax.xml.stream.XMLStreamException;
import org.jboss.as.model.AbstractModelElement;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author John Bailey
 */
public class DeploymentScannerElement extends AbstractModelElement<DeploymentScannerElement> {

    public static final String DEFAULT_STANDALONE_PATH = "standalone/deployments";

    private String name;
    private String path;
    private String relativeTo;
    private int interval = 0;
    private boolean enabled = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getRelativeTo() {
        return relativeTo;
    }

    public void setRelativeTo(String relativeTo) {
        this.relativeTo = relativeTo;
    }

    public int getScanInterval() {
        return interval;
    }

    void setInterval(int interval) {
        this.interval = interval;
    }

    public boolean isScanEnabled() {
        return enabled;
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** {@inheritDoc} */
    @Override
    protected Class<DeploymentScannerElement> getElementClass() {
        return DeploymentScannerElement.class;
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        if (name != null) {
            streamWriter.writeAttribute(Attribute.NAME.getLocalName(), name);
        }
        if (interval != 0) {
            streamWriter.writeAttribute(Attribute.SCAN_INTERVAL.getLocalName(), String.valueOf(interval));
        }
        if (!enabled) {
            streamWriter.writeAttribute(Attribute.SCAN_ENABLED.getLocalName(), "false");
        }
        if (relativeTo != null) {
            streamWriter.writeAttribute(Attribute.RELATIVE_TO.getLocalName(), relativeTo);
        }
        streamWriter.writeAttribute(Attribute.PATH.getLocalName(), path);
        streamWriter.writeEndElement();
    }

    protected <P> void applyRemove(final UpdateContext updateContext, final UpdateResultHandler<? super Void, P> resultHandler, final P param) {
        // require a restart!
    }
}
