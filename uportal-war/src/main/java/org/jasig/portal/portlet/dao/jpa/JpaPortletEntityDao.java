/**
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.jasig.portal.portlet.dao.jpa;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;

import org.apache.commons.lang.Validate;
import org.jasig.portal.jpa.BasePortalJpaDao;
import org.jasig.portal.portlet.dao.IPortletDefinitionDao;
import org.jasig.portal.portlet.dao.IPortletEntityDao;
import org.jasig.portal.portlet.om.IPortletDefinition;
import org.jasig.portal.portlet.om.IPortletDefinitionId;
import org.jasig.portal.portlet.om.IPortletEntity;
import org.jasig.portal.portlet.om.IPortletEntityId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Eric Dalquist
 * @version $Revision$
 */
@Repository
@Qualifier("persistence")
public class JpaPortletEntityDao extends BasePortalJpaDao implements IPortletEntityDao {
    private static final String FIND_PORTLET_ENTS_BY_USER_ID_CACHE_REGION = PortletEntityImpl.class.getName() + ".query.FIND_PORTLET_ENTS_BY_USER_ID";
    private static final String FIND_PORTLET_ENTS_BY_PORTLET_DEF_CACHE_REGION = PortletEntityImpl.class.getName() + ".query.FIND_PORTLET_ENTS_BY_PORTLET_DEF";
    private static final String FIND_PORTLET_ENT_BY_CHAN_SUB_AND_USER_CACHE_REGION = PortletEntityImpl.class.getName() + ".query.FIND_PORTLET_ENT_BY_CHAN_SUB_AND_USER";
    
    private CriteriaQuery<PortletEntityImpl> findEntityBySubIdAndUserIdQuery;
    private CriteriaQuery<PortletEntityImpl> findEntitiesForDefinitionQuery;
    private CriteriaQuery<PortletEntityImpl> findEntitiesForUserIdQuery;
    private ParameterExpression<String> channelSubIdParameter;
    private ParameterExpression<Integer> userIdParameter;
    private ParameterExpression<PortletDefinitionImpl> portletDefinitionParameter;

    private IPortletDefinitionDao portletDefinitionDao;
    
    
    @Autowired
    public void setPortletDefinitionDao(IPortletDefinitionDao portletDefinitionDao) {
        this.portletDefinitionDao = portletDefinitionDao;
    }
    
    @Override
    protected void buildCriteriaQueries(CriteriaBuilder cb) {
        this.channelSubIdParameter = cb.parameter(String.class, "channelSubscribeId");
        this.userIdParameter = cb.parameter(Integer.class, "userId");
        this.portletDefinitionParameter = cb.parameter(PortletDefinitionImpl.class, "portletDefinition");
        
        this.findEntityBySubIdAndUserIdQuery = this.buildFindEntityBySubIdAndUserIdQuery(cb);
        this.findEntitiesForDefinitionQuery = this.buildFindEntitiesForDefinitionQuery(cb);
        this.findEntitiesForUserIdQuery = this.buildFindEntitiesForUserIdQuery(cb);
    }

    protected CriteriaQuery<PortletEntityImpl> buildFindEntityBySubIdAndUserIdQuery(final CriteriaBuilder cb) {
        final CriteriaQuery<PortletEntityImpl> criteriaQuery = cb.createQuery(PortletEntityImpl.class);
        final Root<PortletEntityImpl> entityRoot = criteriaQuery.from(PortletEntityImpl.class);
        criteriaQuery.select(entityRoot);
        criteriaQuery.where(
            cb.and(
                cb.equal(entityRoot.get(PortletEntityImpl_.channelSubscribeId), this.channelSubIdParameter),
                cb.equal(entityRoot.get(PortletEntityImpl_.userId), this.userIdParameter)
            )
        );
        
        return criteriaQuery;
    }

    protected CriteriaQuery<PortletEntityImpl> buildFindEntitiesForDefinitionQuery(final CriteriaBuilder cb) {
        final CriteriaQuery<PortletEntityImpl> criteriaQuery = cb.createQuery(PortletEntityImpl.class);
        final Root<PortletEntityImpl> entityRoot = criteriaQuery.from(PortletEntityImpl.class);
        criteriaQuery.select(entityRoot);
        criteriaQuery.where(
            cb.equal(entityRoot.get(PortletEntityImpl_.portletDefinition), this.portletDefinitionParameter)
        );
        
        return criteriaQuery;
    }

    protected CriteriaQuery<PortletEntityImpl> buildFindEntitiesForUserIdQuery(final CriteriaBuilder cb) {
        final CriteriaQuery<PortletEntityImpl> criteriaQuery = cb.createQuery(PortletEntityImpl.class);
        final Root<PortletEntityImpl> entityRoot = criteriaQuery.from(PortletEntityImpl.class);
        criteriaQuery.select(entityRoot);
        criteriaQuery.where(
            cb.equal(entityRoot.get(PortletEntityImpl_.userId), this.userIdParameter)
        );
        
        return criteriaQuery;
    }

    /* (non-Javadoc)
     * @see org.jasig.portal.dao.portlet.IPortletEntityDao#createPortletEntity(org.jasig.portal.om.portlet.IPortletDefinitionId, java.lang.String, int)
     */
    @Override
    @Transactional
    public IPortletEntity createPortletEntity(IPortletDefinitionId portletDefinitionId, String channelSubscribeId, int userId) {
        Validate.notNull(portletDefinitionId, "portletDefinitionId can not be null");
        Validate.notNull(channelSubscribeId, "channelSubscribeId can not be null");
        
        final IPortletDefinition portletDefinition = this.portletDefinitionDao.getPortletDefinition(portletDefinitionId);
        if (portletDefinition == null) {
            throw new DataRetrievalFailureException("No IPortletDefinition exists for IPortletDefinitionId='" + portletDefinitionId + "'");
        }
        
        IPortletEntity portletEntity = new PortletEntityImpl(portletDefinition, channelSubscribeId, userId);
        
        this.entityManager.persist(portletEntity);

        return portletEntity;
    }

    /* (non-Javadoc)
     * @see org.jasig.portal.dao.portlet.IPortletEntityDao#deletePortletEntity(org.jasig.portal.om.portlet.IPortletEntity)
     */
    @Override
    @Transactional
    public void deletePortletEntity(IPortletEntity portletEntity) {
        Validate.notNull(portletEntity, "portletEntity can not be null");
        
        final IPortletEntity persistentPortletEntity;
        if (this.entityManager.contains(portletEntity)) {
            persistentPortletEntity = portletEntity;
        }
        else {
            persistentPortletEntity = this.entityManager.merge(portletEntity);
        }
        
        this.entityManager.remove(persistentPortletEntity);
    }

    @Override
    public IPortletEntity getPortletEntity(IPortletEntityId portletEntityId) {
        Validate.notNull(portletEntityId, "portletEntityId can not be null");
        
        final long internalPortletEntityId = getNativePortletEntityId(portletEntityId);
        final PortletEntityImpl portletEntity = this.entityManager.find(PortletEntityImpl.class, internalPortletEntityId);
        return portletEntity;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean portletEntityExists(IPortletEntityId portletEntityId) {
        Validate.notNull(portletEntityId, "portletEntityId can not be null");
        
        this.entityManager.clear();
        
        final long internalPortletEntityId = getNativePortletEntityId(portletEntityId);
        final PortletEntityImpl portletEntity = this.entityManager.find(PortletEntityImpl.class, internalPortletEntityId);
        return portletEntity != null;
    }

    /* (non-Javadoc)
     * @see org.jasig.portal.dao.portlet.IPortletEntityDao#getPortletEntity(java.lang.String, int)
     */
    @Override
    public IPortletEntity getPortletEntity(String channelSubscribeId, int userId) {
        Validate.notNull(channelSubscribeId, "portletEntity can not be null");
        
        final TypedQuery<PortletEntityImpl> query = this.createQuery(findEntityBySubIdAndUserIdQuery, FIND_PORTLET_ENT_BY_CHAN_SUB_AND_USER_CACHE_REGION);
        query.setParameter(this.channelSubIdParameter, channelSubscribeId);
        query.setParameter(this.userIdParameter, userId);
        query.setMaxResults(1);
        
        final List<PortletEntityImpl> portletEntities = query.getResultList();
        final IPortletEntity portletEntity = DataAccessUtils.uniqueResult(portletEntities);
        return portletEntity;
    }

    /* (non-Javadoc)
     * @see org.jasig.portal.dao.portlet.IPortletEntityDao#getPortletEntities(org.jasig.portal.om.portlet.IPortletDefinitionId)
     */
    @Override
    public Set<IPortletEntity> getPortletEntities(IPortletDefinitionId portletDefinitionId) {
        Validate.notNull(portletDefinitionId, "portletEntity can not be null");
        
        final IPortletDefinition portletDefinition = this.portletDefinitionDao.getPortletDefinition(portletDefinitionId);
        
        final TypedQuery<PortletEntityImpl> query = this.createQuery(this.findEntitiesForDefinitionQuery, FIND_PORTLET_ENTS_BY_PORTLET_DEF_CACHE_REGION);
        query.setParameter(this.portletDefinitionParameter, (PortletDefinitionImpl)portletDefinition);
        
        final List<PortletEntityImpl> portletEntities = query.getResultList();
        return new HashSet<IPortletEntity>(portletEntities);
    }
    
    /* (non-Javadoc)
     * @see org.jasig.portal.portlet.dao.IPortletEntityDao#getPortletEntitiesForUser(int)
     */
    @Override
    public Set<IPortletEntity> getPortletEntitiesForUser(int userId) {
        final TypedQuery<PortletEntityImpl> query = this.createQuery(this.findEntitiesForUserIdQuery, FIND_PORTLET_ENTS_BY_USER_ID_CACHE_REGION);
        query.setParameter(this.userIdParameter, userId);
        
        final List<PortletEntityImpl> portletEntities = query.getResultList();
        return new HashSet<IPortletEntity>(portletEntities);
    }

    /* (non-Javadoc)
     * @see org.jasig.portal.dao.portlet.IPortletEntityDao#updatePortletEntity(org.jasig.portal.om.portlet.IPortletEntity)
     */
    @Override
    @Transactional
    public void updatePortletEntity(IPortletEntity portletEntity) {
        Validate.notNull(portletEntity, "portletEntity can not be null");

        this.entityManager.persist(portletEntity);
    }
    
    protected long getNativePortletEntityId(IPortletEntityId portletEntityId) {
        final long internalPortletEntityId = Long.parseLong(portletEntityId.getStringId());
        return internalPortletEntityId;
    }

}