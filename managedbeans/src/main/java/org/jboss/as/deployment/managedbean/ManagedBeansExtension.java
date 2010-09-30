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

package org.jboss.as.deployment.managedbean;

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
 * Domain extension used to initialize the managed bean subsystem element handlers.
 *
 * @author John E. Bailey
 * @author Emanuel Muckenhuber
 */
public class ManagedBeansExtension implements Extension {

    public static final String NAMESPACE = "urn:jboss:domain:managedbeans:1.0";

    final ManagedBeanSubsystemElementParser PARSER = new ManagedBeanSubsystemElementParser();
    final SubsystemFactory<ManagedBeansSubsystemElement> FACTORY = new SubsystemFactory<ManagedBeansSubsystemElement>() {
        public ManagedBeansSubsystemElement createSubsystemElement() {
            return new ManagedBeansSubsystemElement();
        }
    };

    /** {@inheritDoc} */
    public void initialize(ExtensionContext context) {
        context.registerSubsystem(NAMESPACE, FACTORY, PARSER);
    }

    /**
     * Activate the extension.
     *
     * @param context the service activation context
     */
    public void activate(final ServiceActivatorContext context) {
    }

    static class ManagedBeanSubsystemElementParser implements XMLElementReader<List<? super AbstractSubsystemUpdate<ManagedBeansSubsystemElement, ?>>> {

        /** {@inheritDoc} */
        public void readElement(XMLExtendedStreamReader reader,
                List<? super AbstractSubsystemUpdate<ManagedBeansSubsystemElement, ?>> arg1) throws XMLStreamException {
            // Require no content
            ParseUtils.requireNoContent(reader);
        }

    }
}
