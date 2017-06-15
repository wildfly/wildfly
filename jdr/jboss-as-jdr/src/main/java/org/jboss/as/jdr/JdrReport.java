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

import static org.jboss.as.jdr.logger.JdrLogger.ROOT_LOGGER;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.jboss.dmr.ModelNode;

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

    private Long startTime;
    private Long endTime;
    private String location;
    private String jdrUuid;

    private static DateFormat DATE_FORMAT = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");

    public JdrReport() {
    }

    public JdrReport(ModelNode result) {
        setStartTime(result.get("start-time").asLong());
        setEndTime(result.get("end-time").asLong());
        setLocation(result.get("report-location").asString());
    }

    /**
     * Indicates the time the JDR report collection was initiated.
     */
    public Long getStartTime() {
        return startTime;
    }

    public String getFormattedStartTime() {
        if(startTime == null)
            return "";
        return DATE_FORMAT.format(startTime);
    }

    public void setStartTime(Date date) {
        startTime = date.getTime();
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setStartTime() {
        setStartTime(new Date().getTime());
    }

    /**
     * Indicates the time the JDR report collection was complete.
     */
    public Long getEndTime() {
        return endTime;
    }

    public String getFormattedEndTime() {
        if(endTime == null)
            return "";
        return DATE_FORMAT.format(endTime);
    }

    public void setEndTime(Date date) {
        endTime = date.getTime();
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public void setEndTime() {
        setEndTime(new Date().getTime());
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
        String jbossConfig = System.getProperty(JBOSS_PROPERTY_DIR);
        Path jbossConfigPath;
        // JDR is being ran from command line
        if (jbossConfig == null) {
            String jbossHome = System.getProperty(JBOSS_HOME_DIR);
            // if JBoss standalone directory does not exist then go no further
            Path defaultDir = new File(jbossHome, DEFAULT_PROPERTY_DIR).toPath();
            if (Files.notExists(defaultDir)) {
                ROOT_LOGGER.couldNotFindJDRPropertiesFile();
            }
            jbossConfigPath = defaultDir.resolve(DATA_DIR);
        } else {
            jbossConfigPath = new File(jbossConfig).toPath();
        }
        Path jdrPropertiesFilePath = jbossConfigPath.resolve(JdrReportExtension.SUBSYSTEM_NAME).resolve(JDR_PROPERTY_FILE_NAME);
        Properties jdrProperties = new Properties();
        try {
            Files.createDirectories(jdrPropertiesFilePath.getParent());
        } catch (IOException e) {
            ROOT_LOGGER.couldNotCreateJDRPropertiesFile(e, jdrPropertiesFilePath);
        }
        if (jdrUuid == null && Files.exists(jdrPropertiesFilePath)) {
            try (InputStream in = Files.newInputStream(jdrPropertiesFilePath)) {
                jdrProperties.load(in);
                this.jdrUuid = jdrProperties.getProperty(UUID_NAME);
            } catch (IOException e) {
                ROOT_LOGGER.couldNotFindJDRPropertiesFile();
            }
        } else {
            try (OutputStream fileOut = Files.newOutputStream(jdrPropertiesFilePath, StandardOpenOption.CREATE)) {
                jdrProperties.setProperty(UUID_NAME, jdrUuid);
                jdrProperties.store(fileOut, JDR_PROPERTIES_COMMENT);
            } catch (IOException e) {
                ROOT_LOGGER.couldNotCreateJDRPropertiesFile(e, jdrPropertiesFilePath);
            }
        }
    }

    public String getJdrUuid() {
        return jdrUuid;
    }
}
