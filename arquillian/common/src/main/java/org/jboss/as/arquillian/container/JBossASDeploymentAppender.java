/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.arquillian.container;

import org.jboss.arquillian.container.test.spi.client.deployment.AuxiliaryArchiveAppender;
import org.jboss.arquillian.test.spi.TestEnricher;
import org.jboss.arquillian.testenricher.cdi.CDIInjectionEnricher;
import org.jboss.arquillian.testenricher.ejb.EJBInjectionEnricher;
import org.jboss.arquillian.testenricher.resource.ResourceInjectionEnricher;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * EmbeddedDeploymentAppender
 *
 * Package the required dependencies needed by the Jboss Embedded Container plugin to run in container.
 *
 * @author <a href="mailto:aslak@conduct.no">Aslak Knutsen</a>
 */
public class JBossASDeploymentAppender implements AuxiliaryArchiveAppender {

    public Archive<?> createAuxiliaryArchive() {
        JavaArchive archive = ShrinkWrap
                .create(JavaArchive.class, "arquillian-jboss-testenrichers.jar")
                .addPackages(true, EJBInjectionEnricher.class.getPackage(), ResourceInjectionEnricher.class.getPackage(),
                        CDIInjectionEnricher.class.getPackage())
                .addAsServiceProvider(TestEnricher.class, CDIInjectionEnricher.class, EJBInjectionEnricher.class,
                        ResourceInjectionEnricher.class);
        return archive;
    }

}
