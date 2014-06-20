package org.wildfly.core.testrunner;

import org.jboss.as.controller.client.ModelControllerClient;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import javax.inject.Inject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * A lightweight test runner for running management based tests
 *
 * @author Stuart Douglas
 */
public class WildflyTestRunner extends BlockJUnit4ClassRunner {

    private static volatile boolean started = false;
    private static volatile Server server;

    /**
     * Creates a BlockJUnit4ClassRunner to run {@code klass}
     *
     * @throws org.junit.runners.model.InitializationError
     *          if the test class is malformed.
     */
    public WildflyTestRunner(Class<?> klass) throws InitializationError {
        super(klass);
        startServerIfRequired();
        doInject(klass, null);

    }

    private void doInject(Class<?> klass, Object instance) {
        Class c = klass;
        try {
            while (c != null && c != Object.class) {
                for (Field field : c.getDeclaredFields()) {
                    if ((instance == null && Modifier.isStatic(field.getModifiers()) ||
                            instance != null && !Modifier.isStatic(field.getModifiers()))) {
                        if (field.isAnnotationPresent(Inject.class)) {
                            field.setAccessible(true);
                            if (field.getType() == ManagementClient.class) {
                                field.set(instance, server.getClient());
                            } else if (field.getType() == ModelControllerClient.class) {
                                field.set(instance, server.getClient().getControllerClient());
                            }
                        }
                    }
                }
                c = c.getSuperclass();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject", e);
        }
    }

    @Override
    protected Object createTest() throws Exception {
        Object res =  super.createTest();
        doInject(getTestClass().getJavaClass(), res);
        return res;
    }

    @Override
    public void run(final RunNotifier notifier) {
        notifier.addListener(new RunListener() {

            @Override
            public void testRunFinished(Result result) throws Exception {
                super.testRunFinished(result);
                if (server != null) {
                    server.stop();
                    server = null;
                }
            }
        });
        startServerIfRequired();
        super.run(notifier);
    }

    private void startServerIfRequired() {
        if (!started) {
            started = true;
            server = new Server();
            server.start();
        }
    }
}
