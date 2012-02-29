package org.jboss.as.test.integration.web.security;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.security.Constants;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.security.Constants.AUTHENTICATION;
import static org.jboss.as.security.Constants.CLASSIC;
import static org.jboss.as.security.Constants.CODE;
import static org.jboss.as.security.Constants.FLAG;
import static org.jboss.as.security.Constants.MAPPING;
import static org.jboss.as.security.Constants.MAPPING_MODULES;
import static org.jboss.as.security.Constants.MODULE_OPTIONS;
import static org.jboss.as.security.Constants.TYPE;

/**
 * @author Stuart Douglas
 */
public class WebSimpleRoleMappingSecurityDomainSetup extends AbstractSecurityDomainSetup {

    private static final Logger log = Logger.getLogger(WebSimpleRoleMappingSecurityDomainSetup.class);

    protected static final String WEB_SECURITY_DOMAIN = "web-tests";

    @Override
    public void setup(final ManagementClient managementClient, final String containerId) {
        log.debug("start of the domain creation");


        final List<ModelNode> updates = new ArrayList<ModelNode>();

        ModelNode op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).add(SUBSYSTEM, "security");
        op.get(OP_ADDR).add(Constants.SECURITY_DOMAIN, getSecurityDomainName());
        updates.add(op);

        op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).add(SUBSYSTEM, "security");
        op.get(OP_ADDR).add(Constants.SECURITY_DOMAIN, getSecurityDomainName());
        op.get(OP_ADDR).add(AUTHENTICATION, Constants.CLASSIC);

        ModelNode loginModule = op.get(Constants.LOGIN_MODULES).add();
        loginModule.get(CODE).set("UsersRoles");
        loginModule.get(FLAG).set("required");
        updates.add(op);

        op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).add(SUBSYSTEM, "security");
        op.get(OP_ADDR).add(Constants.SECURITY_DOMAIN, getSecurityDomainName());
        op.get(OP_ADDR).add(MAPPING, CLASSIC);

        ModelNode mappingModule = op.get(MAPPING_MODULES).add();
        mappingModule.get(CODE).set("SimpleRoles"); // see:  https://docs.jboss.org/author/display/AS71/Security+subsystem+configuration
        mappingModule.get(TYPE).set("role");
        ModelNode mappingOptions = mappingModule.get(MODULE_OPTIONS);
        mappingOptions.get("peter").set("superuser,gooduser");
        op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        updates.add(op);

        applyUpdates(managementClient.getControllerClient(), updates);
        log.debug("end of the domain creation");
    }


    @Override
    protected String getSecurityDomainName() {
        return WEB_SECURITY_DOMAIN;
    }

}
