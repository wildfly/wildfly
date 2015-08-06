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
package org.jboss.as.insights.extension;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.Properties;

import javax.ws.rs.core.Response;

import org.jboss.as.jdr.JdrReport;
import org.jboss.as.jdr.JdrReportCollector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import com.redhat.gss.redhat_support_lib.errors.RequestException;
import com.redhat.gss.redhat_support_lib.api.API;

import static org.jboss.as.insights.logger.InsightsLogger.ROOT_LOGGER;

/**
 * @author <a href="mailto:jkinlaw@redhat.com">Josh Kinlaw</a>
 */
public class InsightsService implements Service<InsightsService> {

    public static final long MILLISECOND_TO_DAY = 86400000;

    /**
     * Properties that can be set via the properties file.
     */
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String URL = "url";
    public static final String PROXY_USER = "proxyUser";
    public static final String PROXY_PASSWORD = "proxyPassword";
    public static final String PROXY_URL = "proxyUrl";
    public static final String PROXY_PORT = "proxyPort";
    public static final String USER_AGENT = "userAgent";
    public static final String BASE_URI = "baseUri";
    public static final String TELEMETRY_ENDPOINT = "telemetryEndpoint";
    public static final String SYSTEM_ENDPOINT = "systemEndpoint";

    public static final String JDR_DESCRIPTION = "JDR for UUID {uuid}";

    private static volatile InsightsService instance;

    public static final String JBOSS_PROPERTY_DIR = "jboss.server.data.dir";

    public static final String TELEMETRY_PROPERTY_FILE_NAME = "insights.properties";

    public static final String TELEMETRY_DESCRIPTION = "Properties file consisting of RHN login information and insights/insights URL";

    public static final String DEFAULT_BASE_URL = "https://api.access.redhat.com";
    public static final String DEFAULT_TELEMETRY_ENDPOINT = "/r/insights/v1/uploads/";
    public static final String DEFAULT_SYSTEM_ENDPOINT = "/r/insights/v1/systems/";

    /**
     * Endpoint of url which when added to the end of the url reveals the
     * location of where to send the JDR Should take the format of
     * /insights/endpoint/
     */
    private String insightsEndpoint;

    /**
     * Endponit of url which when added to the end of the url reveals the
     * location of where to query for the current system uuid
     */
    private String systemEndpoint;

    private boolean enabled = true;

    private long frequency = 1;

    private String rhnUid;

    private String rhnPw;

    private JdrReportCollector jdrCollector;

    private Thread output = initializeOutput();

    private API api;

    private InsightsService(long tick, boolean enabled) {
        this.frequency = tick;
        this.enabled = enabled;
        setConnectionManager();
    }

    public static InsightsService getInstance(long tick, boolean enabled) {
        if (instance == null) {
            synchronized (InsightsService.class) {
                if (instance == null) {
                    instance = new InsightsService(tick, enabled);
                }
            }
        } else {
            instance.frequency = tick;
            instance.enabled = enabled;
        }
        return instance;
    }

    private Thread initializeOutput() {
        return new Thread() {
            @Override
            public void run() {
                synchronized (output) {
                    while (enabled) {
                        try {
                            JdrReport report = jdrCollector.collect();
                            sendJdr(report.getLocation(), report.getJdrUuid());
                        }
                        catch (Exception e) {
                            ROOT_LOGGER.couldNotGenerateJdr(e);
                        }
                        try {
                            output.wait(frequency * MILLISECOND_TO_DAY);
                        } catch (InterruptedException e) {
                            ROOT_LOGGER.threadInterrupted(e);
                            return;
                        }
                    }
                }
            }
        };
    }

    @Override
    public InsightsService getValue() throws IllegalStateException,
            IllegalArgumentException {
        return this;
    }

    @Override
    public void start(StartContext arg0) throws StartException {
        output.start();
    }

    @Override
    public void stop(StopContext arg0) {
        if (output != null && output.isAlive()) {
            output.interrupt();
        }
    }

    void setFrequency(long frequency) {
        ROOT_LOGGER.frequencyUpdated("" + frequency);
        this.frequency = frequency;
        File file = getPropertiesFile();
        FileInputStream fis = null;
        FileOutputStream fileOut = null;
        if (file.exists()) {
            synchronized (output) {
                output.notify();
            }
            try {
                Properties properties = new Properties();
                fis = new FileInputStream(file.getPath());
                properties.load(fis);
                properties.setProperty(InsightsExtension.FREQUENCY, ""
                        + frequency);
                fileOut = new FileOutputStream(file);
                properties.store(fileOut, TELEMETRY_DESCRIPTION);
            } catch (IOException e) {
                ROOT_LOGGER.couldNotWriteFrequency(e);
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        ROOT_LOGGER.couldNotClosePropertiesFile(e);
                    }
                }
                if (fileOut != null) {
                    try {
                        fileOut.close();
                    } catch (IOException e) {
                        ROOT_LOGGER.couldNotClosePropertiesFile(e);
                    }
                }
            }
        }
    }

    public long getFrequency() {
        return this.frequency;
    }

    public static ServiceName createServiceName() {
        return ServiceName.JBOSS
                .append(org.jboss.as.insights.extension.InsightsExtension.SUBSYSTEM_NAME);
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled && (output == null || !output.isAlive())) {
            ROOT_LOGGER.insightsEnabled();
            output = initializeOutput();
            try {
                this.start(null);
            } catch (StartException e) {
                ROOT_LOGGER.couldNotStartThread(e);
            }
        } else if (!enabled) {
            ROOT_LOGGER.insightsDisabled();
        }
        synchronized (output) {
            output.notify();
        }
    }

    public void setJdrReportCollector(JdrReportCollector jdrCollector) {
        this.jdrCollector = jdrCollector;
    }

    public void setRhnUid(String rhnUid) {
        this.rhnUid = rhnUid;
    }

    public void setRhnPw(String rhnPw) {
        this.rhnPw = rhnPw;
    }

    public String getRhnUid() {
        return rhnUid;
    }

    public String getRhnPw() {
        return rhnPw;
    }

    private File getPropertiesFile() {
        String jbossConfig = System.getProperty(JBOSS_PROPERTY_DIR);
        String propertiesFilePath = jbossConfig + File.separator
                + InsightsExtension.SUBSYSTEM_NAME + File.separator
                + TELEMETRY_PROPERTY_FILE_NAME;
        return new File(propertiesFilePath);
    }

    /**
     * Set RHN login credentials and write to insights config file
     *
     * @param rhnUid
     * @param rhnPw
     */
    public void setRhnLoginCredentials(String rhnUid, String rhnPw) {
        setRhnUid(rhnUid);
        setRhnPw(rhnPw);
        Properties properties = new Properties();
        FileOutputStream fileOut = null;
        FileInputStream fis = null;
        File file = getPropertiesFile();
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
            } else {
                fis = new FileInputStream(file.getPath());
                properties.load(fis);
            }
            if (!properties.containsKey(URL)) {
                properties.setProperty(URL, DEFAULT_BASE_URL);
            }
            if (!properties.containsKey(TELEMETRY_ENDPOINT)) {
                properties.setProperty(TELEMETRY_ENDPOINT,
                        DEFAULT_TELEMETRY_ENDPOINT);
            }
            if (!properties.containsKey(SYSTEM_ENDPOINT)) {
                properties
                        .setProperty(SYSTEM_ENDPOINT, DEFAULT_SYSTEM_ENDPOINT);
            }
            if (!properties.containsKey(InsightsExtension.FREQUENCY)) {
                properties.setProperty(InsightsExtension.FREQUENCY, ""
                        + frequency);
            }
            if (!properties.containsKey(InsightsExtension.ENABLED)) {
                properties
                        .setProperty(InsightsExtension.ENABLED, "" + enabled);
            }
            properties.setProperty(USERNAME, rhnUid);
            properties.setProperty(PASSWORD, rhnPw);
            fileOut = new FileOutputStream(file);
            properties.store(fileOut, TELEMETRY_DESCRIPTION);
        } catch (IOException e) {
            ROOT_LOGGER.couldNotCreateEditPropertiesFile(e);
        } finally {
            if (fileOut != null) {
                try {
                    fileOut.close();
                    setConnectionManager();
                } catch (IOException e) {
                    ROOT_LOGGER.couldNotClosePropertiesFile(e);
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    ROOT_LOGGER.couldNotClosePropertiesFile(e);
                }
            }
        }
    }

    public void setRhnLoginCredentials(String rhnUid, String rhnPw,
            String proxyUrl, String proxyPort) {
        Properties properties = new Properties();
        FileOutputStream fileOut = null;
        FileInputStream fis = null;
        File file = getPropertiesFile();
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
            } else {
                fis = new FileInputStream(file.getPath());
                properties.load(fis);
            }
            properties.setProperty(InsightsExtension.PROXY_PORT, proxyPort);
            properties.setProperty(InsightsExtension.PROXY_URL, proxyUrl);
            fileOut = new FileOutputStream(file);
            properties.store(fileOut, TELEMETRY_DESCRIPTION);
        } catch (IOException e) {
            ROOT_LOGGER.couldNotCreateEditPropertiesFile(e);
        } finally {
            if (fileOut != null) {
                try {
                    fileOut.close();
                } catch (IOException e) {
                    ROOT_LOGGER.couldNotClosePropertiesFile(e);
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    ROOT_LOGGER.couldNotClosePropertiesFile(e);
                }
            }
        }
        setRhnLoginCredentials(rhnUid, rhnPw);
    }

    public void setRhnLoginCredentials(String rhnUid, String rhnPw,
            String proxyUrl, String proxyPort, String proxyUser, String proxyPwd) {
        Properties properties = new Properties();
        FileOutputStream fileOut = null;
        FileInputStream fis = null;
        File file = getPropertiesFile();
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
            } else {
                fis = new FileInputStream(file.getPath());
                properties.load(fis);
            }
            properties.setProperty(InsightsExtension.PROXY_PASSWORD, proxyPwd);
            properties.setProperty(InsightsExtension.PROXY_USER, proxyUser);
            fileOut = new FileOutputStream(file);
            properties.store(fileOut, TELEMETRY_DESCRIPTION);
        } catch (IOException e) {
            ROOT_LOGGER.couldNotCreateEditPropertiesFile(e);
        } finally {
            if (fileOut != null) {
                try {
                    fileOut.close();
                } catch (IOException e) {
                    ROOT_LOGGER.couldNotClosePropertiesFile(e);
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    ROOT_LOGGER.couldNotClosePropertiesFile(e);
                }
            }
        }
        setRhnLoginCredentials(rhnUid, rhnPw, proxyUrl, proxyPort);
    }

    public void setConnectionManager() {
        String username = "";
        String password = "";
        String url = "";
        String proxyUser = null;
        String proxyPassword = null;
        URL proxyUrl = null;
        int proxyPort = 0;
        String userAgent = null;
        String jbossConfig = System.getProperty(JBOSS_PROPERTY_DIR);
        String propertiesFilePath = jbossConfig + File.separator
                + InsightsExtension.SUBSYSTEM_NAME + File.separator
                + TELEMETRY_PROPERTY_FILE_NAME;
        Properties properties = new Properties();
        File file = new File(propertiesFilePath);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(propertiesFilePath)) {
                properties.load(fis);
                // setting required fields
                username = properties.getProperty(USERNAME);
                password = properties.getProperty(PASSWORD);
                url = properties.getProperty(URL);
                insightsEndpoint = properties.getProperty(TELEMETRY_ENDPOINT);
                systemEndpoint = properties.getProperty(SYSTEM_ENDPOINT);
                api = new API(propertiesFilePath);
                // this.connectionManager = new ConnectionManager(
                // new ConfigHelper(propertiesFilePath));
            } catch (IOException e) {
                ROOT_LOGGER.couldNotLoadPropertiesFile(e);
            }
        }
    }

    private void sendJdr(String fileName, String uuid) {
        String description = JDR_DESCRIPTION.replace("{uuid}", uuid);
        boolean wasSuccessful = true;
        File file = new File(fileName);
        String systemUrl = "";
        try {
            systemUrl = systemEndpoint;
            Response response = api.getInsights().get(systemUrl + uuid);
            com.redhat.gss.redhat_support_lib.infrastructure.System system = response
                    .readEntity(com.redhat.gss.redhat_support_lib.infrastructure.System.class);
            // if system is unregistered then attempt to register system
            if (system.getUnregistered_at() != null) {
                api.getInsights().addSystem(systemUrl, uuid,
                        InetAddress.getLocalHost().getHostName());
            }
        } catch (RequestException e) {
            // if the system was not found then attempt to register the system
            if (e.getMessage().contains(
                    "" + Response.Status.NOT_FOUND.getStatusCode())) {
                try {
                    api.getInsights().addSystem(systemUrl, uuid,
                            InetAddress.getLocalHost().getHostName());
                } catch (RequestException exception) {
                    ROOT_LOGGER.couldNotRegisterSystem(exception);
                } catch (MalformedURLException exception) {
                    ROOT_LOGGER.couldNotRegisterSystem(exception);
                } catch (UnknownHostException exception) {
                    ROOT_LOGGER.couldNotRegisterSystem(exception);
                }
            } else {
                ROOT_LOGGER.couldNotRegisterSystem(e);
            }
        } catch (MalformedURLException e) {
            ROOT_LOGGER.couldNotFindSystem(e);
        } catch (UnknownHostException e) {
            ROOT_LOGGER.couldNotFindSystem(e);
        }
        try {
            api.getInsights().upload((insightsEndpoint + uuid), file,
                    description);
        } catch (FileNotFoundException e) {
            wasSuccessful = false;
            ROOT_LOGGER.couldNotFindJdr(e);
        } catch (MalformedURLException|ParseException|RequestException e) {
            wasSuccessful = false;
            ROOT_LOGGER.couldNotUploadJdr(e);
        }
        if (wasSuccessful) {
            ROOT_LOGGER.jdrSent();
        }
    }
}