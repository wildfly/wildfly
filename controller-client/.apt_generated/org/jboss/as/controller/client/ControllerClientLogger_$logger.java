
package org.jboss.as.controller.client;

import java.io.Serializable;
import javax.annotation.Generated;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.DelegatingBasicLogger;
import org.jboss.logging.Logger;


/**
 * Warning this class consists of generated code.
 * 
 */
@Generated(value = "org.jboss.logging.processor.generator.model.MessageLoggerImplementor", date = "2013-10-24T15:58:26+0100")
public class ControllerClientLogger_$logger
    extends DelegatingBasicLogger
    implements Serializable, ControllerClientLogger, BasicLogger
{

    private final static long serialVersionUID = 1L;
    private final static java.lang.String FQCN = ControllerClientLogger_$logger.class.getName();
    private final static java.lang.String leakedControllerClient = "JBAS010600: Closing leaked controller client";
    private final static java.lang.String cannotDeleteTempFile = "JBAS010601: Cannot delete temp file %s, will be deleted on exit";

    public ControllerClientLogger_$logger(final Logger log) {
        super(log);
    }

    public final void leakedControllerClient(final Throwable allocationStackTrace) {
        super.log.logf(FQCN, (org.jboss.logging.Logger.Level.WARN), (allocationStackTrace), leakedControllerClient$str());
    }

    protected java.lang.String leakedControllerClient$str() {
        return leakedControllerClient;
    }

    public final void cannotDeleteTempFile(final java.lang.String name) {
        super.log.logf(FQCN, (org.jboss.logging.Logger.Level.WARN), null, cannotDeleteTempFile$str(), name);
    }

    protected java.lang.String cannotDeleteTempFile$str() {
        return cannotDeleteTempFile;
    }

}
