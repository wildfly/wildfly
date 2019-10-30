package org.jboss.as.test.integration.web.security.external;

import io.undertow.security.idm.ExternalCredential;
import org.jboss.security.SimpleGroup;
import org.jboss.security.SimplePrincipal;
import org.jboss.security.auth.callback.ObjectCallback;
import org.jboss.security.auth.spi.AbstractServerLoginModule;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.login.LoginException;
import java.security.Principal;
import java.security.acl.Group;

/**
 * @author Stuart Douglas
 */
public class ExternalLoginModule extends AbstractServerLoginModule {

    private Principal identity;

    // Public methods --------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Override
    public boolean login() throws LoginException {
        if (super.login()) {
            log.debug("super.login()==true");
            return true;
        }

        // Time to see if this is a delegation request.
        NameCallback ncb = new NameCallback("Username:");
        ObjectCallback ocb = new ObjectCallback("Credential:");

        try {
            callbackHandler.handle(new Callback[] { ncb, ocb });
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            return false; // If the CallbackHandler can not handle the required callbacks then no chance.
        }

        String name = ncb.getName();
        Object credential = ocb.getCredential();

        if (credential instanceof ExternalCredential) {
            identity = new SimplePrincipal(name);
            loginOk = true;
            return true;
        }

        return false; // Attempted login but not successful.
    }

    // Protected methods -----------------------------------------------------

    @Override
    protected Principal getIdentity() {
        return identity;
    }

    @Override
    protected Group[] getRoleSets() throws LoginException {
        Group roles = new SimpleGroup("Roles");
        Group[] groups = { roles };
        //group mapping would go here
        if(getIdentity().getName().equals("anil")) {
            roles.addMember(new SimplePrincipal("gooduser"));
        }
        roles.addMember(getIdentity());
        return groups;
    }

}
