package org.jboss.as.process;

import java.util.concurrent.TimeUnit;

/**
 * Policy used to automatically restart a crashed process.
 *
 * @author Emanuel Muckenhuber
 */
interface RespawnPolicy {

    /**
     * Respawn a process.
     *
     * @param process
     */
    void respawn(int count, ManagedProcess process);

    RespawnPolicy NONE = new RespawnPolicy() {

        @Override
        public void respawn(final int count, final ManagedProcess process) {
            ProcessLogger.SERVER_LOGGER.tracef("not trying to respawn process %s.", process.getProcessName());
        }

    };

    RespawnPolicy RESPAWN = new RespawnPolicy() {

        private static final int MAX_WAIT = 60;
        private static final int MAX_RESTARTS = 10;

        @Override
        public void respawn(final int count, final ManagedProcess process) {
            if(count <= MAX_RESTARTS) {
                try {
                    final int waitPeriod = Math.min((count * count), MAX_WAIT);
                    ProcessLogger.SERVER_LOGGER.waitingToRestart(waitPeriod, process.getProcessName());
                    TimeUnit.SECONDS.sleep(waitPeriod);
                } catch (InterruptedException e) {
                    return;
                }
                process.respawn();
            }
        }
    };


}
