package org.jboss.as.test.integration.ejb.security.authorization;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;

/**
 * Created by IntelliJ IDEA.
 * User: jlanik
 * Date: 12/9/11
 * Time: 10:33 AM
 * To change this template use File | Settings | File Templates.
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
}
