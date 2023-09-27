/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
