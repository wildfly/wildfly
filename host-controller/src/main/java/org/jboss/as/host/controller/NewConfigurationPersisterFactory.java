/*
* JBoss, Home of Professional Open Source.
* Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.host.controller;

import java.io.File;
import java.util.List;

import javax.xml.namespace.QName;

import org.jboss.as.controller.parsing.DomainXml;
import org.jboss.as.controller.parsing.HostXml;
import org.jboss.as.controller.parsing.Namespace;
import org.jboss.as.controller.persistence.BackupXmlConfigurationPersister;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.controller.persistence.ModelMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class NewConfigurationPersisterFactory {

    private static final String HOST_XML = "host.xml";

    static ExtensibleConfigurationPersister createHostXmlConfigurationPersister(final File configDir) {
        HostXml hostXml = new HostXml(Module.getSystemModuleLoader());
        return new TempHackConfigurationPersister(getFile(configDir, HOST_XML), new QName(Namespace.CURRENT.getUriString(), "host"), hostXml, hostXml);
    }

    static ExtensibleConfigurationPersister createDomainXmlConfigurationPersister(final File configDir) {
        DomainXml domainXml = new DomainXml(Module.getSystemModuleLoader());
        return new TempHackConfigurationPersister(getFile(configDir, HOST_XML), new QName(Namespace.CURRENT.getUriString(), "domain"), domainXml, domainXml);
    }

    private static File getFile(final File configDir, final String file) {
        if (configDir == null)
            throw new IllegalArgumentException("Domain configuration directory is null");
        File configFile = new File(configDir, HOST_XML);
        if (configFile.exists()) {
            if (configFile.isDirectory()) {
                throw new IllegalArgumentException(configFile.getAbsolutePath() + " is a directory");
            }
        }
        else {
            if (configFile.isDirectory() || !configDir.canWrite()) {
                throw new IllegalArgumentException(configFile.getAbsolutePath() + " is not a writable");
            }
        }
        return configFile;

    }



    /** Disables store() until marshallers are written */
    private static class TempHackConfigurationPersister extends BackupXmlConfigurationPersister {

        public TempHackConfigurationPersister(final File fileName, final QName rootElement,
                XMLElementReader<List<ModelNode>> rootParser, XMLElementWriter<ModelMarshallingContext> rootDeparser) {
            super(fileName, rootElement, rootParser, rootDeparser);
        }

        @Override
        public void store(ModelNode model) throws ConfigurationPersistenceException {
            return;
        }

    }

}
