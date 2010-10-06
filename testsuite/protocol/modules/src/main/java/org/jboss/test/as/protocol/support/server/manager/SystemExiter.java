package org.jboss.test.as.protocol.support.server.manager;
/**
 * Used to override System.exit() calls. For our tests we don't
 * want System.exit to have any effect.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class SystemExiter {
    private static Exiter exiter;

    public static void initialize(Exiter exiter) {
        SystemExiter.exiter = exiter;
    }

    public static void exit(int status) {
        getExiter().exit(status);
    }

    private static Exiter getExiter() {
        return exiter == null ? new DefaultExiter() : exiter;
    }

    public interface Exiter {
        void exit(int status);
    }

    private static class DefaultExiter implements Exiter{
        public void exit(int status) {
            System.exit(status);
        }
    }
}
