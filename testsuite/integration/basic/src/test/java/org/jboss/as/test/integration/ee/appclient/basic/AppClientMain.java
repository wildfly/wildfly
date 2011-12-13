package org.jboss.as.test.integration.ee.appclient.basic;

import javax.ejb.EJB;

import org.jboss.logging.Logger;

/**
 * @author Stuart Douglas
 */
public class AppClientMain {
    private static final Logger logger = Logger.getLogger("org.jboss.as.test.appclient");

    @EJB
    private static AppClientSingletonRemote appClientSingletonRemote;

    public static void main(final String[] params) {
        logger.info("Main method invoked");

        try {
            appClientSingletonRemote.makeAppClientCall(params[0]);
            logger.info("Main method invocation completed with success");
        } catch (Exception e) {
            logger.error("Main method failed", e);
        }
    }

}
