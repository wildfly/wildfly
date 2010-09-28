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

package org.jboss.as;

import java.util.List;
import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.staxmapper.XMLElementReader;

/**
 * The extension context.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ExtensionContext {

    /**
     * Register a subsystem.
     *
     * TODO: consider a mechanism to register alias namespaces.
     *
     * @param namespaceUri the subsystem namespace URI
     * @param factory the subsystem element factory
     * @param reader the XML element reader which parses the subsystem XML
     */
    <E extends AbstractSubsystemElement<E>> void registerSubsystem(String namespaceUri, SubsystemFactory<E> factory, XMLElementReader<List<? super AbstractSubsystemUpdate<E, ?>>> reader);
}
