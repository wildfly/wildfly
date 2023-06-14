/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
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

package org.jboss.as.pojo.descriptor;

import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.xml.IntVersionSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.as.controller.xml.XMLElementSchema;
import org.jboss.as.pojo.ParseResult;
import org.jboss.as.pojo.logging.PojoLogger;
import org.jboss.staxmapper.IntVersion;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Enumerates supported namespaces of the POJO deployment descriptor.
 * @author Paul Ferraro
 */
public enum BeanDeploymentSchema implements XMLElementSchema<BeanDeploymentSchema, ParseResult<KernelDeploymentXmlDescriptor>> {

    VERSION_1_0("bean-deployer", 1),
    VERSION_2_0("bean-deployer", 2),
    VERSION_7_0("pojo", 7),
    ;
    static final BeanDeploymentSchema CURRENT = VERSION_7_0;

    private final VersionedNamespace<IntVersion, BeanDeploymentSchema> namespace;

    BeanDeploymentSchema(String nss, int major) {
        this.namespace = IntVersionSchema.createURN(List.of(IntVersionSchema.JBOSS_IDENTIFIER, nss), new IntVersion(major));
    }

    @Override
    public VersionedNamespace<IntVersion, BeanDeploymentSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public String getLocalName() {
        return "deployment";
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, ParseResult<KernelDeploymentXmlDescriptor> result) throws XMLStreamException {
        if (!this.since(CURRENT)) {
            PojoLogger.ROOT_LOGGER.oldNamespace(this.namespace.getUri());
        }
        new KernelDeploymentXmlDescriptorParser(this).readElement(reader, result);
    }
}
