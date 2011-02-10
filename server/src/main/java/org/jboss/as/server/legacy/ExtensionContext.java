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

package org.jboss.as.server.legacy;

import java.util.Arrays;
import java.util.List;
import org.jboss.as.model.AbstractSubsystemAdd;
import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.ParseResult;
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
     * @param reader the XML element reader which parses the subsystem XML
     */
    <E extends AbstractSubsystemElement<E>> void registerSubsystem(String namespaceUri, XMLElementReader<ParseResult<SubsystemConfiguration<E>>> reader);

    /**
     * A parsed subsystem configuration.
     *
     * @param <E> the element type
     */
    class SubsystemConfiguration<E extends AbstractSubsystemElement<E>> {
        private final AbstractSubsystemAdd<E> subsystemAdd;
        private final List<AbstractSubsystemUpdate<E, ?>> updates;

        public SubsystemConfiguration(final AbstractSubsystemAdd<E> subsystemAdd, final List<AbstractSubsystemUpdate<E, ?>> updates) {
            this.subsystemAdd = subsystemAdd;
            this.updates = updates;
        }

        public SubsystemConfiguration(final AbstractSubsystemAdd<E> subsystemAdd, final AbstractSubsystemUpdate<E, ?>... updates) {
            this.subsystemAdd = subsystemAdd;
            this.updates = Arrays.asList(updates);
        }

        public AbstractSubsystemAdd<E> getSubsystemAdd() {
            return subsystemAdd;
        }

        public List<AbstractSubsystemUpdate<E, ?>> getUpdates() {
            return updates;
        }
    }
}
