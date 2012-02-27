package org.jboss.as.arquillian.container;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.event.container.BeforeDeploy;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.context.ClassContext;
import org.jboss.arquillian.test.spi.event.suite.AfterClass;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;

/**
 * @author Stuart Douglas
 */
public class ServerSetupObserver {


    @Inject
    private Instance<ManagementClient> managementClient;

    @Inject
    private Instance<ClassContext> classContextInstance;

    private Set<ContainerClassHolder> alreadyRun = new HashSet<ContainerClassHolder>();

    private final List<ManagementClient> active = new ArrayList<ManagementClient>();
    private final List<ServerSetupTask> current = new ArrayList<ServerSetupTask>();

    public synchronized void handleBeforeDeployment(@Observes BeforeDeploy event, Container container) throws Exception {

        final ClassContext classContext = classContextInstance.get();
        if (classContext == null) {
            return;
        }

        final Class<?> currentClass = classContext.getActiveId();
        final ContainerClassHolder holder = new ContainerClassHolder(container.getName(), currentClass);
        if (alreadyRun.contains(holder)) {
            return;
        }
        alreadyRun.add(holder);
        ServerSetup setup = currentClass.getAnnotation(ServerSetup.class);
        if (setup == null) {
            return;
        }
        final Class<? extends ServerSetupTask>[] classes = setup.value();
        if (current.isEmpty()) {
            for(Class<? extends ServerSetupTask> clazz : classes) {
                Constructor<? extends ServerSetupTask> ctor = clazz.getDeclaredConstructor();
                ctor.setAccessible(true);
                current.add(ctor.newInstance());
            }
        } else {
            //this should never happen
            for(int i = 0; i < current.size(); ++ i) {
                if(classes[i] != current.get(i).getClass()) {
                    throw new RuntimeException("Mismatched ServerSetupTask current is " + current + " but " + currentClass + " is expecting " + Arrays.asList(classes));
                }
            }
        }

        final ManagementClient client = managementClient.get();
        for(ServerSetupTask instance : current) {
            instance.setup(client);
        }
        active.add(client);
    }

    public synchronized void handleAfterClass(@Observes AfterClass event) throws Exception {
        if (current != null) {
            for(final ManagementClient client : active) {
                for(final ServerSetupTask instance : current) {
                    instance.tearDown(client);
                }
            }
            active.clear();
            current.clear();
        }
    }


    private static final class ContainerClassHolder {
        private final Class<?> testClass;
        private final String name;

        private ContainerClassHolder(final String name, final Class<?> testClass) {
            this.name = name;
            this.testClass = testClass;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final ContainerClassHolder that = (ContainerClassHolder) o;

            if (name != null ? !name.equals(that.name) : that.name != null) return false;
            if (testClass != null ? !testClass.equals(that.testClass) : that.testClass != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = testClass != null ? testClass.hashCode() : 0;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            return result;
        }
    }


}
