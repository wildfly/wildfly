/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jpa.hibernate.envers;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import org.hibernate.envers.Audited;

/**
 * @author Madhumita Sadhukhan
 */

@Entity
@Audited
@Table(name = "SOCCERPLAYER")
@PrimaryKeyJoinColumn(name = "SOCCERPLAYER_ID")
public class SoccerPlayer extends Player {

    private String clubName;

    public String getClubName() {
        return clubName;
    }

    public void setClubName(String clubName) {
        this.clubName = clubName;
    }
}
