package org.jboss.as.test.integration.web.security.external;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.security.Constants;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup;
import org.jboss.as.test.integration.web.security.WebSecurityCommon;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.wildfly.test.security.common.elytron.PropertyFileBasedDomain;
import org.wildfly.test.security.common.elytron.UndertowDomainMapper;

import java.util.Arrays;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.security.Constants.CODE;
import static org.jboss.as.security.Constants.FLAG;
import static org.jboss.as.security.Constants.LOGIN_MODULE;
import static org.jboss.as.security.Constants.MODULE;
import static org.jboss.as.security.Constants.SECURITY_DOMAIN;

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
        if (WebSecurityCommon.isElytron()) {
            cli = new CLIWrapper(true);
            setupElytronBasedSecurityDomain();
        } else {
            log.debug("start of the domain creation");

            final ModelNode compositeOp = new ModelNode();
            compositeOp.get(OP).set(COMPOSITE);
            compositeOp.get(OP_ADDR).setEmptyList();

            ModelNode steps = compositeOp.get(STEPS);
            PathAddress address = PathAddress.pathAddress()
                    .append(SUBSYSTEM, "security")
                    .append(SECURITY_DOMAIN, getSecurityDomainName());

            steps.add(Util.createAddOperation(address));
            address = address.append(Constants.AUTHENTICATION, Constants.CLASSIC);
            steps.add(Util.createAddOperation(address));
            ModelNode loginModule = Util.createAddOperation(address.append(LOGIN_MODULE, "External"));

            loginModule.get(CODE).set("org.jboss.as.test.integration.web.security.external.ExternalLoginModule");
            loginModule.get(MODULE).set("deployment.web-secure-external.war");
            loginModule.get(FLAG).set("required");
            loginModule.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            steps.add(loginModule);

            applyUpdates(managementClient.getControllerClient(), Arrays.asList(compositeOp));
            log.debug("end of the domain creation");
        }
    }

    @Override
    protected String getSecurityDomainName() {
        return WEB_SECURITY_DOMAIN;
    }

    @Override
    public void tearDown(ManagementClient managementClient, String s) {
        if (WebSecurityCommon.isElytron()) {
            try {
                domainMapper.remove(cli);
                ps.remove(cli);
                cli.close();
                ServerReload.executeReloadAndWaitForCompletion(managementClient);
            } catch (Exception e) {
                log.error("Failed to tear down domain.", e);
            }
        }
        else {
            super.tearDown(managementClient, s);
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