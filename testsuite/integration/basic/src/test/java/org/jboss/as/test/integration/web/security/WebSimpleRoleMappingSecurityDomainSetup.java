package org.jboss.as.test.integration.web.security;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.security.Constants.CLASSIC;
import static org.jboss.as.security.Constants.CODE;
import static org.jboss.as.security.Constants.FLAG;
import static org.jboss.as.security.Constants.LOGIN_MODULE;
import static org.jboss.as.security.Constants.MAPPING;
import static org.jboss.as.security.Constants.MAPPING_MODULE;
import static org.jboss.as.security.Constants.MODULE_OPTIONS;
import static org.jboss.as.security.Constants.SECURITY_DOMAIN;
import static org.jboss.as.security.Constants.TYPE;

import java.util.Arrays;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.security.Constants;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 * @author Stuart Douglas
 */
public class WebSimpleRoleMappingSecurityDomainSetup extends AbstractSecurityDomainSetup {

    private static final Logger log = Logger.getLogger(WebSimpleRoleMappingSecurityDomainSetup.class);

    protected static final String WEB_SECURITY_DOMAIN = "web-tests";

    @Override
    public void setup(final ManagementClient managementClient, final String containerId) {
        log.debug("start of the domain creation");
        final ModelNode compositeOp = new ModelNode();
        compositeOp.get(OP).set(COMPOSITE);
        compositeOp.get(OP_ADDR).setEmptyList();

        ModelNode steps = compositeOp.get(STEPS);

        PathAddress addressSD = PathAddress.pathAddress()
                .append(SUBSYSTEM, "security")
                .append(SECURITY_DOMAIN, getSecurityDomainName());

        steps.add(Util.createAddOperation(addressSD));
        PathAddress address = addressSD.append(Constants.AUTHENTICATION, Constants.CLASSIC);
        steps.add(Util.createAddOperation(address));

        ModelNode loginModule = Util.createAddOperation(address.append(LOGIN_MODULE, "UsersRoles"));
        loginModule.get(CODE).set("UsersRoles");
        loginModule.get(FLAG).set("required");
        steps.add(loginModule);

        address = addressSD.append(MAPPING, CLASSIC);
        steps.add(Util.createAddOperation(address));

        ModelNode mappingModule = Util.createAddOperation(address.append(MAPPING_MODULE, "SimpleRoles"));

        mappingModule.get(CODE).set("SimpleRoles"); // see:  https://docs.jboss.org/author/display/AS71/Security+subsystem+configuration
        mappingModule.get(TYPE).set("role");
        ModelNode mappingOptions = mappingModule.get(MODULE_OPTIONS);
        mappingOptions.get("peter").set("superuser,gooduser");
        mappingModule.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        steps.add(mappingModule);

        applyUpdates(managementClient.getControllerClient(), Arrays.asList(compositeOp));
        log.debug("end of the domain creation");
    }


    @Override
    protected String getSecurityDomainName() {
        return WEB_SECURITY_DOMAIN;
    }

}
