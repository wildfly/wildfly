package org.jboss.as.test.integration.ejb.security.authorization;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;

/**
 * This is a parent class with methods which will be inherrited in test session bean to check that the annotations are inherrited.
 *
 * @author <a href="mailto:jlanik@redhat.com">Jan Lanik</a>.
 */
public abstract class ParentAnnOnlyCheck implements SimpleAuthorizationRemote {

   public String defaultAccess(String message) {
      return message;
   }

   @RolesAllowed("Role1")
   public String roleBasedAccessOne(String message) {
      return message;
   }

   @RolesAllowed({"Role2", "Negative", "No-role"})
   public String roleBasedAccessMore(String message) {
      return message;
   }

   @PermitAll
   public String permitAll(String message) {
      return message;
   }

   @DenyAll
   public String denyAll(String message) {
      return message;
   }

   @RolesAllowed("**")
   public String starRoleAllowed(String message) {
      return message;
   }


}
