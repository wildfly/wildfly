package org.jboss.as.embedded;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutionException;

import javax.naming.Context;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.logging.Logger;

/**
 * Due to classloader problems embedded container can't operate directly on StandaloneServer implementation instance. We have to
 * delegate methods executions via reflections.
 *
 * @author <a href="mailto:mmatloka@gmail.com">Michal Matloka</a>
 */
class StandaloneServerAdapter implements StandaloneServer {

    private static final Logger LOGGER = Logger.getLogger(StandaloneServerAdapter.class);

    private final Object server;
    private final Method startMethod;
    private final Method stopMethod;

    /**
     * @param server instance of StandaloneServer BUT loaded by different classloader
     * @param startMethod servers start method
     * @param stopMethod servers stop method
     */
    public StandaloneServerAdapter(Object server, Method startMethod, Method stopMethod) {
        this.server = server;
        this.startMethod = startMethod;
        this.stopMethod = stopMethod;
    }

    @Override
    @Deprecated
    public void deploy(File file) throws IOException, ExecutionException, InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Context getContext() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ModelControllerClient getModelControllerClient() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void start() throws ServerStartException {
        try {
            startMethod.invoke(server, (Object[]) null);
        } catch (Exception e) {
            handleReflectionInvokation(e);
        }
    }

    @Override
    public void stop() {
        try {
            stopMethod.invoke(server, (Object[]) null);
        } catch (Exception e) {
            handleReflectionInvokation(e);
        }
    }

    @Override
    @Deprecated
    public void undeploy(File file) throws ExecutionException, InterruptedException {
        throw new UnsupportedOperationException();
    }

    private void handleReflectionInvokation(Exception e) {
        LOGGER.error("Error while executing method via reflection", e);
    }

}
