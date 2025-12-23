/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.ejb.timer.app;

import java.io.Serializable;

public class TimerInfo implements Serializable {

    private static final long serialVersionUID = 6490189841768125878L;

    Long duration;
    String info;
    Boolean preDestroy;
    Boolean postConstruct;

    public TimerInfo() {

    }

    public TimerInfo(Long duration, String info, Boolean preDestroy, Boolean postConstruct) {
        this.duration = duration;
        this.info = info;
        this.preDestroy = preDestroy;
        this.postConstruct = postConstruct;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public Boolean getPreDestroy() {
        return preDestroy;
    }

    public void setPreDestroy(Boolean preDestroy) {
        this.preDestroy = preDestroy;
    }

    public Boolean getPostConstruct() {
        return postConstruct;
    }

    public void setPostConstruct(Boolean postConstruct) {
        this.postConstruct = postConstruct;
    }

    @Override
    public String toString() {
        return "TimerInfo [duration=" + duration + ", info=" + info + ", preDestroy=" + preDestroy + ", postConstruct="
                + postConstruct + "]";
    }

}
