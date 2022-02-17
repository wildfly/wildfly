/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.weld.deployment;

import java.net.URL;

import org.jboss.as.ee.structure.SpecDescriptorPropertyReplacement;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.metadata.property.PropertyReplacer;
import org.jboss.weld.xml.BeansXmlParser;

/**
 * Alternative to {@link PropertyReplacingBeansXmlParser} that provides CDI 2 / CDI 3
 * handling of empty beans.xml files instead of the new default CDI 4 behavior
 * discussed in https://github.com/eclipse-ee4j/cdi/issues/500.
 */
public final class EmptyDiscoveryBeansXmlParser  extends BeansXmlParser {

    private final PropertyReplacer replacer;

    public EmptyDiscoveryBeansXmlParser(DeploymentUnit deploymentUnit) {
        super(true);
        this.replacer = SpecDescriptorPropertyReplacement.propertyReplacer(deploymentUnit);
    }

    protected PropertyReplacingBeansXmlHandler getHandler(final URL beansXml) {
        return new PropertyReplacingBeansXmlHandler(beansXml, replacer);
    }
}
