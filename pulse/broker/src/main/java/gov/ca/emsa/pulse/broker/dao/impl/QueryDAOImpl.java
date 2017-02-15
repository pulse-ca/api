package gov.ca.emsa.pulse.broker.dao.impl;

import gov.ca.emsa.pulse.broker.dao.QueryDAO;
import gov.ca.emsa.pulse.broker.dto.QueryDTO;
import gov.ca.emsa.pulse.broker.dto.QueryLocationMapDTO;
import gov.ca.emsa.pulse.broker.entity.QueryEntity;
import gov.ca.emsa.pulse.broker.entity.QueryLocationMapEntity;
import gov.ca.emsa.pulse.common.domain.QueryStatus;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Query;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import gov.ca.emsa.pulse.broker.dao.QueryStatusDAO;
import gov.ca.emsa.pulse.common.domain.QueryLocationStatus;
import gov.ca.emsa.pulse.broker.entity.QueryLocationStatusEntity;
import gov.ca.emsa.pulse.broker.entity.QueryStatusEntity;

@Repository
public class QueryDAOImpl extends BaseDAOImpl implements QueryDAO {
	private static final Logger logger = LogManager.getLogger(QueryDAOImpl.class);

	@Autowired QueryStatusDAO statusDao;
	
	@Override
	public QueryDTO create(QueryDTO dto) {
		
		QueryEntity query = new QueryEntity();
		query.setUserId(dto.getUserId());
		QueryStatusEntity queryStatus = statusDao.getQueryStatusByName(dto.getStatus() != null ? dto.getStatus().name() : QueryStatus.Active.name());
		query.setStatus(queryStatus);
		if(queryStatus != null) {
			query.setStatusId(queryStatus.getId());
		}
		query.setTerms(dto.getTerms());
		query.setLastReadDate(new Date());
		entityManager.persist(query);
		
		if(dto.getLocationStatuses() != null && dto.getLocationStatuses().size() > 0) {
			for(QueryLocationMapDTO queryLocStatus : dto.getLocationStatuses()) {
				QueryLocationMapEntity queryLocMap = new QueryLocationMapEntity();
				queryLocMap.setLocationId(queryLocStatus.getLocationId());
				queryLocMap.setQueryId(query.getId());
				queryLocMap.setStartDate(new Date());
				QueryLocationStatusEntity status = statusDao.getQueryLocationStatusByName(QueryLocationStatus.Active.name());
				queryLocMap.setStatusId(status == null ? null : status.getId());
				queryLocMap.setStatus(status);
				entityManager.persist(queryLocMap);
				query.getLocationStatuses().add(queryLocMap);
			}
		}
		
		entityManager.flush();
		return new QueryDTO(query);
	}

	public QueryLocationMapDTO getQueryLocationById(Long queryOrgId) {
		QueryLocationMapEntity entity = null;
		
		Query query = entityManager.createQuery( "SELECT q from QueryLocationMapEntity q "
				+ "JOIN FETCH q.status "
				+ "where q.id = :entityid) ", 
				QueryLocationMapEntity.class );
		
		query.setParameter("entityid", queryOrgId);
		List<QueryLocationMapEntity> results = query.getResultList();
		if(results.size() != 0) {
			entity = results.get(0);
		}
		return new QueryLocationMapDTO(entity);
	}
	
	public QueryLocationMapDTO getQueryLocationMapByQueryAndLocation(Long queryId, Long locationId) {
		QueryLocationMapEntity entity = null;
		
		Query query = entityManager.createQuery( "SELECT q from QueryLocationMapEntity q "
				+ "JOIN FETCH q.status "
				+ "where q.queryId = :queryId " 
				+ "AND q.locationId = :locationId", 
				QueryLocationMapEntity.class );
		
		query.setParameter("queryId", queryId);
		query.setParameter("locationId", locationId);
		List<QueryLocationMapEntity> results = query.getResultList();
		if(results.size() != 0) {
			entity = results.get(0);
		}
		return new QueryLocationMapDTO(entity);
	}
	
	public QueryLocationMapDTO createQueryLocationMap(QueryLocationMapDTO queryLocationMapDto) {
		QueryLocationMapEntity toInsert = new QueryLocationMapEntity();
		toInsert.setLocationId(queryLocationMapDto.getLocationId());
		toInsert.setQueryId(queryLocationMapDto.getQueryId());
		toInsert.setStartDate(new Date());
		QueryLocationStatusEntity status = statusDao.getQueryLocationStatusByName(QueryLocationStatus.Active.name());
		toInsert.setStatusId(status == null ? null : status.getId());
		entityManager.persist(toInsert);
		entityManager.flush();
		return new QueryLocationMapDTO(toInsert);
	}
	
	@Override
	public QueryLocationMapDTO updateQueryLocationMap(QueryLocationMapDTO newQueryLocationMap) {
		logger.info("Update query location " + newQueryLocationMap.getId() + " with status " + newQueryLocationMap.getStatus());
		QueryLocationMapEntity existingQueryLocationMap = getQueryStatusById(newQueryLocationMap.getId());
		if(existingQueryLocationMap != null) {
			existingQueryLocationMap.setEndDate(newQueryLocationMap.getEndDate());
			if((newQueryLocationMap.getStatus() != null && 
				newQueryLocationMap.getStatus() == QueryLocationStatus.Closed) 
				||
				(existingQueryLocationMap.getStatus() != null && 
				(existingQueryLocationMap.getStatus().getStatus() != QueryLocationStatus.Cancelled || 
				existingQueryLocationMap.getStatus().getStatus() != QueryLocationStatus.Closed))) {
				//always change the status if we are moving to Closed.
				//aside from that, don't change the status if it's currently Cancelled or Closed.
				QueryLocationStatusEntity newStatus = 
						statusDao.getQueryLocationStatusByName(newQueryLocationMap.getStatus().name());
				existingQueryLocationMap.setStatusId(newStatus == null ? null : newStatus.getId());
			} 
			entityManager.merge(existingQueryLocationMap);
			entityManager.flush();
			logger.info("Updated query location Status " + newQueryLocationMap.getId());
			return new QueryLocationMapDTO(existingQueryLocationMap);
		} else {
			logger.info("Could not find a query location map with ID " + newQueryLocationMap.getId());
		}
		return null;
	}
	
	@Override
	public QueryDTO update(QueryDTO dto) {
		QueryEntity query = this.getEntityById(dto.getId());
		if(query.getStatus().getStatus() != QueryStatus.Closed) {
			logger.info("Set last read date for query " + dto.getId() + " to " + dto.getLastReadDate());
			query.setLastReadDate(dto.getLastReadDate());
			logger.info("Set status for query " + dto.getId() + " to " + dto.getStatus());
			QueryStatusEntity qStatus = statusDao.getQueryStatusByName(dto.getStatus().name());
			query.setStatus(qStatus);
			if(qStatus != null) {
				query.setStatusId(qStatus.getId());
			}
			//terms and user wouldn't change
			//location statuses should be updated separately in a manager
			entityManager.merge(query);
			entityManager.flush();
		} else {
			logger.warn("Attempted to update query " + dto.getId() + " but it is already marked 'Closed'. Not updating.");
		}
		return new QueryDTO(query);
	}

	@Override
	public void close(Long id) {
		try {
			QueryStatusEntity closedStatus = statusDao.getQueryStatusByName(QueryStatus.Closed.name());
			QueryEntity toDelete = getEntityById(id);
			if(closedStatus != null && toDelete != null) {
				toDelete.setStatus(closedStatus);
				toDelete.setStatusId(closedStatus.getId());
				entityManager.merge(toDelete);
				entityManager.flush();
			}
		} catch(Exception ex) {
			logger.error("Could not delete query with id " + id + ". Maybe it was deleted by another thread?", ex);
		}
	}

	@Override
	public List<QueryDTO> findAllForUser(String user) {
		List<QueryEntity> result = this.getEntitiesByUser(user);
		List<QueryDTO> dtos = new ArrayList<QueryDTO>(result.size());
		for(QueryEntity entity : result) {
			dtos.add(new QueryDTO(entity));
		}
		return dtos;
	}
	
	@Override
	public List<QueryDTO> findAllForUserWithStatus(String userToken, List<QueryStatus> status) {
		List<QueryEntity> result = this.getEntitiesByUserAndStatus(userToken, status);
		List<QueryDTO> dtos = new ArrayList<QueryDTO>(result.size());
		for(QueryEntity entity : result) {
			dtos.add(new QueryDTO(entity));
		}
		return dtos;
	}
	
	@Override
	public QueryDTO getById(Long id) {
		
		QueryDTO dto = null;
		QueryEntity qe = this.getEntityById(id);
		
		if (qe != null) {
			dto = new QueryDTO(qe);
		}
		return dto;
	}
	
	@Override
	public void deleteItemsOlderThan(Date oldestDate) {		
		//this was originally deleting queries but since we want to  keep
		//them around for statistics, it's just marking them as Closed instead
		Query query = entityManager.createQuery( "from QueryEntity qe "
				+ " WHERE qe.lastReadDate <= :cacheDate");
		
		query.setParameter("cacheDate", oldestDate);
		List<QueryEntity> oldQueries = query.getResultList();
		if(oldQueries.size() > 0) {
			for(QueryEntity oldQuery : oldQueries) {
				close(oldQuery.getId());
				logger.info("Query Cleanup: Closed query with id " + oldQuery.getId());
			}
		} else {
			logger.info("Query Cleanup: Closed 0 queries.");
		}
	}
	
	private List<QueryEntity> findAllEntities() {
		Query query = entityManager.createQuery("from QueryEntity");
		return query.getResultList();
	}
	
	private QueryEntity getEntityById(Long id) {
		//the status entity was being cached and calling clear here
		//forces it to refresh based on the query we run
		entityManager.clear();
		
		QueryEntity entity = null;
		Query query = entityManager.createQuery( "SELECT distinct q from QueryEntity q "
				+ "LEFT OUTER JOIN FETCH q.status "
				+ "LEFT OUTER JOIN FETCH q.locationStatuses locs "
				+ "LEFT OUTER JOIN FETCH locs.status status "
				+ "where q.id = :entityid) ", 
				QueryEntity.class );
		
		query.setParameter("entityid", id);
		List<QueryEntity> results = query.getResultList();
		if(results.size() != 0) {
			entity = results.get(0);
		}
		return entity;
	}
	
	public Boolean hasActiveLocations(Long queryId) {		
		QueryLocationStatusEntity statusEntity = statusDao.getQueryLocationStatusByName(QueryLocationStatus.Active.name());
		if(statusEntity == null) {
			return Boolean.FALSE;
		}
		Query query = entityManager.createQuery( "SELECT count(loc) "
				+ "FROM QueryLocationMapEntity loc " 
				+ "WHERE loc.queryId = :queryId " 
				+ "AND statusId = :statusId", Long.class);
		
		query.setParameter("queryId", queryId);
		query.setParameter("statusId", statusEntity.getId());
		Long activeCount = (Long)query.getSingleResult();
		logger.info("Query " + queryId + " has " + activeCount + " ACTIVE statuses.");
		return activeCount > 0;
	}
	
	private QueryLocationMapEntity getQueryStatusById(Long id) {
		QueryLocationMapEntity entity = null;
		
		Query query = entityManager.createQuery( "SELECT distinct q from QueryLocationMapEntity q "
				+ "LEFT OUTER JOIN FETCH q.location " 
				+ "LEFT OUTER JOIN FETCH q.results "
				+ "JOIN FETCH q.status "
				+ "where q.id = :entityid) ", 
				QueryLocationMapEntity.class );
		
		query.setParameter("entityid", id);
		List<QueryLocationMapEntity> result = query.getResultList();
		if(result.size() == 1) {
			entity = result.get(0);
		}
		
		return entity;
	}
	
	private List<QueryEntity> getEntitiesByUser(String user) {
		Query query = entityManager.createQuery( "SELECT distinct q from QueryEntity q "
				+ "LEFT OUTER JOIN FETCH q.status "
				+ "LEFT OUTER JOIN FETCH q.locationStatuses "
				+ "where q.userId = :userId) ", 
				QueryEntity.class );
		
		query.setParameter("userId", user);
		List<QueryEntity> result = query.getResultList();
		return result;
	}
	
	private List<QueryEntity> getEntitiesByUserAndStatus(String user, List<QueryStatus> statuses) {		
		Query query = entityManager.createQuery( "SELECT distinct q from QueryEntity q "
				+ "LEFT OUTER JOIN FETCH q.status "
				+ "LEFT OUTER JOIN FETCH q.locationStatuses "
				+ "where q.userId = :userId "
				+ "and q.status.status IN (:status)) ", 
				QueryEntity.class );
		
		query.setParameter("userId", user);
		query.setParameter("status", statuses);
		List<QueryEntity> result = query.getResultList();
		return result;
	}
}