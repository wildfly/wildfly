package org.jboss.as.controller.access.rbac;

import org.jboss.dmr.ModelNode;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;

/**
 * @author Ladislav Thon <lthon@redhat.com>
 */
public class DuplicateRolesBasicRbacTestCase extends BasicRbacTestCase {
    @Override
    protected ModelNode executeWithRole(ModelNode operation, StandardRole role) {
        operation.get(OPERATION_HEADERS, "roles").add(role.name()).add(role.name());
        return getController().execute(operation, null, null, null);
    }
}
