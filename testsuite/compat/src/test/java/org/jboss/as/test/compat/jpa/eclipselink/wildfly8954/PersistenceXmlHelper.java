/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.compat.jpa.eclipselink.wildfly8954;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.jboss.as.test.compat.util.SystemTestStringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This factory allows us to build strings representing the persistence.xml files.
 */
public class PersistenceXmlHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(PersistenceXmlHelper.class);

    public static PersistenceXmlHelper SINGLETON = new PersistenceXmlHelper();

    private PersistenceXmlHelper() {

    }

    /**
     * Reurn a persistence xml as string.
     *
     * <P>
     * NOTE: <br>
     * The persistence.xml is to be found src/test/resources under the appropriate package.
     *
     *
     * @return A string that can conveniently be wrapped into a String resource to be assembled into a progrmatic war file.
     */
    public String getWFLY8954BaseTestPersistenceXml() {
        // (a) Build up a folder classpath - path
        String packageWithFowardSlashes = WFLY8954BaseTest.class.getPackage().getName().replaceAll("[.]", "/");
        // (b) Build up a classpath search expression
        String clasppathSearchPath = String.format("/%1$s/persistence.xml", packageWithFowardSlashes);
        // (c) Search the classpath for the persistence xml we want to use in our test
        URL pathToPersistenceXml = WFLY8954BaseTest.class.getResource(clasppathSearchPath);
        if (pathToPersistenceXml == null) {
            String errMsg = String.format("Could not find classpath resource: %1$s", pathToPersistenceXml);
            throw new RuntimeException(errMsg);
        }
        LOGGER.info("Going to use the persistence.xml located at {} for system test. ", pathToPersistenceXml);

        // (d) trivially read the file into a string
        File fileToRead = new File(mapUrlToTuri(pathToPersistenceXml));
        return SystemTestStringUtil.SINGLETON.getFileAsString(fileToRead);
    }

    /**
     * Wrapper method to save us from anoying try catch blocks.
     *
     * @param url url to convert to uri
     * @return uri associated to url
     */
    private URI mapUrlToTuri(URL url) {
        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
