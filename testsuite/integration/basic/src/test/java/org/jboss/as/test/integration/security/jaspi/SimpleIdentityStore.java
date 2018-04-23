package org.jboss.as.test.integration.security.jaspi;

import java.util.Collections;

import javax.enterprise.context.RequestScoped;
import javax.security.enterprise.credential.Credential;
import javax.security.enterprise.credential.UsernamePasswordCredential;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.IdentityStore;

@RequestScoped
public class SimpleIdentityStore implements IdentityStore {

    @Override
    public CredentialValidationResult validate(Credential credential) {
        if (credential instanceof UsernamePasswordCredential) {
            if (((UsernamePasswordCredential) credential).getCaller().equals("User")
                    && ((UsernamePasswordCredential) credential).getPassword().compareTo("User")) {
                return new CredentialValidationResult("User", Collections.singleton("User"));
            }
        }
        return CredentialValidationResult.INVALID_RESULT;
    }
}
