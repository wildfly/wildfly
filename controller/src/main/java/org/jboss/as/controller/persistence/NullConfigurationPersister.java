/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.persistence;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementWriter;

/**
 * A configuration persister which does not store configuration changes.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class NullConfigurationPersister extends AbstractConfigurationPersister {

    public NullConfigurationPersister() {
        super(null);
    }

    public NullConfigurationPersister(XMLElementWriter<ModelMarshallingContext> rootDeparser) {
        super(rootDeparser);
    }

    /** {@inheritDoc} */
    @Override
    public PersistenceResource store(final ModelNode model, Set<PathAddress> affectedAddresses) {
        return NullPersistenceResource.INSTANCE;
    }

    /** {@inheritDoc} */
    @Override
    public List<ModelNode> load() {
        return Collections.emptyList();
    }

    private static class NullPersistenceResource implements ConfigurationPersister.PersistenceResource {

        private static final NullPersistenceResource INSTANCE = new NullPersistenceResource();

        @Override
        public void commit() {
        }

        @Override
        public void rollback() {
        }
    }
}
