/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

import org.jboss.as.pojo.ParseResult;
import org.jboss.as.pojo.logging.PojoLogger;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import javax.xml.stream.XMLStreamException;

/**
 * Parse legacy (1.0 and 2.0 schema) Microcontainer jboss-beans.xml.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class LegacyKernelDeploymentXmlDescriptorParser extends KernelDeploymentXmlDescriptorParser {

    public static final String MC_NAMESPACE_1_0 = "urn:jboss:bean-deployer:1.0";
    public static final String MC_NAMESPACE_2_0 = "urn:jboss:bean-deployer:2.0";

    @Override
    public void readElement(XMLExtendedStreamReader reader, ParseResult<KernelDeploymentXmlDescriptor> value) throws XMLStreamException {
        PojoLogger.ROOT_LOGGER.oldNamespace(reader.getNamespaceURI());
        super.readElement(reader, value);
    }
}