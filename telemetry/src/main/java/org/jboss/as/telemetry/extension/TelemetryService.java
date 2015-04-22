package org.jboss.as.telemetry.extension;

import org.jboss.as.jdr.JdrReport;
import org.jboss.as.jdr.JdrReportCollector;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * @author <a href="mailto:jkinlaw@redhat.com">Josh Kinlaw</a>
 */
public class TelemetryService implements Service<TelemetryService> {

    public static final long MILLISECOND_TO_DAY = 86400000;

    private static volatile TelemetryService instance;

    Logger log = Logger.getLogger(TelemetryService.class);

    private boolean enabled = true;

    // Frequency thread should run in days
    private long frequency = 1;

    private JdrReportCollector jdrCollector;

    private Thread output = initializeOutput();

    private TelemetryService(long tick, boolean enabled) {
        this.frequency = tick;
        this.enabled = enabled;
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
}