/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.microprofile.faulttolerance.tck;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.container.ClassContainer;
import org.jboss.shrinkwrap.api.container.LibraryContainer;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.impl.base.io.IOUtil;

/**
 * Ported from SR FT.
 *
 * @author Radoslav Husar
 */
public class FaultToleranceApplicationArchiveProcessor implements ApplicationArchiveProcessor {

    private static final Logger LOGGER = Logger.getLogger(FaultToleranceApplicationArchiveProcessor.class.getName());

    private static final String MAX_THREADS_OVERRIDE = "io.smallrye.faulttolerance.globalThreadPoolSize=1000";
    private static final String MP_CONFIG_PATH = "/WEB-INF/classes/META-INF/microprofile-config.properties";

    @Override
    public void process(Archive<?> applicationArchive, TestClass testClass) {
        if (!(applicationArchive instanceof ClassContainer)) {
            LOGGER.warning(
                    "Unable to add additional classes - not a class/resource container: "
                            + applicationArchive);
            return;
        }
        ClassContainer<?> classContainer = (ClassContainer<?>) applicationArchive;

        if (applicationArchive instanceof LibraryContainer) {
            JavaArchive additionalBeanArchive = ShrinkWrap.create(JavaArchive.class);
            additionalBeanArchive.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
            ((LibraryContainer<?>) applicationArchive).addAsLibrary(additionalBeanArchive);
        } else {
            classContainer.addAsResource(EmptyAsset.INSTANCE, "META-INF/beans.xml");
        }

        if (!applicationArchive.contains("META-INF/beans.xml")) {
            applicationArchive.add(EmptyAsset.INSTANCE, "META-INF/beans.xml");
        }

        String config;
        if (!applicationArchive.contains(MP_CONFIG_PATH)) {
            config = MAX_THREADS_OVERRIDE;
        } else {
            ByteArrayOutputStream output = readCurrentConfig(applicationArchive);
            applicationArchive.delete(MP_CONFIG_PATH);
            config = output.toString() + "\n" + MAX_THREADS_OVERRIDE;
        }
        classContainer.addAsResource(new StringAsset(config), MP_CONFIG_PATH);

        LOGGER.info("Added additional resources to " + applicationArchive.toString(true));
    }

    private ByteArrayOutputStream readCurrentConfig(Archive<?> appArchive) {
        try {
            Node node = appArchive.get(MP_CONFIG_PATH);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            IOUtil.copy(node.getAsset().openStream(), outputStream);
            return outputStream;
        } catch (IOException e) {
            throw new RuntimeException("Failed to prepare microprofile-config.properties");
        }
    }
}
