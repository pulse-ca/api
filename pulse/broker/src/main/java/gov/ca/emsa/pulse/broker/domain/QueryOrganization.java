package gov.ca.emsa.pulse.broker.domain;

import java.util.ArrayList;
import java.util.List;

import gov.ca.emsa.pulse.broker.dto.DtoToDomainConverter;
import gov.ca.emsa.pulse.broker.dto.PatientRecordDTO;
import gov.ca.emsa.pulse.broker.dto.QueryOrganizationDTO;

public class QueryOrganization {
	private Long id;
	private Long queryId;
	private Long orgId;
	private String status;
	private Long startDate;
	private Long endDate;
	private Boolean success;
	private List<PatientRecord> results;
	
	public QueryOrganization() {
		results = new ArrayList<PatientRecord>();
	}
	
	public QueryOrganization(QueryOrganizationDTO dto) {
		this();
		
		this.id = dto.getId();
		this.queryId = dto.getQueryId();
		this.orgId = dto.getOrgId();
		this.status = dto.getStatus();
		this.success = dto.getSuccess();
		if(dto.getStartDate() != null) {
			this.startDate = dto.getStartDate().getTime();
		}
		if(dto.getEndDate() != null) {
			this.endDate = dto.getEndDate().getTime();
		}
		
		if(dto.getResults() != null && dto.getResults().size() > 0) {
			for(PatientRecordDTO prDto : dto.getResults()) {
				PatientRecord pr = new PatientRecord(prDto);
				this.results.add(pr);
			}
		}
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getQueryId() {
		return queryId;
	}

	public void setQueryId(Long queryId) {
		this.queryId = queryId;
	}

	public Long getOrgId() {
		return orgId;
	}

	public void setOrgId(Long orgId) {
		this.orgId = orgId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Long getStartDate() {
		return startDate;
	}

	public void setStartDate(Long startDate) {
		this.startDate = startDate;
	}

	public Long getEndDate() {
		return endDate;
	}

	public void setEndDate(Long endDate) {
		this.endDate = endDate;
	}

	public Boolean getSuccess() {
		return success;
	}

	public void setSuccess(Boolean success) {
		this.success = success;
	}

	public List<PatientRecord> getResults() {
		return results;
	}

	public void setResults(List<PatientRecord> results) {
		this.results = results;
	}
}
