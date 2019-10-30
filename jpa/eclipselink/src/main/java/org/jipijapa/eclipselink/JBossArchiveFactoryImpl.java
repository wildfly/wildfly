/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
