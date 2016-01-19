package org.jboss.as.insights.extension;

import static org.jboss.as.insights.logger.InsightsLogger.ROOT_LOGGER;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.Response;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.insights.api.InsightsScheduler;
import org.jboss.as.jdr.JdrReport;
import org.jboss.as.jdr.JdrReportCollector;

import com.redhat.gss.redhat_support_lib.api.API;
import com.redhat.gss.redhat_support_lib.errors.RequestException;
import org.jboss.msc.value.InjectedValue;

class InsightsJdrScheduler implements InsightsScheduler {

    /**
     * Properties that can be set via the properties file.
     */
    private static final String JDR_DESCRIPTION = "JDR for UUID {uuid}";

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

    private volatile boolean enabled = false;
    private volatile int scheduleInterval = 1;

    private String rhnUid;
    private String rhnPw;
    private String proxyUrl;
    private int proxyPort;
    private String proxyUser;
    private String proxyPw;
    private String url;
    private String userAgent;

    private API api;

    private final InjectedValue<JdrReportCollector> jdrCollector;

    private ScheduledFuture<?> insightsTask;
    private final ScheduledExecutorService scheduledExecutor;

    private class InsightsScheduleRunnable implements Runnable {
        @Override
        public void run() {
            try {
                sendJdr();
            } catch (OperationFailedException e) {
                ROOT_LOGGER.threadInterrupted(e);
            }
        }
    }

    private final InsightsScheduleRunnable insightsRunnable = new InsightsScheduleRunnable();

    public InsightsJdrScheduler(ScheduledExecutorService scheduledExecutor, InsightsConfiguration config, InjectedValue<JdrReportCollector> jdrCollector) {
        this.scheduledExecutor = scheduledExecutor;
        this.scheduleInterval = config.getScheduleInterval();
        this.systemEndpoint = config.getSystemEndpoint();
        this.insightsEndpoint = config.getInsightsEndpoint();
        this.rhnUid = config.getRhnUid();
        this.rhnPw = config.getRhnPw();
        this.url = config.getUrl();
        this.proxyPw = config.getProxyPw();
        this.proxyUser = config.getProxyUser();
        this.proxyUrl = config.getProxyUrl();
        this.userAgent = config.getUserAgent();
        this.proxyPort = config.getProxyPort();
        this.jdrCollector = jdrCollector;
        // setupApi();
    }

    private void setupApi() {
        URL proxyUrlUrl;
        try {
            proxyUrlUrl = new URL(proxyUrl);
        } catch (MalformedURLException e) {
            proxyUrlUrl = null;
        }
        if (rhnUid == null) {
            ROOT_LOGGER.rhnUidIsNull();
        }
        if (rhnPw == null) {
            ROOT_LOGGER.rhnPwIsNull();
        }
        api = new API(rhnUid, rhnPw, url, proxyUser, proxyPw, proxyUrlUrl, proxyPort, userAgent);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setScheduleInterval(int scheduleInterval) {
        boolean enabled = this.enabled;
        if(enabled) {
            stopScheduler();
        }
        this.scheduleInterval = scheduleInterval;
        this.enabled = enabled;
        startScheduler();
    }

    @Override
    public void startScheduler() {
        final boolean enabled = this.enabled;
        if (!enabled) {
            return;
        }
        ROOT_LOGGER.insightsEnabled();
        setupApi();
        this.enabled = true;
        startSchedule();
    }

    @Override
    public void stopScheduler() {
        this.enabled = false;
        ROOT_LOGGER.insightsDisabled();
        cancelSchedule();
    }

    private void cancelSchedule() {
        if (insightsTask != null) {
            insightsTask.cancel(false);
            insightsTask = null;
        }
    }

    private synchronized void startSchedule() {
        if (scheduleInterval <= 0) {
            scheduleInterval = InsightsService.DEFAULT_SCHEDULE_INTERVAL;
        }
        insightsTask = scheduledExecutor.scheduleWithFixedDelay(insightsRunnable, 0, scheduleInterval, TimeUnit.DAYS);
    }

    protected void sendJdr() throws OperationFailedException {
        JdrReport report = jdrCollector.getValue().collect();
        String fileName = report.getLocation();
        String uuid = report.getJdrUuid();
        String description = JDR_DESCRIPTION.replace("{uuid}", uuid);
        boolean wasSuccessful = true;
        File file = new File(fileName);
        String systemUrl = "";
        try {
            systemUrl = systemEndpoint;
            api.getInsights();
            Response response = api.getInsights().get(systemUrl + uuid);
            com.redhat.gss.redhat_support_lib.infrastructure.System system = response.readEntity(
                    com.redhat.gss.redhat_support_lib.infrastructure.System.class);
            // if system is unregistered then attempt to register system
            if (system.getUnregistered_at() != null) {
                api.getInsights().addSystem(systemUrl, uuid,
                        InetAddress.getLocalHost().getHostName());
            }
        } catch (RequestException e) {
            // if the system was not found then attempt to register the system
            if (e.getMessage().contains("" + Response.Status.NOT_FOUND.getStatusCode())) {
                try {
                    api.getInsights().addSystem(systemUrl, uuid, InetAddress.getLocalHost().getHostName());
                } catch (RequestException | MalformedURLException | UnknownHostException exception) {
                    ROOT_LOGGER.couldNotRegisterSystem(exception);
                }
            } else {
                ROOT_LOGGER.couldNotRegisterSystem(e);
            }
        } catch (MalformedURLException | UnknownHostException e) {
            ROOT_LOGGER.couldNotFindSystem(e);
        }
        try {
            // upload JDR to insights
            api.getInsights().upload((insightsEndpoint + uuid), file, description);
        } catch (FileNotFoundException e) {
            wasSuccessful = false;
            ROOT_LOGGER.couldNotFindJdr(e);
        } catch (MalformedURLException | ParseException | RequestException e) {
            wasSuccessful = false;
            ROOT_LOGGER.couldNotUploadJdr(e);
        }
        if (wasSuccessful) {
            ROOT_LOGGER.jdrSent();
        }
    }

    @Override
    public void setRhnUid(String rhnUid) {
        this.rhnUid = rhnUid;
        setupApi();
    }

    @Override
    public void setRhnPw(String rhnPw) {
        this.rhnPw = rhnPw;
    }

    @Override
    public void setProxyUrl(String proxyUrl) {
        this.proxyUrl = proxyUrl;
        setupApi();
    }

    @Override
    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
        setupApi();
    }

    @Override
    public void setProxyUser(String proxyUser) {
        this.proxyUser = proxyUser;
    }

    @Override
    public void setProxyPw(String proxyPw) {
        this.proxyPw = proxyPw;
    }

    @Override
    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public void setInsightsEndpoint(String insightsEndpoint) {
        this.insightsEndpoint = insightsEndpoint;
    }

    @Override
    public void setSystemEndpoint(String systemEndpoint) {
        this.systemEndpoint = systemEndpoint;
    }

    @Override
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
        setupApi();
    }

    @Override
    public void enable(String rhnUid, String rhnPw) {
        enable(rhnUid, rhnPw, null, -1, null, null);
    }

    @Override
    public void enable(String rhnUid, String rhnPw, String proxyUrl, int proxyPort, String proxyUser, String proxyPwd) {
        this.rhnUid = rhnUid;
        this.rhnPw = rhnPw;
        this.proxyUrl = proxyUrl;
        this.proxyPort = proxyPort;
        this.proxyUser = proxyUser;
        this.proxyPw = proxyPwd;
        this.enabled = true;
    }
}
