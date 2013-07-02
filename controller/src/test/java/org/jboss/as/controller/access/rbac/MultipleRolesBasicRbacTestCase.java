package org.jboss.as.controller.access.rbac;

import org.jboss.dmr.ModelNode;
import org.junit.internal.AssumptionViolatedException;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;

/**
 * @author Ladislav Thon <lthon@redhat.com>
 */
public class MultipleRolesBasicRbacTestCase extends BasicRbacTestCase {
    @Override
    protected ModelNode executeWithRole(ModelNode operation, StandardRole role) {
        StandardRole additionalRole = role != StandardRole.AUDITOR ? StandardRole.AUDITOR : StandardRole.DEPLOYER;
        operation.get(OPERATION_HEADERS, "roles").add(role.name()).add(additionalRole.name());
        return getController().execute(operation, null, null, null);
    }

    // ---
    // this entire section is to be deleted once multiple roles are properly implemented

    private void ignore() {
        throw new AssumptionViolatedException("ManagementPermissionCollection.implies doesn't work in case of users with multiple roles");
    }

    @Override
    public void testMonitorNoAccess() {
        ignore();
    }

    @Override
    public void testMonitorReadConfigPermitted() {
        ignore();
    }

    @Override
    public void testMonitorWriteConfigDenied() {
        ignore();
    }

    @Override
    public void testMonitorReadRuntimePermitted() {
        ignore();
    }

    @Override
    public void testMonitorWriteRuntimeDenied() {
        ignore();
    }

    @Override
    public void testOperatorNoAccess() {
        ignore();
    }

    @Override
    public void testOperatorReadConfigPermitted() {
        ignore();
    }

    @Override
    public void testOperatorWriteConfigDenied() {
        ignore();
    }

    @Override
    public void testOperatorReadRuntimePermitted() {
        ignore();
    }

    @Override
    public void testOperatorWriteRuntimePermitted() {
        ignore();
    }

    @Override
    public void testMaintainerNoAccess() {
        ignore();
    }

    @Override
    public void testMaintainerReadConfigPermitted() {
        ignore();
    }

    @Override
    public void testMaintainerWriteConfigPermitted() {
        ignore();
    }

    @Override
    public void testMaintainerReadRuntimePermitted() {
        ignore();
    }

    @Override
    public void testMaintainerWriteRuntimePermitted() {
        ignore();
    }

    @Override
    public void testDeployerNoAccess() {
        ignore();
    }

    @Override
    public void testDeployerReadConfigPermitted() {
        ignore();
    }

    @Override
    public void testDeployerWriteConfigPermitted() {
        ignore();
    }

    @Override
    public void testDeployerReadRuntimePermitted() {
        ignore();
    }

    @Override
    public void testDeployerWriteRuntimePermitted() {
        ignore();
    }

    @Override
    public void testAdministratorReadConfigPermitted() {
        ignore();
    }

    @Override
    public void testAdministratorWriteConfigPermitted() {
        ignore();
    }

    @Override
    public void testAdministratorReadRuntimePermitted() {
        ignore();
    }

    @Override
    public void testAdministratorWriteRuntimePermitted() {
        ignore();
    }
}
