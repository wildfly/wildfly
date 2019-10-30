package org.jboss.as.test.integration.jpa.hibernate.envers;

import java.util.ArrayList;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;

/**
 * @author Madhumita Sadhukhan
 */
@Stateless
public class SLSBOrg {

    @PersistenceContext(unitName = "myOrg")
    EntityManager em;

    public Organization createOrg(String name, String type, String startDate, String endDate, String location) {

        Organization org = new Organization();
        org.setName(name);
        org.setType(type);
        org.setStartDate(startDate);
        org.setEndDate(endDate);
        org.setLocation(location);
        em.persist(org);
        return org;
    }

    public Organization updateOrg(Organization o) {
        return em.merge(o);
    }

    public void deleteOrg(Organization o) {
        em.remove(em.merge(o));
    }

    public Organization retrieveOldOrgbyId(int id) {

        AuditReader reader = AuditReaderFactory.get(em);
        List<Number> revList = reader.getRevisions(Organization.class, id);

        Organization org1_rev = reader.find(Organization.class, id, 2);
        return org1_rev;
    }

    public Organization retrieveDeletedOrgbyId(int id) {

        AuditReader reader = AuditReaderFactory.get(em);
        List<Number> revList = reader.getRevisions(Organization.class, id);
        /*for (Number revisionNumber : revList) {
            System.out.println("Available revisionNumber for o1:" + revisionNumber);
        }*/
        List<Object> custHistory = new ArrayList<Object>();
        AuditQuery query = reader.createQuery().forRevisionsOfEntity(Organization.class, true, true);
        query.add(AuditEntity.revisionType().eq(RevisionType.DEL));

        Organization rev = (Organization) (((List<Object>) (query.getResultList())).toArray()[0]);

        return rev;
    }

    public Organization retrieveOldOrgbyEntityName(String name, int id) {

        AuditReader reader = AuditReaderFactory.get(em);
        Organization org1_rev = reader.find(Organization.class, name, id, 3);
        return org1_rev;
    }

}
