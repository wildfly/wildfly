/*
 *
 *  * JBoss, Home of Professional Open Source.
 *  * Copyright $year Red Hat, Inc., and individual contributors
 *  * as indicated by the @author tags.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.wildfly.test.preview.hibernate.search.coordination;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.hibernate.search.mapper.orm.Search;

import java.util.List;

@ApplicationScoped
@Transactional
public class SearchBean {

    @PersistenceContext
    EntityManager em;

    @SuppressWarnings("unchecked")
    public List<String> findAgentNames() {
        return em.createNativeQuery("select name from HSEARCH_AGENT")
                .getResultList();
    }

    public void create(String text) {
        IndexedEntity entity = new IndexedEntity();
        entity.text = text;
        em.persist(entity);
    }

    public List<IndexedEntity> search(String keyword) {
        return Search.session(em).search(IndexedEntity.class)
                .where(f -> f.match().field("text").matching(keyword))
                .fetchAllHits();
    }
}
