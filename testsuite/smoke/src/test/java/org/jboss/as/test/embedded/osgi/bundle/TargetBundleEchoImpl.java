package org.jboss.as.test.embedded.osgi.bundle;

import org.jboss.as.test.embedded.osgi.api.Echo;
import org.jboss.logging.Logger;

public class TargetBundleEchoImpl implements Echo {

    static final Logger log = Logger.getLogger(TargetBundleEchoImpl.class);

    @Override
    public String echo(String message) {
        log.infof("Echo: %s", message);
        return message;
    }
}