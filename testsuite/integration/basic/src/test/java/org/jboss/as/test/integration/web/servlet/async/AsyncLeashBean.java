/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.servlet.async;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;

import jakarta.ejb.Singleton;
import jakarta.enterprise.context.ApplicationScoped;

@Singleton
@ApplicationScoped
public class AsyncLeashBean implements AsyncLeash {// , AsyncListener{

    private long initTStamp = -1;
    private long timeoutTStamp = -1;
    private long expectedTDiff = -1;
    private LinkedList<Event> details;

    @Override
    public void init(final long expected) {
        this.initTStamp = System.currentTimeMillis();
        this.expectedTDiff = expected;
        this.details = new LinkedList<Event>();
        log("init", null);
    }

    @Override
    public void onTimeout(final Throwable throwable) {
        log("onTimeout", throwable);
        this.timeoutTStamp = System.currentTimeMillis();
    }

    @Override
    public void onComplete() {
        log("onError", null);
    }

    @Override
    public void onError(final Throwable throwable) {
        log("onError", throwable);
    }

    @Override
    public void onStartAsync() {
        log("onStartAsync", null);
    }

    @Override
    public boolean isTimeout() {
        return timeoutTStamp != -1;
    }

    @Override
    public String detail() {
        final StringBuilder sb = new StringBuilder();
        for(Event e: details) {
            sb.append(isTimeout()).append(e.toString()).append("\n");
        }
        return sb.toString();
    }

    @Override
    public long timeoutTStamp() {
        return this.timeoutTStamp;
    }

    @Override
    public long initTStamp() {
        return this.initTStamp;
    }

    @Override
    public boolean initialized() {
        return initTStamp != -1;
    }

    @Override
    public long getExpectedTDiff() {
        return expectedTDiff;
    }

    //just to give insight into test run, JIC.
    private void log(final String name, final Throwable t) {
        final Event e = new Event(name, t);
        this.details.add(e);
    }
    private final class Event{
        final String name;
        final long eventTStamp = System.currentTimeMillis();
        final String stackTrace;
        public Event(String name, Throwable t) {
            this.name = name;
            if(t != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                t.printStackTrace(pw);
                stackTrace = sw.toString();
            } else {
                stackTrace  = "N/A";
            }
        }

        public String toString() {
            return this.name+"\n - tStamp: "+this.eventTStamp+"\n - trace: "+this.stackTrace;
        }
    }
}
