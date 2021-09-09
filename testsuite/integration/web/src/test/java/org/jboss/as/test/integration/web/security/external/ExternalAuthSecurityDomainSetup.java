package org.jboss.as.test.integration.web.security.external;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.logging.Logger;
import org.wildfly.test.security.common.elytron.PropertyFileBasedDomain;
import org.wildfly.test.security.common.elytron.UndertowDomainMapper;

/**
 * @author Stuart Douglas
 */
public class ExternalAuthSecurityDomainSetup extends AbstractSecurityDomainSetup {

    private static final Logger log = Logger.getLogger(ExternalAuthSecurityDomainSetup.class);

    protected static final String WEB_SECURITY_DOMAIN = "web-tests";
    private CLIWrapper cli;

    private PropertyFileBasedDomain ps;
    private UndertowDomainMapper domainMapper;

    @Override
    public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
        cli = new CLIWrapper(true);
        setupElytronBasedSecurityDomain();
    }

    @Override
    protected String getSecurityDomainName() {
        return WEB_SECURITY_DOMAIN;
    }

    @Override
    public void tearDown(ManagementClient managementClient, String s) {
        try {
            domainMapper.remove(cli);
            ps.remove(cli);
            cli.close();
            ServerReload.executeReloadAndWaitForCompletion(managementClient);
        } catch (Exception e) {
            log.error("Failed to tear down domain.", e);
        }
    }

    private void setupElytronBasedSecurityDomain() throws Exception {
        ps = PropertyFileBasedDomain.builder()
                .withUser(Credentials.BAD_USER_NAME, Credentials.NOT_USED_PASSWORD, Credentials.BAD_USER_ROLE)
                .withUser(Credentials.GOOD_USER_NAME, Credentials.NOT_USED_PASSWORD, Credentials.CORRECT_ROLE)
                .withUser(Credentials.AUTHORIZED_WITHOUT_AUTHENTICATION_USER_NAME, Credentials.NOT_USED_PASSWORD, Credentials.CORRECT_ROLE)
                .withName(WEB_SECURITY_DOMAIN).build();
        ps.create(cli);
        domainMapper = UndertowDomainMapper.builder().withName(WEB_SECURITY_DOMAIN).build();
        domainMapper.create(cli);
    }

}