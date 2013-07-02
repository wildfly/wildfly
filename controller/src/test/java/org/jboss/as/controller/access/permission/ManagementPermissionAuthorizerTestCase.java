package org.jboss.as.controller.access.permission;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.access.*;
import org.jboss.as.controller.access.constraint.ScopingConstraint;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.junit.Before;
import org.junit.Test;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.EnumSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * @author Ladislav Thon <lthon@redhat.com>
 */
public class ManagementPermissionAuthorizerTestCase {
    private Caller caller;
    private Environment environment;
    private ManagementPermissionAuthorizer authorizer;

    @Before
    public void setUp() {
        caller = new Caller();
        ControlledProcessState processState = new ControlledProcessState(false);
        processState.setRunning();
        environment = new Environment(processState, ProcessType.EMBEDDED_SERVER);
        authorizer = new ManagementPermissionAuthorizer(new TestPermissionFactory());
    }

    @Test
    public void testAuthorizerResourcePermit() {
        Action action = new Action(null, null, EnumSet.of(Action.ActionEffect.ACCESS,
                Action.ActionEffect.READ_CONFIG));
        TargetResource targetResource = TargetResource.forStandalone(null, null);
        AuthorizationResult result = authorizer.authorize(caller, environment, action, targetResource);

        assertEquals(AuthorizationResult.Decision.PERMIT, result.getDecision());
    }

    @Test
    public void testAuthorizerResourceDeny() {
        Action action = new Action(null, null, EnumSet.of(Action.ActionEffect.ACCESS,
                Action.ActionEffect.READ_CONFIG, Action.ActionEffect.WRITE_CONFIG));
        TargetResource targetResource = TargetResource.forStandalone(null, null);
        AuthorizationResult result = authorizer.authorize(caller, environment, action, targetResource);

        assertEquals(AuthorizationResult.Decision.DENY, result.getDecision());
    }

    @Test
    public void testAuthorizerAttributePermit() {
        Action action = new Action(null, null, EnumSet.of(Action.ActionEffect.ACCESS,
                Action.ActionEffect.READ_CONFIG));
        TargetResource targetResource = TargetResource.forStandalone(null, null);
        TargetAttribute targetAttribute = new TargetAttribute(null, new ModelNode(), targetResource);
        AuthorizationResult result = authorizer.authorize(caller, environment, action, targetAttribute);

        assertEquals(AuthorizationResult.Decision.PERMIT, result.getDecision());
    }

    @Test
    public void testAuthorizerAttributeDeny() {
        Action action = new Action(null, null, EnumSet.of(Action.ActionEffect.ACCESS,
                Action.ActionEffect.READ_CONFIG, Action.ActionEffect.WRITE_CONFIG));
        TargetResource targetResource = TargetResource.forStandalone(null, null);
        TargetAttribute targetAttribute = new TargetAttribute(null, new ModelNode(), targetResource);
        AuthorizationResult result = authorizer.authorize(caller, environment, action, targetAttribute);

        assertEquals(AuthorizationResult.Decision.DENY, result.getDecision());
    }

    // ---

    private static final class TestPermissionFactory implements PermissionFactory {
        private PermissionCollection getUserPermissions() {
            ManagementPermissionCollection mpc = new ManagementPermissionCollection(TestManagementPermission.class);
            mpc.add(new TestManagementPermission(Action.ActionEffect.ACCESS));
            mpc.add(new TestManagementPermission(Action.ActionEffect.READ_CONFIG));
            mpc.add(new TestManagementPermission(Action.ActionEffect.READ_RUNTIME));
            return mpc;
        }

        private PermissionCollection getRequiredPermissions(Action action) {
            ManagementPermissionCollection mpc = new ManagementPermissionCollection(TestManagementPermission.class);
            for (Action.ActionEffect actionEffect : action.getActionEffects()) {
                mpc.add(new TestManagementPermission(actionEffect));
            }
            return mpc;
        }

        @Override
        public PermissionCollection getUserPermissions(Caller caller, Environment callEnvironment, Action action, TargetAttribute target) {
            return getUserPermissions();
        }

        @Override
        public PermissionCollection getUserPermissions(Caller caller, Environment callEnvironment, Action action, TargetResource target) {
            return getUserPermissions();
        }

        @Override
        public PermissionCollection getRequiredPermissions(Action action, TargetAttribute target) {
            return getRequiredPermissions(action);
        }

        @Override
        public PermissionCollection getRequiredPermissions(Action action, TargetResource target) {
            return getRequiredPermissions(action);
        }
    }

    private static final class TestManagementPermission extends ManagementPermission {
        private TestManagementPermission(Action.ActionEffect actionEffect) {
            super("test", actionEffect);
        }

        @Override
        public ManagementPermission createScopedPermission(ScopingConstraint constraint) {
            return null;
        }

        @Override
        public boolean implies(Permission permission) {
            return equals(permission);
        }
    }
}
