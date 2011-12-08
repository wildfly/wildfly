package org.jboss.as.test.integration.jpa.beanvalidation;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * @author Madhumita Sadhukhan
 */
@Stateless
public class SLSBInheritance {

    @PersistenceContext(unitName = "myPlayer")
    EntityManager em;

    public SoccerPlayer createSoccerPlayer(String firstName, String lastName, String game, String clubName) {

        SoccerPlayer socplayer = new SoccerPlayer();
        socplayer.setFirstName(firstName);
        socplayer.setLastName(lastName);
        socplayer.setGame(game);
        socplayer.setClubName(clubName);
        em.persist(socplayer);
        return socplayer;
    }

    public SoccerPlayer updateSoccerPlayer(SoccerPlayer p) {

        em.merge(p);
        return p;
    }

}
