package gov.ca.emsa.pulse.broker.manager.impl;

import java.util.Date;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import gov.ca.emsa.pulse.broker.adapter.Adapter;
import gov.ca.emsa.pulse.broker.adapter.AdapterFactory;
import gov.ca.emsa.pulse.broker.dto.OrganizationDTO;
import gov.ca.emsa.pulse.broker.dto.PatientRecordDTO;
import gov.ca.emsa.pulse.broker.dto.QueryOrganizationDTO;
import gov.ca.emsa.pulse.broker.dto.QueryStatus;
import gov.ca.emsa.pulse.broker.manager.PatientManager;
import gov.ca.emsa.pulse.broker.manager.QueryManager;

@Component
public class PatientQueryService implements Runnable {
	private static final Logger logger = LogManager.getLogger(PatientQueryService.class);

	private QueryOrganizationDTO queryOrg;
	private OrganizationDTO org;
	@Autowired private QueryManager queryManager;
	@Autowired private PatientManager patientManager;
	@Autowired private AdapterFactory adapterFactory;
	private PatientRecordDTO toSearch;
	private String samlMessage;
	
	@Override
	@Transactional
	public void run() {
		boolean queryError = false;
		//query this organization directly for patient matches
		List<PatientRecordDTO> searchResults = null;
		Adapter adapter = adapterFactory.getAdapter(org);
		if(adapter != null) {
			logger.info("Starting query to " + org.getAdapter() + " for orgquery " + queryOrg.getId());
			try {
				searchResults = adapter.queryPatients(org, toSearch, samlMessage);
			} catch(Exception ex) {
				logger.error("Exception thrown in adapter " + adapter.getClass(), ex);
				queryError = true;
			}
		}
		//store the patients returned so we can retrieve them later when all orgs have finished querying
		if(searchResults != null && searchResults.size() > 0) {
			for(PatientRecordDTO patient : searchResults) {
				patient.setQueryOrganizationId(queryOrg.getId());
					
				//save the search results
				queryManager.addPatientRecord(patient);
			}
		} 
		
		queryOrg.setStatus(QueryStatus.COMPLETE.name());
		queryOrg.setEndDate(new Date());
		queryOrg.setSuccess(!queryError);
		queryManager.createOrUpdateQueryOrganization(queryOrg);
		logger.info("Completed query to " + org.getAdapter() + " for orgquery " + queryOrg.getId());
	}

	public QueryOrganizationDTO getQueryOrg() {
		return queryOrg;
	}

	public void setQueryOrg(QueryOrganizationDTO queryOrg) {
		this.queryOrg = queryOrg;
	}

	public OrganizationDTO getOrg() {
		return org;
	}

	public void setOrg(OrganizationDTO org) {
		this.org = org;
	}

	public PatientRecordDTO getToSearch() {
		return toSearch;
	}

	public void setToSearch(PatientRecordDTO toSearch) {
		this.toSearch = toSearch;
	}

	public String getSamlMessage() {
		return samlMessage;
	}

	public void setSamlMessage(String samlMessage) {
		this.samlMessage = samlMessage;
	}

	public QueryManager getQueryManager() {
		return queryManager;
	}

	public void setQueryManager(QueryManager queryManager) {
		this.queryManager = queryManager;
	}

	public PatientManager getPatientManager() {
		return patientManager;
	}

	public void setPatientManager(PatientManager patientManager) {
		this.patientManager = patientManager;
	}

	public AdapterFactory getAdapterFactory() {
		return adapterFactory;
	}

	public void setAdapterFactory(AdapterFactory adapterFactory) {
		this.adapterFactory = adapterFactory;
	}
}