package org.jboss.as.test.integration.sar.context.classloader.mbean;

import org.jboss.logging.Logger;

/**
 * A MBean class which resides in a JBoss Module. This MBean tests that the TCCL corresponds to the deployment classloader of the deployment through which this MBean was deployed
 *
 * @author: Jaikiran Pai
 */
public class MBeanInAModuleService implements MBeanInAModuleServiceMBean {

    private static final Logger logger = Logger.getLogger(MBeanInAModuleService.class);

    static {
        logger.trace("Static block of " + MBeanInAModuleService.class.getName() + " being loaded");
        // test TCCL in static block
        testClassLoadByTCCL("org.jboss.as.test.integration.sar.context.classloader.ClassAInSarDeployment");
    }

    public MBeanInAModuleService() {
        logger.trace("Constructing " + this);
        // test TCCL in constructor
        testClassLoadByTCCL("org.jboss.as.test.integration.sar.context.classloader.ClassBInSarDeployment");
    }

    @Override
    public int add(int a, int b) {
        return a + b;
    }

    public void start() {
        logger.trace("Starting " + this);
        // test TCCL in lifecycle method
        testClassLoadByTCCL("org.jboss.as.test.integration.sar.context.classloader.ClassCInSarDeployment");
    }

    public void stop() {
        logger.trace("Stopping " + this);
        // test TCCL in lifecycle method
        testClassLoadByTCCL("org.jboss.as.test.integration.sar.context.classloader.ClassDInSarDeployment");
    }

    private static void testClassLoadByTCCL(final String className) {
        final ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        logger.trace("Trying to load class " + className + " from TCCL " + tccl);
        try {
            tccl.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        logger.trace("Successfully loaded class " + className + " from TCCL " + tccl);
    }
}
