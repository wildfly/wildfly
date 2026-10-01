package org.jboss.as.test.integration.ejb.access.log;

import org.jboss.logging.Logger;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Local;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

@Stateless
@Remote(SLSBRemote.class)
@Local(SLSBLocal.class)
@LocalBean
public class SLSB implements SLSBLocal {
    protected static final Logger logger = Logger.getLogger(SLSB.class);

    @Override
    @PermitAll
    public String echo(String msg) {
        logger.info("[TOMMY] SLSB.echo");
        return String.format("ECHO[%s]", msg);
    }

    @Override
    @RolesAllowed("Role1")
    public String echoSecuredRole1(String msg) {
        logger.info("[TOMMY] SLSB.echoSecuredRole1 Role1");
        return String.format("ECHO_SECURED_1[%s]", msg);
    }

    @Override
    @RolesAllowed("Role2")
    public String echoSecuredRole2(String msg) {
        logger.info("[TOMMY] SLSB.echoSecuredRole2 Role2");
        return String.format("ECHO_SECURED_2[%s]", msg);
    }
}
