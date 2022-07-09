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

package org.jboss.as.weld.deployment.processors;

import java.lang.reflect.Constructor;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.weld.deployment.PropertyReplacingBeansXmlParser;
import org.jboss.logging.Logger;
import org.jboss.weld.xml.BeansXmlParser;

/**
 * Temporary factory for creating instances of BeansXmlParser. This class won't be needed once Weld 5 is a direct
 * dependency of non-preview WFLY.
 */
final class BeansXmlParserFactory {

    private static final Logger log = Logger.getLogger(BeansXmlParserFactory.class.getPackage().getName());

    private static final Constructor<? extends BeansXmlParser> constructor;
    static {
        String cname = PropertyReplacingBeansXmlParser.class.getPackage().getName() + ".AlternativePropertyReplacingBeansXmlParser";
        Constructor<? extends BeansXmlParser> ctor = null;
        try {
            @SuppressWarnings("unchecked")
            Class<? extends BeansXmlParser> clazz = (Class<? extends BeansXmlParser>) BeansXmlParserFactory.class.getClassLoader().loadClass(cname);
            ctor = clazz.getDeclaredConstructor(DeploymentUnit.class, boolean.class);
            log.debugf("Found constructor for %s", cname);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            // ignore
            log.debugf(e, "Cannot find constructor for %s", cname);
        }
        constructor = ctor;
    }

    static BeansXmlParser getPropertyReplacingParser(DeploymentUnit deploymentUnit, boolean legacyBeansXmlTreatment) throws DeploymentUnitProcessingException {
        if (constructor == null) {
            return new PropertyReplacingBeansXmlParser(deploymentUnit);
        }
        try {
            return constructor.newInstance(deploymentUnit, legacyBeansXmlTreatment);
        } catch (ReflectiveOperationException e) {
            throw new DeploymentUnitProcessingException(e);
        }
    }
}
