package org.jboss.as.test.integration.security.jaspi;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import jakarta.security.enterprise.AuthenticationException;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import jakarta.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import jakarta.security.enterprise.credential.Password;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStoreHandler;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RequestScoped
public class SimpleHttpAuthenticationMechanism implements HttpAuthenticationMechanism {

    @Inject
    private IdentityStoreHandler identityStoreHandler;

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request, HttpServletResponse response, HttpMessageContext httpMessageContext) throws AuthenticationException {
        String username = request.getParameter("name");
        String pw = request.getParameter("pw");
        if (username != null && pw != null) {

            Password password = new Password(pw);

            CredentialValidationResult result = identityStoreHandler.validate(new UsernamePasswordCredential(username, password));

            if (result.getStatus() == CredentialValidationResult.Status.VALID) {
                return httpMessageContext.notifyContainerAboutLogin(result.getCallerPrincipal(), result.getCallerGroups());
            }

            return httpMessageContext.responseUnauthorized();
        }

        if (httpMessageContext.isProtected()) {
            return httpMessageContext.responseUnauthorized();
        }

        return httpMessageContext.doNothing();
    }
}
