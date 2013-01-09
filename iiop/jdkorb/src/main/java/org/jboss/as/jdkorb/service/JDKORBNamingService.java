package org.jboss.as.jdkorb.service;

import com.sun.corba.se.impl.orbutil.ORBConstants;
import org.jboss.as.iiop.service.CorbaNamingService;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import com.sun.corba.se.spi.orb.ORB;

/**
 * @author Stuart Douglas
 */
public class JDKORBNamingService extends CorbaNamingService {

    @Override
    public void start(StartContext context) throws StartException {
        super.start(context);
        try {
            ((ORB)orbInjector.getValue()).register_initial_reference(ORBConstants.PERSISTENT_NAME_SERVICE_NAME, this.namingService);
        } catch (Exception e) {
            throw new StartException(e);
        }
    }
}
