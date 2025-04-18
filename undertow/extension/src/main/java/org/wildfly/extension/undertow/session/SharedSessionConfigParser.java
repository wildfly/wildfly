/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
