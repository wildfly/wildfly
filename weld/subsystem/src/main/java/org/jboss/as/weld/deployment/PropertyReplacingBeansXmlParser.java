/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.deployment;

import java.net.URL;

import org.jboss.as.ee.structure.SpecDescriptorPropertyReplacement;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.metadata.property.PropertyReplacer;
import org.jboss.weld.xml.BeansXmlParser;

/**
 * Fork of {@link org.jboss.weld.xml.BeansXmlParser} to support standard WildFly deployment descriptor expression resolution.
 *
 * @author Stuart Douglas
 * @author Pete Muir
 * @author Jozef Hartinger
 */
public class PropertyReplacingBeansXmlParser extends BeansXmlParser {

    private final PropertyReplacer replacer;

    public PropertyReplacingBeansXmlParser(DeploymentUnit deploymentUnit, boolean legacyEmptyBeansXmlTreatment) {
        super(legacyEmptyBeansXmlTreatment);
        this.replacer = SpecDescriptorPropertyReplacement.propertyReplacer(deploymentUnit);
    }

    protected PropertyReplacingBeansXmlHandler getHandler(final URL beansXml) {
        return new PropertyReplacingBeansXmlHandler(beansXml, replacer);
    }
}
