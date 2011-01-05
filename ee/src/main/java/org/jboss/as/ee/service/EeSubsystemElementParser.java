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

package org.jboss.as.ee.service;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.ParseResult;
import org.jboss.as.model.ParseUtils;
import org.jboss.as.server.ExtensionContext;
import org.jboss.as.server.ExtensionContext.SubsystemConfiguration;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * @author Weston M. Price
 *
 */
public class EeSubsystemElementParser implements XMLStreamConstants, XMLElementReader<ParseResult<ExtensionContext.SubsystemConfiguration<EeSubsystemElement>>> {

    @Override
    public void readElement(XMLExtendedStreamReader reader, ParseResult<SubsystemConfiguration<EeSubsystemElement>> result)
            throws XMLStreamException {

        //TODO currently does nothing usefull but allows for the subsystem in configuration
        ParseUtils.requireNoAttributes(reader);
        ParseUtils.requireNoContent(reader);
        result.setResult(new ExtensionContext.SubsystemConfiguration<EeSubsystemElement>(new EeSubsystemAdd()));

    }

}
