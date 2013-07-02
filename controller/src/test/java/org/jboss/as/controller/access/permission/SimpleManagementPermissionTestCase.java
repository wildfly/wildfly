package org.jboss.as.controller.access.permission;

import org.jboss.as.controller.access.Action;
import org.jboss.as.controller.access.constraint.Constraint;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Ladislav Thon <lthon@redhat.com>
 */
public class SimpleManagementPermissionTestCase {
    private static final SimpleManagementPermission ACCESS_ALLOWED_REQUIRED = new SimpleManagementPermission(Action.ActionEffect.ACCESS, new TestConstraint(true, Constraint.ControlFlag.REQUIRED));
    private static final SimpleManagementPermission ACCESS_DISALLOWED_REQUIRED = new SimpleManagementPermission(Action.ActionEffect.ACCESS, new TestConstraint(false, Constraint.ControlFlag.REQUIRED));
    private static final SimpleManagementPermission READ_CONFIG_ALLOWED_REQUIRED = new SimpleManagementPermission(Action.ActionEffect.READ_CONFIG, new TestConstraint(true, Constraint.ControlFlag.REQUIRED));
    private static final SimpleManagementPermission READ_RUNTIME_ALLOWED_REQUIRED = new SimpleManagementPermission(Action.ActionEffect.READ_RUNTIME, new TestConstraint(true, Constraint.ControlFlag.REQUIRED));
    private static final SimpleManagementPermission READ_RUNTIME_DISALLOWED_REQUIRED = new SimpleManagementPermission(Action.ActionEffect.READ_RUNTIME, new TestConstraint(false, Constraint.ControlFlag.REQUIRED));

    private static final SimpleManagementPermission ACCESS_ALLOWED_REQUIRED_ALLOWED_OPTIONAL = new SimpleManagementPermission(Action.ActionEffect.ACCESS, new TestConstraint(true, Constraint.ControlFlag.REQUIRED), new TestConstraint(true, Constraint.ControlFlag.OPTIONAL));
    private static final SimpleManagementPermission ACCESS_ALLOWED_REQUIRED_DISALLOWED_OPTIONAL = new SimpleManagementPermission(Action.ActionEffect.ACCESS, new TestConstraint(true, Constraint.ControlFlag.REQUIRED), new TestConstraint(false, Constraint.ControlFlag.OPTIONAL));
    private static final SimpleManagementPermission ACCESS_DISALLOWED_REQUIRED_ALLOWED_OPTIONAL = new SimpleManagementPermission(Action.ActionEffect.ACCESS, new TestConstraint(false, Constraint.ControlFlag.REQUIRED), new TestConstraint(true, Constraint.ControlFlag.OPTIONAL));
    private static final SimpleManagementPermission ACCESS_DISALLOWED_REQUIRED_DISALLOWED_OPTIONAL = new SimpleManagementPermission(Action.ActionEffect.ACCESS, new TestConstraint(false, Constraint.ControlFlag.REQUIRED), new TestConstraint(false, Constraint.ControlFlag.OPTIONAL));

    private static final SimpleManagementPermission ACCESS_ALLOWED_SUFFICIENT_ALLOWED_OPTIONAL = new SimpleManagementPermission(Action.ActionEffect.ACCESS, new TestConstraint(true, Constraint.ControlFlag.SUFFICIENT), new TestConstraint(true, Constraint.ControlFlag.OPTIONAL));
    private static final SimpleManagementPermission ACCESS_ALLOWED_SUFFICIENT_DISALLOWED_OPTIONAL = new SimpleManagementPermission(Action.ActionEffect.ACCESS, new TestConstraint(true, Constraint.ControlFlag.SUFFICIENT), new TestConstraint(false, Constraint.ControlFlag.OPTIONAL));
    private static final SimpleManagementPermission ACCESS_DISALLOWED_SUFFICIENT_ALLOWED_OPTIONAL = new SimpleManagementPermission(Action.ActionEffect.ACCESS, new TestConstraint(false, Constraint.ControlFlag.SUFFICIENT), new TestConstraint(true, Constraint.ControlFlag.OPTIONAL));
    private static final SimpleManagementPermission ACCESS_DISALLOWED_SUFFICIENT_DISALLOWED_OPTIONAL = new SimpleManagementPermission(Action.ActionEffect.ACCESS, new TestConstraint(false, Constraint.ControlFlag.SUFFICIENT), new TestConstraint(false, Constraint.ControlFlag.OPTIONAL));

    private static final SimpleManagementPermission ACCESS_ALLOWED_REQUIRED_ALLOWED_SUFFICIENT = new SimpleManagementPermission(Action.ActionEffect.ACCESS, new TestConstraint(true, Constraint.ControlFlag.REQUIRED), new TestConstraint(true, Constraint.ControlFlag.SUFFICIENT));
    private static final SimpleManagementPermission ACCESS_ALLOWED_REQUIRED_DISALLOWED_SUFFICIENT = new SimpleManagementPermission(Action.ActionEffect.ACCESS, new TestConstraint(true, Constraint.ControlFlag.REQUIRED), new TestConstraint(false, Constraint.ControlFlag.SUFFICIENT));
    private static final SimpleManagementPermission ACCESS_DISALLOWED_REQUIRED_ALLOWED_SUFFICIENT = new SimpleManagementPermission(Action.ActionEffect.ACCESS, new TestConstraint(false, Constraint.ControlFlag.REQUIRED), new TestConstraint(true, Constraint.ControlFlag.SUFFICIENT));
    private static final SimpleManagementPermission ACCESS_DISALLOWED_REQUIRED_DISALLOWED_SUFFICIENT = new SimpleManagementPermission(Action.ActionEffect.ACCESS, new TestConstraint(false, Constraint.ControlFlag.REQUIRED), new TestConstraint(false, Constraint.ControlFlag.SUFFICIENT));

    private static final SimpleManagementPermission ACCESS_ALLOWED_SUFFICIENT_ALLOWED_REQUIRED = new SimpleManagementPermission(Action.ActionEffect.ACCESS, new TestConstraint(true, Constraint.ControlFlag.SUFFICIENT), new TestConstraint(true, Constraint.ControlFlag.REQUIRED));
    private static final SimpleManagementPermission ACCESS_ALLOWED_SUFFICIENT_DISALLOWED_REQUIRED = new SimpleManagementPermission(Action.ActionEffect.ACCESS, new TestConstraint(true, Constraint.ControlFlag.SUFFICIENT), new TestConstraint(false, Constraint.ControlFlag.REQUIRED));
    private static final SimpleManagementPermission ACCESS_DISALLOWED_SUFFICIENT_ALLOWED_REQUIRED = new SimpleManagementPermission(Action.ActionEffect.ACCESS, new TestConstraint(false, Constraint.ControlFlag.SUFFICIENT), new TestConstraint(true, Constraint.ControlFlag.REQUIRED));
    private static final SimpleManagementPermission ACCESS_DISALLOWED_SUFFICIENT_DISALLOWED_REQUIRED = new SimpleManagementPermission(Action.ActionEffect.ACCESS, new TestConstraint(false, Constraint.ControlFlag.SUFFICIENT), new TestConstraint(false, Constraint.ControlFlag.REQUIRED));

    @Test
    public void testSameActionEffectAndSameConstraint() {
        assertTrue(ACCESS_ALLOWED_REQUIRED.implies(ACCESS_ALLOWED_REQUIRED));
    }

    @Test
    public void testSameActionEffectAndDifferentConstraint() {
        assertFalse(ACCESS_ALLOWED_REQUIRED.implies(ACCESS_DISALLOWED_REQUIRED));
    }

    @Test
    public void testDifferentActionEffectButSameConstraint() {
        assertFalse(READ_CONFIG_ALLOWED_REQUIRED.implies(READ_RUNTIME_ALLOWED_REQUIRED));
    }

    @Test
    public void testDifferentActionEffectAndDifferentConstraint() {
        assertFalse(READ_CONFIG_ALLOWED_REQUIRED.implies(READ_RUNTIME_ALLOWED_REQUIRED));
        assertFalse(READ_CONFIG_ALLOWED_REQUIRED.implies(READ_RUNTIME_DISALLOWED_REQUIRED));
    }

    @Test
    public void testSameActionEffectAndTwoSameConstraints() {
        assertTrue(ACCESS_ALLOWED_REQUIRED_ALLOWED_OPTIONAL.implies(ACCESS_ALLOWED_REQUIRED_ALLOWED_OPTIONAL));

        assertTrue(ACCESS_ALLOWED_SUFFICIENT_ALLOWED_OPTIONAL.implies(ACCESS_ALLOWED_SUFFICIENT_ALLOWED_OPTIONAL));

        assertTrue(ACCESS_ALLOWED_REQUIRED_ALLOWED_SUFFICIENT.implies(ACCESS_ALLOWED_REQUIRED_ALLOWED_SUFFICIENT));

        assertTrue(ACCESS_ALLOWED_SUFFICIENT_ALLOWED_REQUIRED.implies(ACCESS_ALLOWED_SUFFICIENT_ALLOWED_REQUIRED));
    }

    @Test
    public void testSameActionEffectAndTwoDifferentConstraints() {
        assertFalse(ACCESS_ALLOWED_REQUIRED_ALLOWED_OPTIONAL.implies(ACCESS_ALLOWED_REQUIRED_DISALLOWED_OPTIONAL));
        assertFalse(ACCESS_ALLOWED_REQUIRED_ALLOWED_OPTIONAL.implies(ACCESS_DISALLOWED_REQUIRED_ALLOWED_OPTIONAL));
        assertFalse(ACCESS_ALLOWED_REQUIRED_ALLOWED_OPTIONAL.implies(ACCESS_DISALLOWED_REQUIRED_DISALLOWED_OPTIONAL));

        assertTrue(ACCESS_ALLOWED_SUFFICIENT_ALLOWED_OPTIONAL.implies(ACCESS_ALLOWED_SUFFICIENT_DISALLOWED_OPTIONAL));
        assertFalse(ACCESS_ALLOWED_SUFFICIENT_ALLOWED_OPTIONAL.implies(ACCESS_DISALLOWED_SUFFICIENT_ALLOWED_OPTIONAL));
        assertFalse(ACCESS_ALLOWED_SUFFICIENT_ALLOWED_OPTIONAL.implies(ACCESS_DISALLOWED_SUFFICIENT_DISALLOWED_OPTIONAL));

        // TODO how is this supposed to work?
        //assertTrue(ACCESS_ALLOWED_REQUIRED_ALLOWED_SUFFICIENT.implies(ACCESS_ALLOWED_REQUIRED_DISALLOWED_SUFFICIENT));
        assertFalse(ACCESS_ALLOWED_REQUIRED_ALLOWED_SUFFICIENT.implies(ACCESS_DISALLOWED_REQUIRED_ALLOWED_SUFFICIENT));
        assertFalse(ACCESS_ALLOWED_REQUIRED_ALLOWED_SUFFICIENT.implies(ACCESS_DISALLOWED_REQUIRED_DISALLOWED_SUFFICIENT));

        assertTrue(ACCESS_ALLOWED_SUFFICIENT_ALLOWED_REQUIRED.implies(ACCESS_ALLOWED_SUFFICIENT_DISALLOWED_REQUIRED));
        assertFalse(ACCESS_ALLOWED_SUFFICIENT_ALLOWED_REQUIRED.implies(ACCESS_DISALLOWED_SUFFICIENT_ALLOWED_REQUIRED));
        assertFalse(ACCESS_ALLOWED_SUFFICIENT_ALLOWED_REQUIRED.implies(ACCESS_DISALLOWED_SUFFICIENT_DISALLOWED_REQUIRED));
    }

    // ---

    private static final class TestConstraint implements Constraint {
        private final boolean allowed;
        private final ControlFlag controlFlag;

        private TestConstraint(boolean allowed, ControlFlag controlFlag) {
            this.allowed = allowed;
            this.controlFlag = controlFlag;
        }

        @Override
        public boolean violates(Constraint other) {
            if (other instanceof TestConstraint) {
                return this.allowed != ((TestConstraint) other).allowed;
            }
            return false;
        }

        @Override
        public ControlFlag getControlFlag() {
            return controlFlag;
        }

        @Override
        public int compareTo(Constraint o) {
            return 0;
        }
    }
}
