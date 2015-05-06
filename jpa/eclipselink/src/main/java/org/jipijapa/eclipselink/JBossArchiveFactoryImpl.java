/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
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

package org.jipijapa.eclipselink;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.persistence.internal.jpa.deployment.ArchiveFactoryImpl;
import org.eclipse.persistence.jpa.Archive;

/**
 * The JBossArchiveFactoryImpl provided here allows Eclipse to
 * scan JBoss AS 7 deployments for classes, so clasess don't have
 * to be explicitly listed in persistence.xml .
 *
 * To enable this, set the system property eclipselink.archive.factory
 * to the fully qualified name of this class.
 *
 * See https://community.jboss.org/wiki/HowToUseEclipseLinkWithAS7
 *
 * @author Rich DiCroce
 *
 */
public class JBossArchiveFactoryImpl extends ArchiveFactoryImpl {

    private static final String VFS = "vfs";

    public JBossArchiveFactoryImpl() {
        super();
    }


    public JBossArchiveFactoryImpl(Logger logger) {
        super(logger);
    }

    @Override
    public Archive createArchive(URL rootUrl, String descriptorLocation, @SuppressWarnings("rawtypes") Map properties) throws URISyntaxException, IOException {
        String protocol = rootUrl.getProtocol();
        if (VFS.equals(protocol)) {
            return new VFSArchive(rootUrl, descriptorLocation);
        } else {
            return super.createArchive(rootUrl, descriptorLocation, properties);
        }
    }

}
