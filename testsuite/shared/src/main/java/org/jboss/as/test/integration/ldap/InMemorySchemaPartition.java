/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ldap;

import java.net.URL;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.schema.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.extractor.impl.ResourceMap;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.partition.ldif.AbstractLdifPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In-memory schema-only partition which loads the data in the similar way as the
 * {@link org.apache.directory.api.ldap.schemaloader.JarLdifSchemaLoader}.
 *
 * @author Josef Cacek
 */
public class InMemorySchemaPartition extends AbstractLdifPartition {

    private static Logger LOG = LoggerFactory.getLogger(InMemorySchemaPartition.class);

    /**
     * Filesystem path separator pattern, either forward slash or backslash. java.util.regex.Pattern is immutable so only one
     * instance is needed for all uses.
     */

    public InMemorySchemaPartition(SchemaManager schemaManager) {
        super(schemaManager);
    }

    /**
     * Partition initialization - loads schema entries from the files on classpath.
     *
     * @see org.apache.directory.server.core.partition.impl.avl.AvlPartition#doInit()
     */
    @Override
    protected void doInit() throws Exception {
        if (initialized)
            return;

        LOG.debug("Initializing schema partition " + getId());
        suffixDn.apply(schemaManager);
        super.doInit();

        // load schema
        final Map<String, Boolean> resMap = ResourceMap.getResources(Pattern.compile("schema[/\\Q\\\\E]ou=schema.*"));
        for (String resourcePath : new TreeSet<String>(resMap.keySet())) {
            if (resourcePath.endsWith(".ldif")) {
                URL resource = DefaultSchemaLdifExtractor.getUniqueResource(resourcePath, "Schema LDIF file");
                LdifReader reader = new LdifReader(resource.openStream());
                LdifEntry ldifEntry = reader.next();
                reader.close();

                Entry entry = new DefaultEntry(schemaManager, ldifEntry.getEntry());
                // add mandatory attributes
                if (entry.get(SchemaConstants.ENTRY_CSN_AT) == null) {
                    entry.add(SchemaConstants.ENTRY_CSN_AT, defaultCSNFactory.newInstance().toString());
                }
                if (entry.get(SchemaConstants.ENTRY_UUID_AT) == null) {
                    entry.add(SchemaConstants.ENTRY_UUID_AT, UUID.randomUUID().toString());
                }
                AddOperationContext addContext = new AddOperationContext(null, entry);
                super.add(addContext);
            }
        }
    }

}
