package org.jboss.as.domain.http.server.security;

import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.SecurityContext;
import io.undertow.server.HttpServerExchange;

/**
 * A wrapper around the {@link AuthenticationMechanism}s to ensure that the identity manager is aware of the current mechanism.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class AuthenticationMechanismWrapper implements AuthenticationMechanism {

    private final AuthenticationMechanism wrapped;
    private final org.jboss.as.domain.management.AuthMechanism mechanism;

    public AuthenticationMechanismWrapper(final AuthenticationMechanism wrapped, org.jboss.as.domain.management.AuthMechanism mechanism) {
        this.wrapped = wrapped;
        this.mechanism = mechanism;
    }

    @Override
    public AuthenticationMechanismOutcome authenticate(HttpServerExchange exchange, SecurityContext securityContext) {
        try {
            RealmIdentityManager.setAuthenticationMechanism(mechanism);
            return wrapped.authenticate(exchange, securityContext);
        } finally {
            RealmIdentityManager.setAuthenticationMechanism(null);
        }
    }

    @Override
    public ChallengeResult sendChallenge(HttpServerExchange exchange, SecurityContext securityContext) {
        return wrapped.sendChallenge(exchange, securityContext);
    }

}
