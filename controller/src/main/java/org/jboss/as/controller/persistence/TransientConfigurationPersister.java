/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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

import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Set;

/**
 * Do not store the new model.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class TransientConfigurationPersister extends XmlConfigurationPersister {
    /**
     * Construct a new instance.
     *
     * @param file         the configuration file
     * @param rootElement  the root element of the configuration file
     * @param rootParser   the root model parser
     * @param rootDeparser the root model deparser
     */
    public TransientConfigurationPersister(final ConfigurationFile file, final QName rootElement, final XMLElementReader<List<ModelNode>> rootParser, final XMLElementWriter<ModelMarshallingContext> rootDeparser) {
        super(file.getBootFile(), rootElement, rootParser, rootDeparser);
    }

    @Override
    public PersistenceResource store(ModelNode model, Set<PathAddress> affectedAddresses) throws ConfigurationPersistenceException {
        // do not store the new model
        return NullPersistenceResource.INSTANCE;
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
