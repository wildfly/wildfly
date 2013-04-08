package org.jboss.as.domain.http.server.security;

import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.SecurityContext;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.Methods;

/**
 * A wrapper around the {@link AuthenticationMechanism}s to ensure that the identity manager is aware of the current mechanism.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class AuthenticationMechanismWrapper implements AuthenticationMechanism {

    private final AuthenticationMechanism wrapped;
    private final org.jboss.as.domain.management.AuthMechanism mechanism;

    public AuthenticationMechanismWrapper(final AuthenticationMechanism wrapped) {
        this.wrapped = wrapped;
        String mechanismName = wrapped.getName();
        if ("BASIC".equals(mechanismName)) {
            mechanism = org.jboss.as.domain.management.AuthMechanism.PLAIN;
        } else if ("DIGEST".equals(mechanismName)) {
            mechanism = org.jboss.as.domain.management.AuthMechanism.DIGEST;
        } else if ("CLIENT-CERT".equals(mechanismName)) {
            mechanism = org.jboss.as.domain.management.AuthMechanism.CLIENT_CERT;
        } else {
            mechanism = null;
        }
    }

    @Override
    public String getName() {
        return wrapped.getName();
    }

    @Override
    public AuthenticationMechanismOutcome authenticate(HttpServerExchange exchange, SecurityContext securityContext) {
        try {
            RealmIdentityManager.setAuthenticationMechanism(mechanism);
            if (isPreflightedOptions(exchange)) {
                return AuthenticationMechanismOutcome.AUTHENTICATED;
            }
            return wrapped.authenticate(exchange, securityContext);
        } finally {
            RealmIdentityManager.setAuthenticationMechanism(null);
        }
    }

    @Override
    public ChallengeResult sendChallenge(HttpServerExchange exchange, SecurityContext securityContext) {
        return wrapped.sendChallenge(exchange, securityContext);
    }

    private boolean isPreflightedOptions(HttpServerExchange exchange) {
        return exchange.getRequestMethod().equals(Methods.OPTIONS) && exchange.getRequestHeaders().contains(Headers.ORIGIN);
    }

}
