/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jdr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.jboss.as.controller.SimpleResourceDefinition;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class JdrReportSubsystemDefinition extends SimpleResourceDefinition {

    public static final String JBOSS_PROPERTY_DIR = "jboss.server.data.dir";

    public static final String JDR_PROPERTY_FILE_NAME = "jdr.properties";

    public static final String UUID_NAME = "UUID";

    public static final String JDR_PROPERTIES_COMMENT = "JDR Properties";

    static final JdrReportSubsystemDefinition INSTANCE = new JdrReportSubsystemDefinition();

    private String uuid;

    private JdrReportSubsystemDefinition() {
        super(JdrReportExtension.SUBSYSTEM_PATH, JdrReportExtension.getResourceDescriptionResolver(),
                JdrReportSubsystemAdd.INSTANCE,
                JdrReportSubsystemRemove.INSTANCE);
        setUUID();
        System.out.println("UUID: " + uuid);
    }

    private void setUUID() {
        String jbossConfig = System.getProperty(JBOSS_PROPERTY_DIR);
        String jdrPropertiesFilePath = jbossConfig + File.separator + JdrReportExtension.SUBSYSTEM_NAME + File.separator + JDR_PROPERTY_FILE_NAME;
        Properties jdrProperties = new Properties();
        InputStream jdrIS = getClass().getClassLoader().getResourceAsStream(jdrPropertiesFilePath);
        if(jdrIS != null) {
            try {
                jdrProperties.load(jdrIS);
                uuid = jdrProperties.getProperty(UUID_NAME);
                if(uuid == null) {
                    uuid = java.util.UUID.randomUUID().toString();
                    jdrProperties.setProperty(UUID_NAME, uuid);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            uuid = java.util.UUID.randomUUID().toString();
            jdrProperties.setProperty(UUID_NAME, java.util.UUID.randomUUID().toString());
            FileOutputStream fileOut = null;
            try {
                File file = new File(jdrPropertiesFilePath);
                file.getParentFile().mkdirs();
                fileOut = new FileOutputStream(jdrPropertiesFilePath);
                jdrProperties.store(fileOut, JDR_PROPERTIES_COMMENT);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                if(fileOut != null) {
                    try {
                        fileOut.close();
                    }
                    catch(IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public String getUUID() {
        return uuid;
    }
}