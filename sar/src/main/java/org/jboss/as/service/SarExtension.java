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

package org.jboss.as.service;

import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.Extension;
import org.jboss.as.ExtensionContext;
import org.jboss.as.SubsystemFactory;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.ParseUtils;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Extension used to enable SAR deployments.
 *
 * @author John Bailey
 */
public class SarExtension implements Extension {

    public static final String NAMESPACE = "urn:jboss:domain:sar:1.0";

    static final SarSubSystemElementParser PARSER = new SarSubSystemElementParser();
    static final SubsystemFactory<SarSubsystemElement> FACTORY = new SubsystemFactory<SarSubsystemElement>() {
        public SarSubsystemElement createSubsystemElement() {
            return new SarSubsystemElement();
        }
    };

    /** {@inheritDoc} */
    public void initialize(ExtensionContext context) {
        context.registerSubsystem(NAMESPACE, PARSER);
    }

    /**
     * Activate the extension.
     *
     * @param context the service activation context
     */
    public void activate(final ServiceActivatorContext context) {
        //
    }


    static class SarSubSystemElementParser implements XMLElementReader<List<? super AbstractSubsystemUpdate<SarSubsystemElement, ?>>> {

        /** {@inheritDoc} */
        public void readElement(XMLExtendedStreamReader reader, List<? super AbstractSubsystemUpdate<SarSubsystemElement, ?>> arg1)
                throws XMLStreamException {
            // Require no content
            ParseUtils.requireNoContent(reader);
        }
    }
}
