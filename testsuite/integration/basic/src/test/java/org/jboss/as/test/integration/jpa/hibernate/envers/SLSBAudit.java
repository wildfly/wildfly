package org.jboss.as.test.integration.jpa.hibernate.envers;

import java.util.ArrayList;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;

/**
 * @author Madhumita Sadhukhan
 */
@Stateless
public class SLSBAudit {
    @PersistenceContext(unitName = "myCustPhone")
    EntityManager em;

    public Customer createCustomer(String firstName, String surName, String type, String areacode, String phnumber) {
        Phone phone1 = new Phone();
        phone1.setNumber(phnumber);
        phone1.setAreacode(areacode);
        phone1.setType(type);

        Phone phone2 = new Phone();
        phone2.setNumber("777222123");
        phone2.setAreacode("+420");
        phone2.setType("HOME");

        Customer cust = new Customer();
        cust.setFirstname(firstName);
        cust.setSurname(surName);

        cust.getPhones().add(phone1);
        cust.getPhones().add(phone2);
        em.persist(cust);

        em.persist(phone1);
        em.persist(phone2);

        return cust;
    }

    public Customer updateCustomer(Customer c) {
        return em.merge(c);
    }

    public Phone updatePhone(Phone p) {
        return em.merge(p);
    }

    public void deletePhone(Phone p) {
        em.remove(em.merge(p));
    }

    public int retrieveOldPhoneListSizeFromCustomer(int id, int revnumber) {
        AuditReader reader = AuditReaderFactory.get(em);
        Customer cust_rev = reader.find(Customer.class, id, revnumber);
        return cust_rev.getPhones().size();
    }

    public String retrieveOldPhoneListVersionFromCustomer(int id) {
        AuditReader reader = AuditReaderFactory.get(em);
        Customer cust_rev = reader.find(Customer.class, id, 2);
        return cust_rev.getPhones().get(1).getType();
    }

    public List<Object> verifyRevision(int id) {

        AuditReader reader = AuditReaderFactory.get(em);
        // boolean b;
        // String queryString = "select a.originalId.REV from " + "CUSTOMER_PHONE" + "_AUD a";
        // String queryString = "select column_name from information_schema.columns where table_name = 'CUSTOMER_PHONE_AUD'";
        // Query query = em.createQuery(queryString);
        List<Object> custHistory = new ArrayList<Object>();
        List<Number> revList = reader.getRevisions(Customer.class, id);

        for (Number revisionNumber : revList) {

            AuditQuery query = reader.createQuery().forEntitiesAtRevision(Customer.class, revisionNumber);
            query.add(AuditEntity.property("firstname").eq("MADHUMITA"));
            if (query.getResultList() != null && query.getResultList().size() > 0) { custHistory.add(query.getResultList()); }
        }

        return custHistory;

    }

    public List<Object> verifyRevisionType(int id) {

        AuditReader reader = AuditReaderFactory.get(em);
        List<Object> custHistory = new ArrayList<Object>();
        List<Number> revList = reader.getRevisions(Customer.class, id);

        for (Number revisionNumber : revList) {

            AuditQuery query = reader.createQuery().forEntitiesAtRevision(Customer.class, revisionNumber);
            query.add(AuditEntity.revisionType().eq(RevisionType.MOD));
            if (query.getResultList() != null && query.getResultList().size() > 0) { custHistory.add(query.getResultList()); }
        }

        return custHistory;

    }

    public List<Object> verifyOtherFields(int id) {

        AuditReader reader = AuditReaderFactory.get(em);
        boolean b;

        Customer cust1_rev = reader.find(Customer.class, id, 3);
        String queryString = "select a.originalId.phones_id from CUSTOMER_PHONE_AUD a";

        Query query = em.createQuery(queryString);

        List<Object> custHistory = query.getResultList();

        return custHistory;
    }
}
