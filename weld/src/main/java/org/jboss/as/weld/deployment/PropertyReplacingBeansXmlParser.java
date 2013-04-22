/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.weld.deployment;

import java.net.URL;

import org.jboss.as.ee.structure.SpecDescriptorPropertyReplacement;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.metadata.property.PropertyReplacer;
import org.jboss.weld.xml.BeansXmlParser;

/**
 * Fork of {@link org.jboss.weld.xml.BeansXmlParser} to fix some minor XML parsing issues.
 *
 * @author Stuart Douglas
 * @author Pete Muir
 * @author Jozef Hartinger
 */
public class PropertyReplacingBeansXmlParser extends BeansXmlParser {

    private final PropertyReplacer replacer;

    public PropertyReplacingBeansXmlParser(DeploymentUnit deploymentUnit) {
        this.replacer = SpecDescriptorPropertyReplacement.propertyReplacer(deploymentUnit);
    }

    protected PropertyReplacingBeansXmlHandler getHandler(final URL beansXml) {
        return new PropertyReplacingBeansXmlHandler(beansXml, replacer);
    }
}
