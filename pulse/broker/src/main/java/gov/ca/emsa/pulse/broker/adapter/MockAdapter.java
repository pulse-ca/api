package gov.ca.emsa.pulse.broker.adapter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import gov.ca.emsa.pulse.broker.domain.Document;
import gov.ca.emsa.pulse.broker.domain.MockPatient;
import gov.ca.emsa.pulse.broker.dto.OrganizationDTO;
import gov.ca.emsa.pulse.broker.dto.PatientOrganizationMapDTO;
import gov.ca.emsa.pulse.broker.dto.PatientRecordDTO;
import gov.ca.emsa.pulse.broker.dto.SearchResultConverter;
import gov.ca.emsa.pulse.service.SearchService;

@Component
public class MockAdapter implements Adapter {
	private static final Logger logger = LogManager.getLogger(MockAdapter.class);
	private DateFormat formatter;

	public MockAdapter() {
		formatter = new SimpleDateFormat(SearchService.dobFormat);
	}
	
	@Override
	public List<PatientRecordDTO> queryPatients(OrganizationDTO org, PatientRecordDTO toSearch, String samlMessage) {
		String postUrl = org.getEndpointUrl() + "/patients";
		MultiValueMap<String,String> parameters = new LinkedMultiValueMap<String,String>();
		parameters.add("firstName", toSearch.getFirstName());
		parameters.add("lastName", toSearch.getLastName());
		if(toSearch.getDateOfBirth() != null) {
			parameters.add("dob", formatter.format(toSearch.getDateOfBirth()));
		}
		parameters.add("gender", toSearch.getGender());
		parameters.add("ssn", toSearch.getSsn());
		if(toSearch.getAddress() != null) {
			parameters.add("zipcode", toSearch.getAddress().getZipcode());
		}
		parameters.add("samlMessage", samlMessage);
		RestTemplate restTemplate = new RestTemplate();
		MockPatient[] searchResults = null;
		try {
			searchResults = restTemplate.postForObject(postUrl, parameters, MockPatient[].class);
		} catch(Exception ex) {
			logger.error("Exception when querying " + postUrl, ex);
			throw ex;
		}
		
		List<PatientRecordDTO> records = new ArrayList<PatientRecordDTO>();
		for(int i = 0; i < searchResults.length; i++) {
			PatientRecordDTO record = SearchResultConverter.convertToPatientRecord(searchResults[i]);
			records.add(record);
		}
		return records;
	}

	@Override
	public Document[] queryDocuments(OrganizationDTO org, PatientOrganizationMapDTO toSearch, String samlMessage) {
		String postUrl = org.getEndpointUrl() + "/documents";
		MultiValueMap<String,String> parameters = new LinkedMultiValueMap<String,String>();
		parameters.add("patientId", toSearch.getOrgPatientId());
		parameters.add("samlMessage", samlMessage);
		RestTemplate restTemplate = new RestTemplate();
		Document[] searchResults = null;
		try {
			searchResults = restTemplate.postForObject(postUrl, parameters, Document[].class);
		} catch(Exception ex) {
			logger.error("Exception when querying " + postUrl, ex);
			throw ex;
		}
		return searchResults;
	}
}
