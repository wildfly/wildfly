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

package org.jboss.as.weld;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.ParseResult;
import org.jboss.as.model.ParseUtils;
import org.jboss.as.server.Extension;
import org.jboss.as.server.ExtensionContext;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Domain extension used to initialize the weld subsystem element handlers.
 *
 * @author Stuart Douglas
 */
public class WeldExtension implements Extension {

    public static final String NAMESPACE = "urn:jboss:domain:weld:1.0";

    private static final Logger log = Logger.getLogger("org.jboss.weld");

    final WeldSubsystemElementParser PARSER = new WeldSubsystemElementParser();

    /** {@inheritDoc} */
    public void initialize(ExtensionContext context) {
        context.registerSubsystem(NAMESPACE, PARSER);
    }

    /** {@inheritDoc} */
    public void activate(final ServiceActivatorContext context) {
        log.info("Activating Weld Extension");
    }

    static class WeldSubsystemElementParser implements
            XMLElementReader<ParseResult<ExtensionContext.SubsystemConfiguration<WeldSubsystemElement>>> {

        /** {@inheritDoc} */
        public void readElement(XMLExtendedStreamReader reader,
                ParseResult<ExtensionContext.SubsystemConfiguration<WeldSubsystemElement>> result) throws XMLStreamException {
            ParseUtils.requireNoAttributes(reader);
            ParseUtils.requireNoContent(reader);
            result.setResult(new ExtensionContext.SubsystemConfiguration<WeldSubsystemElement>(new WeldSubsystemAdd()));
        }

    }
}
