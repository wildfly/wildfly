package org.jboss.as.mail.extension;


import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import javax.mail.Session;
import java.util.Properties;

/**
 * @author Tomaz Cerar
 * @created 27.7.11 0:14
 */
public class MailSessionService implements Service<Session> {
    private static final Logger log = Logger.getLogger(MailSessionService.class);

    private MailSessionConfig sessionConfig;

    public MailSessionService(MailSessionConfig sessionConfig) {
        log.info("service construced with config: "+sessionConfig);
        this.sessionConfig = sessionConfig;
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        log.info("start...");

    }

    @Override
    public void stop(StopContext stopContext) {
        log.info("stop...");
    }

    @Override
    public Session getValue() throws IllegalStateException, IllegalArgumentException {
        log.info("should return value");
        return Session.getDefaultInstance(new Properties());

    }
}
