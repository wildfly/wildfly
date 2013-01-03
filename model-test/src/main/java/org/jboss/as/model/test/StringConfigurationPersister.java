/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.model.test;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.persistence.AbstractConfigurationPersister;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.ModelMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementWriter;

/**
 * Internal class used to marshall/read the model
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class StringConfigurationPersister extends AbstractConfigurationPersister {

    private final List<ModelNode> bootOperations;
    volatile String marshalled;

    public StringConfigurationPersister(List<ModelNode> bootOperations, XMLElementWriter<ModelMarshallingContext> rootDeparser) {
        super(rootDeparser);
        this.bootOperations = bootOperations;
    }

    @Override
    public PersistenceResource store(ModelNode model, Set<PathAddress> affectedAddresses)
            throws ConfigurationPersistenceException {
        return new StringPersistenceResource(model, this);
    }

    @Override
    public List<ModelNode> load() throws ConfigurationPersistenceException {
        return bootOperations;
    }

    public List<ModelNode> getBootOperations(){
        return bootOperations;
    }

    public String getMarshalled() {
        return marshalled;
    }

    private class StringPersistenceResource implements PersistenceResource {
        private byte[] bytes;

        StringPersistenceResource(final ModelNode model, final AbstractConfigurationPersister persister) throws ConfigurationPersistenceException {
            ByteArrayOutputStream output = new ByteArrayOutputStream(1024 * 8);
            try {
                try {
                    persister.marshallAsXml(model, output);
                } finally {
                    try {
                        output.close();
                    } catch (Exception ignore) {
                    }
                    bytes = output.toByteArray();
                }
            } catch (Exception e) {
                throw new ConfigurationPersistenceException("Failed to marshal configuration", e);
            }
        }

        @Override
        public void commit() {
            marshalled = new String(bytes);
        }

        @Override
        public void rollback() {
            marshalled = null;
        }
    }
}