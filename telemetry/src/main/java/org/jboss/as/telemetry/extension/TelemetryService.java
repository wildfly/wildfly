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


//import org.apache.commons.net.ftp.FTPClient;
import org.jboss.as.jdr.JdrReport;
import org.jboss.as.jdr.JdrReportCollector;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;


//import com.redhat.gss.redhat_support_lib.errors.FTPException;
import com.redhat.gss.redhat_support_lib.errors.RequestException;
import com.redhat.gss.redhat_support_lib.helpers.ConfigHelper;
import com.redhat.gss.redhat_support_lib.infrastructure.BaseQuery;
import com.redhat.gss.redhat_support_lib.web.ConnectionManager;

/**
 * @author <a href="mailto:jkinlaw@redhat.com">Josh Kinlaw</a>
 */
public class TelemetryService extends BaseQuery implements Service<TelemetryService> {

    //public static final long MILLISECOND_TO_DAY = 86400000;
    public static final long MILLISECOND_TO_DAY = 1;

    public static final String JBOSS_PROPERTY_DIR = "jboss.server.data.dir";

    public static final String TELEMETRY_PROPERTY_FILE_NAME = "telemetry.properties";

    public static final String TELEMETRY_DESCRIPTION = "Properties file consisting of RHN login information and telemetry/insights URL";

    public static final String DEFAULT_BASE_URL = "https://api.access.qa.redhat.com";
    public static final String DEFAULT_ENDPOINT = "/api/telemetry/uploads/";

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
    public static final String TELEMETRY_ENDPOINT = "endpoint";

    public static final String JDR_DESCRIPTION = "JDR for UUID {uuid}";

    private static volatile TelemetryService instance;

    /**
     * Endpoint of url which when added to the end of the url reveals the
     * location of where to send the JDR
     * Should take the format of /telemetry/endpoint/
     */
    private String telemetryEndpoint;

    private ConnectionManager connectionManager = null;

    Logger log = Logger.getLogger(TelemetryService.class);

    private boolean enabled = true;

    // Frequency thread should run in days
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

    /**
     * Set RHN login credentials and write to telemetry config file
     *
     * @param rhnUid
     * @param rhnPw
     */
    public void setRhnLoginCredentials(String rhnUid, String rhnPw) {
        setRhnUid(rhnUid);
        setRhnPw(rhnPw);
        String jbossConfig = System.getProperty(JBOSS_PROPERTY_DIR);
        String propertiesFilePath = jbossConfig + File.separator + TelemetryExtension.SUBSYSTEM_NAME + File.separator + TELEMETRY_PROPERTY_FILE_NAME;
        Properties properties = new Properties();
        FileOutputStream fileOut = null;
        File file = new File(propertiesFilePath);
        try {
            if(!file.exists()) {
                file.getParentFile().mkdirs();
                properties.setProperty(URL, DEFAULT_BASE_URL);
                properties.setProperty(TELEMETRY_ENDPOINT, DEFAULT_ENDPOINT);
            }
            else {
                properties.load(new FileInputStream(propertiesFilePath));
            }
            properties.setProperty(USERNAME, rhnUid);
            properties.setProperty(PASSWORD, rhnPw);
            fileOut = new FileOutputStream(file);
            properties.store(fileOut, TELEMETRY_DESCRIPTION);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if(fileOut != null) {
                try {
                    fileOut.close();
                    setConnectionManager();
                }
                catch(IOException e) {
                    e.printStackTrace();
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
        String propertiesFilePath = jbossConfig + File.separator + TelemetryExtension.SUBSYSTEM_NAME + File.separator + TELEMETRY_PROPERTY_FILE_NAME;
        Properties properties = new Properties();
        FileInputStream fis = null;
        File file = new File(propertiesFilePath);
        if(file.exists()) {
            try {
                fis = new FileInputStream(propertiesFilePath);
                properties.load(fis);
                // setting required fields
                username = properties.getProperty(USERNAME);
                password = properties.getProperty(PASSWORD);
                url = properties.getProperty(URL);
                telemetryEndpoint = properties.getProperty(TELEMETRY_ENDPOINT);

                // setting optional fields
                proxyUser = properties.getProperty(PROXY_USER);
                proxyPassword = properties.getProperty(PROXY_PASSWORD);
                userAgent = properties.getProperty(USER_AGENT);
                if(properties.containsKey(PROXY_PORT)) {
                    proxyPort = Integer.parseInt(properties.getProperty(PROXY_PORT));
                }
                if(properties.containsKey(PROXY_URL)) {
                    proxyUrl = new URL(properties.getProperty(PROXY_URL));
                }
                this.connectionManager = new ConnectionManager(new ConfigHelper(
                        username, password, url, proxyUser, proxyPassword, proxyUrl, proxyPort, userAgent, false));
            } catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                if(fis != null) {
                    try {
                        fis.close();
                    }
                    catch(IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private String sendJdr(String fileName) {
        // TODO: replace with JdrReport uuid property
        String uuid = "uuid_placeholder";
        String description = JDR_DESCRIPTION.replace("{uuid}", uuid);
        File file = new File(fileName);
//        List<String> queryParams = new ArrayList<String>();
//        queryParams.add("public=" + Boolean.toString(publicVis));
        String uri = null;
        // TODO: move 2GB to constant
//        if (file.length() > 200000L) {
//            FTPClient ftp = null;
//            FileInputStream fis = null;
//            try {
//                ftp = connectionManager.getFTP();
//                ftp.cwd(connectionManager.getConfig().getFtpDir());
//                ftp.enterLocalPassiveMode();
//                fis = new FileInputStream(file);
//                if (!ftp.storeFile(file.getName(), fis)) {
//                    throw new FTPException("Error during FTP store file.");
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            } finally {
//                try {
//                    if(fis != null) {
//                        fis.close();
//                    }
//                    if(ftp != null) {
//                        ftp.logout();
//                    }
//                } catch (IOException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//            }
//        } else {
            String fullUrl = connectionManager.getConfig().getUrl() + telemetryEndpoint + uuid;
            System.out.println("connectionManager: " + connectionManager + "\nfullUrl: " + fullUrl + "\nfile : " + file.getPath() + "\ndescription: " + description);
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
//        }
        return uri;
    }
}