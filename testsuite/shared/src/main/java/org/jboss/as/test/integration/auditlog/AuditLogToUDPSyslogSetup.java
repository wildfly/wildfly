package org.jboss.as.test.integration.auditlog;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROTOCOL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UDP;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.logging.syslogserver.UDPSyslogServerConfig;
import org.jboss.dmr.ModelNode;
import org.productivity.java.syslog4j.SyslogConstants;
import org.productivity.java.syslog4j.server.SyslogServerConfigIF;

/**
 * {@link org.jboss.as.arquillian.api.ServerSetupTask} implementation which configures syslog server and auditlog-to-syslog
 * handler for this test.
 *
 * @author Josef Cacek
 */
public class AuditLogToUDPSyslogSetup extends AuditLogToSyslogSetup {

    @Override
    protected ModelNode addAuditlogSyslogProtocol(PathAddress syslogHandlerAddress) {
        return Util.createAddOperation(syslogHandlerAddress.append(PROTOCOL, UDP));
    }

    @Override
    protected String getSyslogProtocol() {
        return SyslogConstants.UDP;
    }

    @Override
    protected SyslogServerConfigIF getSyslogConfig() {
        return new UDPSyslogServerConfig();
    }
}