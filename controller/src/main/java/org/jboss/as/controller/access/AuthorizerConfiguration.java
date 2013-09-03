package org.jboss.as.controller.access;

import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.access.constraint.ScopingConstraint;

/**
 * Encapsulates the current configuration of all aspects of the access control system that are
 * available to {@link Authorizer} implementations.
 * <p>The results of changes to the access control configuration made via the WildFly management
 * layer will be made available here.</p>
 */
public interface AuthorizerConfiguration {

    /**
     * Gets whether the authorizer uses a role-based authorization mechanism.
     *
     * @return {@code true} if a role-based mechanism is used; {@code false} if not
     */
    boolean isRoleBased();

    /**
     * Gets the names of the "standard" "built-in" roles used by the authorizer. A built-in role requires no
     * end user configuration.
     *
     * @return the standard role names. Will not be {@code null}, but may be an empty set if roles are not used
     *         or no built-in roles are used.
     */
    Set<String> getStandardRoles();

    /**
     * Gets the configured scoped roles, keyed by the name of the role.
     *
     * @return the scoped roles. Will not be {@code null}
     */
    Map<String, ScopedRole> getScopedRoles();

    /**
     * Gets the names of the all roles used by the authorizer, including both built-in roles and roles added via
     * end user configuration.
     *
     * @return the role names. Will not be {@code null}, but may be an empty set if roles are not used
     *         or no built-in roles are used and no end user configured roles exist.
     */
    Set<String> getAllRoles();

    /**
     * Gets whether the current {@link #getAllRoles() set of roles} contains the given role, with the
     * check performed using a case-insensitive algorithm.
     * @param roleName the name of the role
     * @return {@code true} if the current role set includes an item that
     *         {@link String#equalsIgnoreCase(String) equals ignoring case} the given {@code roleName}
     */
    boolean hasRole(String roleName);

    /**
     * Gets the configured role mappings, keyed by the name of the role.
     *
     * @return the role mappings. Will not be {@code null}
     */
    Map<String, RoleMapping> getRoleMappings();

    /**
     * Gets whether role mapping should use roles obtained from the security realm and associated
     * with the {@link Caller}.
     *
     * @return {@code true} if role
     */
    boolean isMapUsingRealmRoles();

    /**
     * Gets whether JMX calls to non-facade mbeans (i.e. those that result in invocations to
     * {@link Authorizer#authorizeJmxOperation(Caller, Environment, JmxAction)}) should be treated as 'sensitive'.
     *
     * @return {@code true} if non-facade mbean calls are sensitive; {@code false} otherwise
     */
    boolean isNonFacadeMBeansSensitive();

    /**
     * Register a listener for changes in the configured scoped roles.
     * @param listener the listener. Cannot be {@code null}
     */
    void registerScopedRoleListener(ScopedRoleListener listener);

    /**
     * Unregister a listener for changes in the configured scoped roles.
     * @param listener the listener. Cannot be {@code null}
     */
    void unregisterScopedRoleListener(ScopedRoleListener listener);

    /**
     * Types of {@link org.jboss.as.controller.access.AuthorizerConfiguration.MappingPrincipal}s used in {@link Caller} to {@link RoleMapping} mapping.
     */
    enum PrincipalType {
        GROUP, USER
    }

    /**
     * Encapsulates the notion of a role to which a caller can be mapped.
     */
    interface RoleMapping {

        /**
         * The name of the role.
         *
         * @return the name. Will not be {@code null}
         */
        String getName();

        /**
         * Gets whether the caller matches the role mapping's inclusion rules.
         *
         * @param caller the caller
         * @return the principal that results in the caller satisfying the role mapping's inclusion rules,
         *         or {@code null} if the caller does not satisfy them
         */
        MappingPrincipal isIncluded(Caller caller);

        /**
         * Gets whether the caller matches the role mapping's exclusion rules.
         *
         * @param caller the caller
         * @return the principal that results in the caller satisfying the role mapping's exclusion rules,
         *         or {@code null} if the caller does not satisfy them
         */
        MappingPrincipal isExcluded(Caller caller);
    }

    /**
     * Encapsulates the notion of a principal used in {@link Caller} to {@link RoleMapping} mapping.
     */
    interface MappingPrincipal {

        /**
         * Gets the type of the principal.
         *
         * @return the principal type. Will not be {@code null}
         */
        PrincipalType getType();

        /**
         * The name of the security realm for which the principal is valid
         * @return the realm name, or {@code null} if the principal is not specific to any realm
         */
        String getRealm();

        /**
         * Gets the name of the principal
         *
         * @return the principal name. Will not be {@code null}
         */
        String getName();
    }

    /**
     * Encapsulates configuration information for a scoped role.
     */
    final class ScopedRole {
        private final String name;
        private final String baseRoleName;
        private final ScopingConstraint scopingConstraint;

        public ScopedRole(String name, String baseRoleName, ScopingConstraint scopingConstraint) {
            this.name = name;
            this.baseRoleName = baseRoleName;
            this.scopingConstraint = scopingConstraint;
        }

        /**
         * Gets the name of the scoped role.
         * @return the name of the role. Will not be {@code null}
         */
        public String getName() {
            return name;
        }

        /**
         * Gets the name of the role on which the scoped role is based.
         * @return the name of the base role. Will not be {@code null}
         */
        public String getBaseRoleName() {
            return baseRoleName;
        }

        /**
         * Gets the {@link ScopingConstraint} used to apply constraints to the permissions of the base role
         * in order to derive the permissions of the scoped role.
         * @return the constraint. Will not be {@code null}
         */
        public ScopingConstraint getScopingConstraint() {
            return scopingConstraint;
        }
    }

    /**
     * Listener for changes to the configured scoped roles.
     */
    interface ScopedRoleListener {
        /**
         * Notification that a scoped role is being added. The notification will be received
         * before the role becomes visible in the roles collections exposed by the {@link AuthorizerConfiguration}.
         * @param added the scoped role
         */
        void scopedRoleAdded(ScopedRole added);

        /**
         * Notification that a scoped role has been removed. The notification will be received
         * after the role is no longer visible in the roles collections exposed by the {@link AuthorizerConfiguration}.
         * @param removed the scoped role
         */
        void scopedRoleRemoved(ScopedRole removed);
    }
}
