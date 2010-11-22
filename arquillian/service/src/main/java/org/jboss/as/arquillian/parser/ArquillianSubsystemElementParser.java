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

package org.jboss.as.arquillian.parser;

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.ExtensionContext.SubsystemConfiguration;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.ParseResult;
import org.jboss.as.model.ParseUtils;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Parser responsible for handling the Arquillian subsystem schema.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 17-Nov-2010
 */
public final class ArquillianSubsystemElementParser implements XMLStreamConstants,
        XMLElementReader<ParseResult<SubsystemConfiguration<ArquillianSubsystemElement>>> {

    @Override
    public void readElement(XMLExtendedStreamReader reader, ParseResult<SubsystemConfiguration<ArquillianSubsystemElement>> result)
            throws XMLStreamException {

        ParseUtils.requireNoAttributes(reader);
        ParseUtils.requireNoContent(reader);

        ArquillianSubsystemAdd add = new ArquillianSubsystemAdd();
        List<AbstractSubsystemUpdate<ArquillianSubsystemElement, ?>> updates = new ArrayList<AbstractSubsystemUpdate<ArquillianSubsystemElement, ?>>();
        updates.add(new ArquillianSubsystemUpdate());

        result.setResult(new SubsystemConfiguration<ArquillianSubsystemElement>(add, updates));
    }
}
