package org.jboss.as.telemetry.extension;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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
import com.redhat.gss.redhat_support_lib.helpers.ConfigHelper;
import com.redhat.gss.redhat_support_lib.infrastructure.Telemetries;
import com.redhat.gss.redhat_support_lib.web.ConnectionManager;

import static org.jboss.as.telemetry.logger.TelemetryLogger.ROOT_LOGGER;

/**
 * @author <a href="mailto:jkinlaw@redhat.com">Josh Kinlaw</a>
 */
public class TelemetryService extends Telemetries implements
        Service<TelemetryService> {

//    public static final long MILLISECOND_TO_DAY = 86400000;
    public static final long MILLISECOND_TO_DAY = 1; //for testing purposes

    public static final String JBOSS_PROPERTY_DIR = "jboss.server.data.dir";

    public static final String TELEMETRY_PROPERTY_FILE_NAME = "telemetry.properties";

    public static final String TELEMETRY_DESCRIPTION = "Properties file consisting of RHN login information and telemetry/insights URL";

    public static final String DEFAULT_BASE_URL = "https://api.access.redhat.com";
    public static final String DEFAULT_TELEMETRY_ENDPOINT = "/r/insights/v1/uploads/";
    public static final String DEFAULT_SYSTEM_ENDPOINT = "/r/insights/v1/systems/";

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

    private static volatile TelemetryService instance;

    public static final String JBOSS_PROPERTY_DIR = "jboss.server.data.dir";

    public static final String TELEMETRY_PROPERTY_FILE_NAME = "telemetry.properties";

    public static final String TELEMETRY_DESCRIPTION = "Properties file consisting of RHN login information and telemetry/insights URL";

    public static final String DEFAULT_BASE_URL = "https://api.access.redhat.com";
    public static final String DEFAULT_TELEMETRY_ENDPOINT = "/r/insights/v1/uploads/";
    public static final String DEFAULT_SYSTEM_ENDPOINT = "/r/insights/v1/systems/";

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

    /**
     * Endpoint of url which when added to the end of the url reveals the
     * location of where to send the JDR Should take the format of
     * /telemetry/endpoint/
     */
    private String telemetryEndpoint;

    /**
     * Endponit of url which when added to the end of the url reveals the
     * location of where to query for the current system uuid
     */
    private String systemEndpoint;

    private ConnectionManager connectionManager = null;

    private boolean enabled = true;

    private long frequency = 1;

    private String rhnUid;

    private String rhnPw;

    private JdrReportCollector jdrCollector;

    private Thread output = initializeOutput();

    private TelemetryService(long tick, boolean enabled) {
        this.frequency = tick;
        this.enabled = enabled;
        setConnectionManager();
    }

    public static TelemetryService getInstance(long tick, boolean enabled) {
        if (instance == null) {
            synchronized (TelemetryService.class) {
                if (instance == null) {
                    instance = new TelemetryService(tick, enabled);
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
                            sendJdr(report.getLocation());
                            output.wait(frequency * MILLISECOND_TO_DAY);
                        } catch (InterruptedException e) {
                            ROOT_LOGGER.threadInterrupted(e);
                            return;
                        } catch (Exception e) {
                            ROOT_LOGGER.couldNotGenerateJdr(e);
                            try {
                                output.wait(frequency * MILLISECOND_TO_DAY);
                            } catch (InterruptedException e2) {
                                ROOT_LOGGER.threadInterrupted(e);
                            }
                        }
                    }
                }
            }
        };
    }

    @Override
    public TelemetryService getValue() throws IllegalStateException,
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
        if(file.exists()) {
            synchronized (output) {
                output.notify();
            }
            try {
                Properties properties = new Properties();
                fis = new FileInputStream(file.getPath());
                properties.load(fis);
                properties.setProperty(TelemetryExtension.FREQUENCY, "" + frequency);
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
                .append(org.jboss.as.telemetry.extension.TelemetryExtension.SUBSYSTEM_NAME);
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled && (output == null || !output.isAlive())) {
            ROOT_LOGGER.telemetryEnabled();
            output = initializeOutput();
            try {
                this.start(null);
            } catch (StartException e) {
                ROOT_LOGGER.couldNotStartThread(e);
            }
        } else if (!enabled) {
            ROOT_LOGGER.telemetryDisabled();
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
                + TelemetryExtension.SUBSYSTEM_NAME + File.separator
                + TELEMETRY_PROPERTY_FILE_NAME;
        return new File(propertiesFilePath);
    }

    /**
     * Set RHN login credentials and write to telemetry config file
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
                properties.setProperty(URL, DEFAULT_BASE_URL);
                properties.setProperty(TELEMETRY_ENDPOINT,
                        DEFAULT_TELEMETRY_ENDPOINT);
                properties.setProperty(SYSTEM_ENDPOINT, DEFAULT_SYSTEM_ENDPOINT);
                properties.setProperty(TelemetryExtension.FREQUENCY, "" + frequency);
                properties.setProperty(TelemetryExtension.ENABLED, "" + enabled);
            } else {
                fis = new FileInputStream(file.getPath());
                properties.load(fis);
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
    public void setTelemetryUri() {
        String jbossConfig = System.getProperty(JBOSS_PROPERTY_DIR);
        String propertiesFilePath = jbossConfig + File.separator
                + TelemetryExtension.SUBSYSTEM_NAME + File.separator
                + TELEMETRY_PROPERTY_FILE_NAME;
        Properties properties = new Properties();
        FileOutputStream fileOut = null;
        FileInputStream fis = null;
        File file = getPropertiesFile();
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                properties.setProperty(URL, DEFAULT_BASE_URL);
                properties.setProperty(TELEMETRY_ENDPOINT,
                        DEFAULT_TELEMETRY_ENDPOINT);
                properties.setProperty(SYSTEM_ENDPOINT, DEFAULT_SYSTEM_ENDPOINT);
                properties.setProperty(TelemetryExtension.FREQUENCY, "" + frequency);
                properties.setProperty(TelemetryExtension.ENABLED, "" + enabled);
            } else {
                fis = new FileInputStream(file.getPath());
                properties.load(fis);
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
                + TelemetryExtension.SUBSYSTEM_NAME + File.separator
                + TELEMETRY_PROPERTY_FILE_NAME;
        Properties properties = new Properties();
        FileInputStream fis = null;
        File file = new File(propertiesFilePath);
        if (file.exists()) {
            try {
                fis = new FileInputStream(propertiesFilePath);
                properties.load(fis);
                // setting required fields
                username = properties.getProperty(USERNAME);
                password = properties.getProperty(PASSWORD);
                url = properties.getProperty(URL);
                telemetryEndpoint = properties.getProperty(TELEMETRY_ENDPOINT);
                systemEndpoint = properties.getProperty(SYSTEM_ENDPOINT);

                this.connectionManager = new ConnectionManager(
                        new ConfigHelper(propertiesFilePath));
            } catch (IOException e) {
                ROOT_LOGGER.couldNotLoadPropertiesFile(e);
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        ROOT_LOGGER.couldNotClosePropertiesFile(e);
                    }
                }
            }
        }
    }

    private void sendJdr(String fileName) {
        // TODO: replace with JdrReport uuid property
        String uuid = "6fdc7b27-2c64-4d73-b04c-aa3ddafeb05e"; // need to replace this with wildfly UUID
        String description = JDR_DESCRIPTION.replace("{uuid}", uuid);
        boolean wasSuccessful = true;
        File file = new File(fileName);
        String fullUrl = connectionManager.getConfig().getUrl()
                + telemetryEndpoint + uuid;
        String systemUrl = "";
        try {
            systemUrl = new URL(new URL(connectionManager.getConfig().getUrl()), systemEndpoint).toString();
            Response response = get(connectionManager.getConnection(),
                    new URL(new URL(systemUrl), uuid).toString());
            com.redhat.gss.redhat_support_lib.infrastructure.System system = response.readEntity(com.redhat.gss.redhat_support_lib.infrastructure.System.class);
            // if system is unregistered then attempt to register system
            if(system.getUnregistered_at() != null) {
                try {
                    addSystem(connectionManager.getConnection(), systemUrl, uuid);
                } catch (RequestException e) {
                    ROOT_LOGGER.couldNotRegisterSystem(e);
                }
                catch (MalformedURLException e) {
                    ROOT_LOGGER.couldNotRegisterSystem(e);
                }
            }
        } catch (RequestException e) {
            // if the system was not found then attempt to register the system
            if(e.getMessage().contains("" + Response.Status.NOT_FOUND.getStatusCode())) {
                try {
                    addSystem(connectionManager.getConnection(), systemUrl, uuid);
                } catch (RequestException exception) {
                    ROOT_LOGGER.couldNotRegisterSystem(exception);
                }
                catch (MalformedURLException exception) {
                    ROOT_LOGGER.couldNotRegisterSystem(exception);
                }
            }
        } catch (MalformedURLException e) {
            ROOT_LOGGER.couldNotFindSystem(e);
        }
        try {
            upload(connectionManager.getConnection(), new URL(new URL(new URL(connectionManager.getConfig().getUrl()), telemetryEndpoint), uuid).toString(), file,
                    description);
        } catch (FileNotFoundException e) {
            wasSuccessful = false;
            ROOT_LOGGER.couldNotFindJdr(e);
        } catch (MalformedURLException e) {
            wasSuccessful = false;
            ROOT_LOGGER.couldNotUploadJdr(e);
        } catch (ParseException e) {
            wasSuccessful = false;
            ROOT_LOGGER.couldNotUploadJdr(e);
        } catch (RequestException e) {
            wasSuccessful = false;
            ROOT_LOGGER.couldNotUploadJdr(e);
        }
        if(wasSuccessful) {
            ROOT_LOGGER.jdrSent();
        }
    }
}