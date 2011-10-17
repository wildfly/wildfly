package org.jboss.as.host.controller;

import java.util.concurrent.TimeUnit;

/**
 * Policy used to automatically try to reconnect to a crashed master HC.
 *
 * @author Emanuel Muckenhuber
 */
interface ReconnectPolicy {

    /**
     * Respawn a process.
     *
     * @param process
     */
    void wait(int count) throws InterruptedException;


    ReconnectPolicy CONNECT = new ReconnectPolicy() {

        @Override
        public void wait(int count) throws InterruptedException {
            final int waitPeriod;
            if (count < 5) {
                waitPeriod = 100;
            } else if (count >= 5 && count < 10) {
                waitPeriod = 300;
            } else if (count >= 10 && count < 15) {
                waitPeriod = 1000;
            } else {
                waitPeriod = 5000;
            }
            TimeUnit.MILLISECONDS.sleep(waitPeriod);
        }
    };

    ReconnectPolicy RECONNECT = new ReconnectPolicy() {

        private static final int MAX_WAIT = 15;

        @Override
        public void wait(final int count) throws InterruptedException {
            final int waitPeriod = Math.min((count * count), MAX_WAIT);
            TimeUnit.SECONDS.sleep(waitPeriod);
        }
    };




}
