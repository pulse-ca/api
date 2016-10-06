package gov.ca.emsa.pulse.broker.adapter;

import java.util.List;
import gov.ca.emsa.pulse.common.domain.PatientSearch;
import gov.ca.emsa.pulse.broker.dto.DocumentDTO;
import gov.ca.emsa.pulse.broker.dto.OrganizationDTO;
import gov.ca.emsa.pulse.broker.dto.PatientOrganizationMapDTO;
import gov.ca.emsa.pulse.broker.dto.PatientRecordDTO;
import gov.ca.emsa.pulse.broker.saml.SAMLInput;

public interface Adapter {
	public List<PatientRecordDTO> queryPatients(OrganizationDTO org, PatientSearch toSearch, SAMLInput samlInput);
	public List<DocumentDTO> queryDocuments(OrganizationDTO org, PatientOrganizationMapDTO orgPatient, SAMLInput samlInput);
	public void retrieveDocumentsContents(OrganizationDTO org, List<DocumentDTO> documents, SAMLInput samlInput);
}
