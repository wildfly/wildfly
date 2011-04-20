package org.jboss.as.testsuite.integration.osgi.bundle;

import org.jboss.as.testsuite.integration.osgi.api.Echo;
import org.jboss.logging.Logger;

public class TargetBundleEchoImpl implements Echo {

    static final Logger log = Logger.getLogger(TargetBundleEchoImpl.class);

    @Override
    public String echo(String message) {
        log.infof("Echo: %s", message);
        return message;
    }
}