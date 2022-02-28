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
 * Temporary alternative to {@link PropertyReplacingBeansXmlParser} that can work with Weld 5 API and can therefore
 * choose between current and legacy behavior when parsing empty beans.xml files.
 * See also https://github.com/eclipse-ee4j/cdi/issues/500
 *
 * This class will no longer be needed once Weld 5 becomes a direct dependency for non-preview WFLY.
 * The functionality should then be merged into {@link PropertyReplacingBeansXmlParser}
 */
public final class AlternativePropertyReplacingBeansXmlParser extends BeansXmlParser {

    private final PropertyReplacer replacer;

    public AlternativePropertyReplacingBeansXmlParser(DeploymentUnit deploymentUnit, boolean legacyEmptyBeansXmlTreatment) {
        super(legacyEmptyBeansXmlTreatment);
        this.replacer = SpecDescriptorPropertyReplacement.propertyReplacer(deploymentUnit);
    }

    protected PropertyReplacingBeansXmlHandler getHandler(final URL beansXml) {
        return new PropertyReplacingBeansXmlHandler(beansXml, replacer);
    }
}
