/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.controller.access.permission;

import static org.junit.Assert.assertEquals;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.Action;
import org.jboss.as.controller.access.AuthorizationResult;
import org.jboss.as.controller.access.Caller;
import org.jboss.as.controller.access.Environment;
import org.jboss.as.controller.access.TargetAttribute;
import org.jboss.as.controller.access.TargetResource;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Ladislav Thon <lthon@redhat.com>
 */
public class ManagementPermissionAuthorizerTestCase {

    private static final ManagementResourceRegistration ROOT_RR = ManagementResourceRegistration.Factory.create(new SimpleResourceDefinition(null, new NonResolvingResourceDescriptionResolver()) {
        @Override
        public List<AccessConstraintDefinition> getAccessConstraints() {
            return Collections.emptyList();
        }
    });

    private Caller caller;
    private Environment environment;
    private ManagementPermissionAuthorizer authorizer;

    @Before
    public void setUp() {
        caller = Caller.createCaller(null);
        ControlledProcessState processState = new ControlledProcessState(false);
        processState.setRunning();
        environment = new Environment(processState, ProcessType.EMBEDDED_SERVER);
        TestPermissionFactory testPermissionFactory = new TestPermissionFactory();
        authorizer = new ManagementPermissionAuthorizer(testPermissionFactory, testPermissionFactory);
    }

    @Test
    public void testAuthorizerResourcePermit() {
        Action action = new Action(null, null, EnumSet.of(Action.ActionEffect.ADDRESS,
                Action.ActionEffect.READ_CONFIG));
        TargetResource targetResource = TargetResource.forStandalone(PathAddress.EMPTY_ADDRESS, ROOT_RR, null);
        AuthorizationResult result = authorizer.authorize(caller, environment, action, targetResource);

        assertEquals(AuthorizationResult.Decision.PERMIT, result.getDecision());
    }

    @Test
    public void testAuthorizerResourceDeny() {
        Action action = new Action(null, null, EnumSet.of(Action.ActionEffect.ADDRESS,
                Action.ActionEffect.READ_CONFIG, Action.ActionEffect.WRITE_CONFIG));
        TargetResource targetResource = TargetResource.forStandalone(PathAddress.EMPTY_ADDRESS, ROOT_RR, null);
        AuthorizationResult result = authorizer.authorize(caller, environment, action, targetResource);

        assertEquals(AuthorizationResult.Decision.DENY, result.getDecision());
    }

    @Test
    public void testAuthorizerAttributePermit() {
        Action action = new Action(null, null, EnumSet.of(Action.ActionEffect.ADDRESS,
                Action.ActionEffect.READ_CONFIG));
        TargetResource targetResource = TargetResource.forStandalone(PathAddress.EMPTY_ADDRESS, ROOT_RR, null);
        TargetAttribute targetAttribute = new TargetAttribute("test", null, new ModelNode(), targetResource);
        AuthorizationResult result = authorizer.authorize(caller, environment, action, targetAttribute);

        assertEquals(AuthorizationResult.Decision.PERMIT, result.getDecision());
    }

    @Test
    public void testAuthorizerAttributeDeny() {
        Action action = new Action(null, null, EnumSet.of(Action.ActionEffect.ADDRESS,
                Action.ActionEffect.READ_CONFIG, Action.ActionEffect.WRITE_CONFIG));
        TargetResource targetResource = TargetResource.forStandalone(PathAddress.EMPTY_ADDRESS, ROOT_RR, null);
        TargetAttribute targetAttribute = new TargetAttribute("test", null, new ModelNode(), targetResource);
        AuthorizationResult result = authorizer.authorize(caller, environment, action, targetAttribute);

        assertEquals(AuthorizationResult.Decision.DENY, result.getDecision());
    }

    // ---

    private static final class TestPermissionFactory implements PermissionFactory, JmxPermissionFactory {
        private PermissionCollection getUserPermissions() {
            ManagementPermissionCollection mpc = new ManagementPermissionCollection("test", TestManagementPermission.class);
            mpc.add(new TestManagementPermission(Action.ActionEffect.ADDRESS));
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

        @Override
        public boolean isNonFacadeMBeansSensitive() {
            return false;
        }

        @Override
        public Set<String> getUserRoles(Caller caller, Environment callEnvironment, Action action, TargetResource target) {
            return null;
        }
    }

    private static final class TestManagementPermission extends ManagementPermission {
        private TestManagementPermission(Action.ActionEffect actionEffect) {
            super("test", actionEffect);
        }

        @Override
        public boolean implies(Permission permission) {
            return equals(permission);
        }
    }
}
