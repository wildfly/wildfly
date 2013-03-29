package org.jboss.as.undertow;

import java.io.IOException;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.xnio.OptionMap;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class WorkerService implements Service<XnioWorker> {
    private volatile XnioWorker worker;
    private final OptionMap options;

    protected WorkerService(OptionMap options) {
        this.options = options;
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        final Xnio xnio = Xnio.getInstance();

        try {
            worker = xnio.createWorker(options);
        } catch (IOException e) {
            throw new StartException("Could not create worker!",e);
        }
    }

    @Override
    public void stop(StopContext stopContext) {
        worker.shutdown();
    }

    @Override
    public XnioWorker getValue() throws IllegalStateException, IllegalArgumentException {
        return worker;
    }
}
