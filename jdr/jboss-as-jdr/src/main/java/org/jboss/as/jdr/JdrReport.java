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

package org.jboss.as.jdr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.util.Date;
import java.util.Properties;

import static org.jboss.as.jdr.logger.JdrLogger.ROOT_LOGGER;

/**
 * Provides metadata about and access to the data collected by a {@link JdrReportCollector}.
 *
 * @author Brian Stansberry
 * @author Mike M. Clark
 */
public class JdrReport {

    public static final String JBOSS_PROPERTY_DIR = "jboss.server.data.dir";

    public static final String JDR_PROPERTY_FILE_NAME = "jdr.properties";

    public static final String UUID_NAME = "UUID";

    public static final String JDR_PROPERTIES_COMMENT = "JDR Properties";

    public static final String JBOSS_HOME_DIR = "jboss.home.dir";

    public static final String DEFAULT_PROPERTY_DIR = "standalone";

    public static final String DATA_DIR = "data";

    private Date startTime;
    private Date endTime;
    private String location;
    private String jdrUuid;

    public JdrReport() {
        setJdrUuid();
    }

    /**
     * Indicates the time the JDR report collection was initiated.
     */
    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date time) {
        startTime = time;
    }

    public void setStartTime() {
        setStartTime(new Date());
    }

    /**
     * Indicates the time the JDR report collection was complete.
     */
    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date time) {
        endTime = time;
    }

    public void setEndTime() {
        setEndTime(new Date());
    }

    /**
     * Indicates the location of the generated JDR report.
     *
     * @return location of report.
     */
    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setJdrUuid(String jdrUuid) {
        this.jdrUuid = jdrUuid;
    }

    /**
     * sets JDR UUID using jdr.properties file
     * if the JDR does not have a UUID, then generate one for them and store it in the jdr.properties file
     */
    private void setJdrUuid() {
        String jbossConfig = System.getProperty(JBOSS_PROPERTY_DIR);
        // JDR is being ran from command line
        if(jbossConfig == null) {
            String jbossHome = System.getProperty(JBOSS_HOME_DIR);
            // if JBoss standalone directory does not exist then go no further
            if(!new File(jbossHome + File.separator + DEFAULT_PROPERTY_DIR).exists()) {
                ROOT_LOGGER.error(ROOT_LOGGER.couldNotFindJDRPropertiesFile());
            }
            jbossConfig = jbossHome + File.separator + DEFAULT_PROPERTY_DIR + File.separator + DATA_DIR;
        }
        String jdrPropertiesFilePath = jbossConfig + File.separator + JdrReportExtension.SUBSYSTEM_NAME + File.separator + JDR_PROPERTY_FILE_NAME;
        Properties jdrProperties = new Properties();
        FileInputStream jdrIS = null;
        File jdrFile = new File(jdrPropertiesFilePath);
        if(jdrFile.exists()) {
            try {
                jdrIS = new FileInputStream(jdrPropertiesFilePath);
                jdrProperties.load(jdrIS);
                jdrUuid = jdrProperties.getProperty(UUID_NAME);
                if(jdrUuid == null) {
                    jdrUuid = java.util.UUID.randomUUID().toString();
                    jdrProperties.setProperty(UUID_NAME, jdrUuid);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                if(jdrIS != null) {
                    try {
                        jdrIS.close();
                    }
                    catch(IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            jdrUuid = java.util.UUID.randomUUID().toString();
            jdrProperties.setProperty(UUID_NAME, jdrUuid);
            FileOutputStream fileOut = null;
            try {
                jdrFile.getParentFile().mkdirs();
                fileOut = new FileOutputStream(jdrFile);
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

    public String getJdrUuid() {
        return jdrUuid;
    }
}
