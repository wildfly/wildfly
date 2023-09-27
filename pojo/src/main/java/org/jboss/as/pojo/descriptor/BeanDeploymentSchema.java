/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
