/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow.session;

import org.jboss.as.ee.structure.JBossDescriptorPropertyReplacement;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.jbossallxml.JBossAllXMLParser;
import org.jboss.as.web.session.SharedSessionManagerConfig;
import org.jboss.metadata.property.PropertyReplacer;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.wildfly.extension.undertow.logging.UndertowLogger;

import javax.xml.stream.XMLStreamException;

/**
 * Parse shared session manager config
 *
 * @author Stuart Douglas
 */
public class SharedSessionConfigParser implements JBossAllXMLParser<SharedSessionManagerConfig> {

    private final SharedSessionConfigSchema schema;

    public SharedSessionConfigParser(SharedSessionConfigSchema schema) {
        this.schema = schema;
    }

    @Override
    public SharedSessionManagerConfig parse(XMLExtendedStreamReader reader, DeploymentUnit unit) throws XMLStreamException {
        if (unit.getParent() != null) {
            UndertowLogger.ROOT_LOGGER.sharedSessionConfigNotInRootDeployment(unit.getName());
            return null;
        }
        PropertyReplacer replacer = JBossDescriptorPropertyReplacement.propertyReplacer(unit);
        SharedSessionManagerConfig result = new SharedSessionManagerConfig();
        new SharedSessionConfigXMLReader(this.schema, replacer).readElement(reader, result);
        return result;
    }
}
