package org.jboss.as.telemetry.extension;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Properties;

import org.apache.commons.net.ftp.FTPClient;
import org.jboss.as.jdr.JdrReport;
import org.jboss.as.jdr.JdrReportCollector;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import com.redhat.gss.redhat_support_lib.errors.FTPException;
import com.redhat.gss.redhat_support_lib.errors.RequestException;
import com.redhat.gss.redhat_support_lib.helpers.ConfigHelper;
import com.redhat.gss.redhat_support_lib.infrastructure.BaseQuery;
import com.redhat.gss.redhat_support_lib.web.ConnectionManager;

/**
 * @author <a href="mailto:jkinlaw@redhat.com">Josh Kinlaw</a>
 */
public class TelemetryService extends BaseQuery implements Service<TelemetryService> {

    public static final long MILLISECOND_TO_DAY = 86400000;

    public static final String JBOSS_PROPERTY_DIR = "jboss.server.data.dir";

    public static final String TELEMETRY_PROPERTY_FILE_NAME = "telemetry.properties";

    public static final String BASE_URI = "baseUri";

    public static final String TELEMETRY_PATH = "telemetryPath";

    public static final String JDR_DESCRIPTION = "JDR for UUID {uuid}";

    private static volatile TelemetryService instance;

    private ConnectionManager connectionManager = null;

    private String telemetryUri;

    Logger log = Logger.getLogger(TelemetryService.class);

    private boolean enabled = true;

    // Frequency thread should run in days
    private long frequency = 1;

    private JdrReportCollector jdrCollector;

    private Thread output = initializeOutput();

    private TelemetryService(long tick, boolean enabled) {
        this.frequency = tick;
        this.enabled = enabled;
        // TODO: move properties to properties file
        String username = "";
        String password = "";
        String url = "";
        String proxyUser = null;
        String proxyPassword = null;
        URL proxyUrl = null;
        int proxyPort = 0;
        String userAgent = null;
        this.connectionManager = new ConnectionManager(new ConfigHelper(
                username, password, url, proxyUser, proxyPassword, proxyUrl, proxyPort, userAgent, false));
    }

    public static TelemetryService getInstance(long tick, boolean enabled) {
        if(instance == null) {
            synchronized (TelemetryService.class) {
                if(instance == null) {
                    instance = new TelemetryService(tick, enabled);
                }
            }
        }
        else {
            instance.frequency = tick;
            instance.enabled = enabled;
        }
        return instance;
    }

    private Thread initializeOutput() {
        return new Thread() {
            @Override
            public void run() {
                synchronized(output) {
                    while(enabled) {
                        try{
                            JdrReport report = jdrCollector.collect();
                            System.out.println("report location: " + report.getLocation());
                            String response = sendJdr(report.getLocation());
                            System.out.println("telemetry response: " + response);
                            output.wait(frequency * MILLISECOND_TO_DAY);
                        } catch (InterruptedException e) {
                            return;
                        } catch (Exception e) {
                            // TODO: log.error
                            // TODO: implement catch blocks for JDR exceptions that can be handled
                            e.printStackTrace();
                            try {
                                output.wait(frequency * MILLISECOND_TO_DAY);
                            }
                            catch (InterruptedException e2) {
                                e2.printStackTrace();
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
        if(output != null && output.isAlive()) {
            output.interrupt();
        }
    }

    void setFrequency(long frequency) {
        log.info("Frequency of Telemetry Subsystem updated to " + frequency);
        this.frequency = frequency;
        synchronized(output) {
            output.notify();
        }
    }

    public long getFrequency() {
        return this.frequency;
    }

    public static ServiceName createServiceName() {
        return ServiceName.JBOSS.append(org.jboss.as.telemetry.extension.TelemetryExtension.SUBSYSTEM_NAME);
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if(enabled && (output == null || !output.isAlive())) {
            log.info("Telemetry Subsystem Enabled");
            output = initializeOutput();
            try {
                this.start(null);
            } catch (StartException e) {
                e.printStackTrace();
            }
        }
        else if(!enabled) {
            log.info("Telemetry Subsystem Disabled");
        }
        synchronized(output) {
            output.notify();
        }
    }

    public void setJdrReportCollector(JdrReportCollector jdrCollector) {
        this.jdrCollector = jdrCollector;
    }

    public void setTelemetryUri() {
        String jbossConfig = System.getProperty(JBOSS_PROPERTY_DIR);
        String telemetryPropFilePath = jbossConfig + File.separator + TelemetryExtension.SUBSYSTEM_NAME +
                    File.separator + TELEMETRY_PROPERTY_FILE_NAME;
        Properties telemetryProperties = new Properties();
        InputStream telemetryIs = getClass().getClassLoader().getResourceAsStream(telemetryPropFilePath);
        if(telemetryIs != null) {
            try {
                telemetryProperties.load(telemetryIs);
                telemetryUri = telemetryProperties.getProperty(BASE_URI) +
                        telemetryProperties.getProperty(TELEMETRY_PATH);
            }
            catch(IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String sendJdr(String fileName) {
        // TODO: replace with property
        String url = "/rs/telemetry/api/uploads";
        String uuid = "[uuid placeholder]";
        String description = JDR_DESCRIPTION.replace("{uuid}", uuid);
        File file = new File(fileName);
//        List<String> queryParams = new ArrayList<String>();
//        queryParams.add("public=" + Boolean.toString(publicVis));
        String uri = null;
        // TODO: move 2GB to constant
        if (file.length() > 200000L) {
            FTPClient ftp = null;
            FileInputStream fis = null;
            try {
                ftp = connectionManager.getFTP();
                ftp.cwd(connectionManager.getConfig().getFtpDir());
                ftp.enterLocalPassiveMode();
                fis = new FileInputStream(file);
                if (!ftp.storeFile(file.getName(), fis)) {
                    throw new FTPException("Error during FTP store file.");
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if(fis != null) {
                        fis.close();
                    }
                    if(ftp != null) {
                        ftp.logout();
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        } else {
            String fullUrl = connectionManager.getConfig().getUrl() + url;
            try {
                uri = upload(connectionManager.getConnection(), fullUrl, file,
                        description).getStringHeaders().getFirst("location");
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (RequestException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return uri;
    }
}