/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.multipleviews;

import jakarta.ejb.Local;
import jakarta.ejb.Stateful;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * @author Stuart Douglas
 */
@Stateful
@ApplicationScoped
@Local({MusicPlayer.class, EntertainmentDevice.class})
public class Mp3Player implements MusicPlayer, EntertainmentDevice {

    private int songCount;

    public int getSongCount() {
        return songCount;
    }

    public void setSongCount(final int songCount) {
        this.songCount = songCount;
    }
}
