package gov.ca.emsa.pulse.common.soap;

import gov.ca.emsa.pulse.common.domain.Address;
import gov.ca.emsa.pulse.common.domain.Document;
import gov.ca.emsa.pulse.common.domain.DocumentIdentifier;
import gov.ca.emsa.pulse.common.domain.DocumentQuery;
import gov.ca.emsa.pulse.common.domain.DocumentRetrieve;
import gov.ca.emsa.pulse.common.domain.GivenName;
import gov.ca.emsa.pulse.common.domain.Patient;
import gov.ca.emsa.pulse.common.domain.PatientRecord;
import gov.ca.emsa.pulse.common.domain.PatientSearch;
import ihe.iti.xds_b._2007.RetrieveDocumentSetRequestType;
import ihe.iti.xds_b._2007.RetrieveDocumentSetResponseType;
import ihe.iti.xds_b._2007.RetrieveDocumentSetResponseType.DocumentResponse;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;

import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryRequest;
import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryResponse;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ClassificationType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExternalIdentifierType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExtrinsicObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.IdentifiableType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.InternationalStringType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.LocalizedStringType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectListType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.SlotType1;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ValueListType;

import org.hl7.v3.ADExplicit;
import org.hl7.v3.AdxpExplicitCity;
import org.hl7.v3.AdxpExplicitState;
import org.hl7.v3.AdxpExplicitStreetAddressLine;
import org.hl7.v3.EnExplicitFamily;
import org.hl7.v3.EnExplicitGiven;
import org.hl7.v3.II;
import org.hl7.v3.PNExplicit;
import org.hl7.v3.PRPAIN201305UV02;
import org.hl7.v3.PRPAIN201306UV02;
import org.hl7.v3.PRPAIN201306UV02MFMIMT700711UV01Subject1;
import org.hl7.v3.PRPAIN201306UV02MFMIMT700711UV01Subject2;
import org.hl7.v3.PRPAMT201306UV02LivingSubjectAdministrativeGender;
import org.hl7.v3.PRPAMT201306UV02LivingSubjectBirthTime;
import org.hl7.v3.PRPAMT201306UV02LivingSubjectName;
import org.hl7.v3.PRPAMT201306UV02ParameterList;
import org.hl7.v3.PRPAMT201310UV02OtherIDs;
import org.hl7.v3.PRPAMT201310UV02Patient;
import org.hl7.v3.PRPAMT201310UV02Person;
import org.hl7.v3.TELExplicit;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SOAPToJSONServiceImpl implements SOAPToJSONService {
	private final static String PATIENT_ID_PARAMETER = "$XDSDocumentEntryPatientId";
	private final static String DOCUMENT_STATUS_PARAMETER = "$XDSDocumentEntryStatus";
	
	public PatientSearch convertToPatientSearch(PRPAIN201305UV02 request){
		PRPAMT201306UV02ParameterList requestParameterList = request.getControlActProcess().getQueryByParameter().getValue().getParameterList();
		PatientSearch ps = new PatientSearch();
		List<PRPAMT201306UV02LivingSubjectAdministrativeGender> gender = requestParameterList.getLivingSubjectAdministrativeGender();
		List<PRPAMT201306UV02LivingSubjectBirthTime> dob = requestParameterList.getLivingSubjectBirthTime();
		List<PRPAMT201306UV02LivingSubjectName> name = requestParameterList.getLivingSubjectName();
		if((gender != null && !gender.isEmpty()) && (dob != null && !dob.isEmpty()) && (name != null  && !name.isEmpty())){
			ps.setGender(gender.get(0).getValue().get(0).getCode());
			ps.setDob(dob.get(0).getValue().get(0).getValue());
			List<Serializable> names = name.get(0).getValue().get(0).getContent();
			for(Serializable nameInList: names){
				if(nameInList instanceof JAXBElement<?>){
					if(((JAXBElement<?>) nameInList).getName().getLocalPart().equals("given")){
						ArrayList<GivenName> givens = new ArrayList<GivenName>();
						GivenName given = new GivenName();
						given.setGivenName(((JAXBElement<EnExplicitGiven>) nameInList).getValue().getContent());
						givens.add(given);
						ps.getPatientName().setGivenName(givens);
					}else if(((JAXBElement<?>) nameInList).getName().getLocalPart().equals("family")){
						ps.getPatientName().setFamilyName(((JAXBElement<EnExplicitFamily>) nameInList).getValue().getContent());
					}
				}
			}
		}
		return ps;
	}
	
	public List<PatientRecord> convertToPatientRecords(PRPAIN201306UV02 request){
		List<PatientRecord> result = new ArrayList<PatientRecord>();
		
		List<PRPAIN201306UV02MFMIMT700711UV01Subject1> subjects = request.getControlActProcess().getSubject();
		if(subjects != null && subjects.size() > 0) {
			for(PRPAIN201306UV02MFMIMT700711UV01Subject1 subject : subjects) {
				PatientRecord patientRecord = new PatientRecord();				
				
				PRPAIN201306UV02MFMIMT700711UV01Subject2 currSubject = subject.getRegistrationEvent().getSubject1();
				PRPAMT201310UV02Patient patient = currSubject.getPatient();
				//set given and family name
				JAXBElement<PRPAMT201310UV02Person> patientPerson = patient.getPatientPerson();
				List<PNExplicit> names = patientPerson.getValue().getName();
				for(PNExplicit name : names) {
					List<Serializable> nameParts = name.getContent();
					for(Serializable namePart : nameParts) {
						if(namePart instanceof JAXBElement<?>) {
							if(((JAXBElement<?>) namePart).getName().getLocalPart().equalsIgnoreCase("given")) {
								ArrayList<GivenName> givens = new ArrayList<GivenName>();
								GivenName given = new GivenName();
								given.setGivenName(((JAXBElement<EnExplicitGiven>) namePart).getValue().getContent());
								givens.add(given);
								patientRecord.getPatientName().setGivenName(givens);
							} else if(((JAXBElement<?>) namePart).getName().getLocalPart().equalsIgnoreCase("family")) {
								patientRecord.getPatientName().setFamilyName(((JAXBElement<EnExplicitFamily>)namePart).getValue().getContent());
							}
						}
					}
				}
				
				patientRecord.setGender(patientPerson.getValue().getAdministrativeGenderCode().getCode());
				patientRecord.setDateOfBirth(patientPerson.getValue().getBirthTime().getValue());
				
				//TODO: just taking the first listed phone number for now
				//but eventually we should accommodate multiple phone numbers
				List<TELExplicit> tels = patientPerson.getValue().getTelecom();
				if(tels.size() >= 1) {
					patientRecord.setPhoneNumber(tels.get(0).getValue());
				} 
				
				//TODO: just taking the first listed address for now
				//but eventually we should accommodate multiple addresses
				List<ADExplicit> addressList = patientPerson.getValue().getAddr();
				if(addressList.size() >= 1) {
					Address address = new Address();
					ADExplicit addr = addressList.get(0);
					List<Serializable> addressContent = addr.getContent();
					for(Serializable line : addressContent) {
						if(line instanceof JAXBElement<?>) {
							if(((JAXBElement<?>) line).getName().getLocalPart().equalsIgnoreCase("streetAddressLine")) {
								if(StringUtils.isEmpty(address.getStreet1())) {
									address.setStreet1(((JAXBElement<AdxpExplicitStreetAddressLine>)line).getValue().getContent());
								} else if(StringUtils.isEmpty(address.getStreet2())) {
									address.setStreet2(((JAXBElement<AdxpExplicitStreetAddressLine>)line).getValue().getContent());
								}
							} else if(((JAXBElement<?>) line).getName().getLocalPart().equalsIgnoreCase("city")) {
								address.setCity(((JAXBElement<AdxpExplicitCity>)line).getValue().getContent());
							} else if(((JAXBElement<?>) line).getName().getLocalPart().equalsIgnoreCase("state")) {
								address.setState(((JAXBElement<AdxpExplicitState>)line).getValue().getContent());
							}
						}
					}
					patientRecord.setAddress(address);
				}
				result.add(patientRecord);
			}
		}
		
		return result;
	}
	
	public List<Patient> convertToPatients(PRPAIN201306UV02 request){
		List<Patient> result = new ArrayList<Patient>();
		
		List<PRPAIN201306UV02MFMIMT700711UV01Subject1> subjects = request.getControlActProcess().getSubject();
		if(subjects != null && subjects.size() > 0) {
			for(PRPAIN201306UV02MFMIMT700711UV01Subject1 subject : subjects) {
				Patient patient = new Patient();				
				
				PRPAIN201306UV02MFMIMT700711UV01Subject2 currSubject = subject.getRegistrationEvent().getSubject1();
				PRPAMT201310UV02Patient subjPatient = currSubject.getPatient();
				//set org patient id; just take the first one now and throw an exception 
				//if there aren't any because we won't be able to do anything else with this
				//patient without an id
				//building this from section 1.13.1 on http://sequoiaproject.org/wp-content/uploads/2014/11/nhin-query-for-documents-production-specification-v3.0.pdf
				List<II> ids = subjPatient.getId();
				if(ids != null && ids.size() > 0) {
					//TODO: prob want to store the extension and root separately
					patient.setOrgPatientId(ids.get(0).getExtension() + "^^^&amp;" + ids.get(0).getRoot() + "&amp;ISO");
				} else {
					patient.setOrgPatientId("COULD NOT PARSE OR WAS EMPTY");
				}
				
				//set given and family name
				JAXBElement<PRPAMT201310UV02Person> patientPerson = subjPatient.getPatientPerson();
				List<PNExplicit> names = patientPerson.getValue().getName();
				for(PNExplicit name : names) {
					List<Serializable> nameParts = name.getContent();
					for(Serializable namePart : nameParts) {
						if(namePart instanceof JAXBElement<?>) {
							if(((JAXBElement<?>) namePart).getName().getLocalPart().equalsIgnoreCase("given")) {
								ArrayList<GivenName> givens = new ArrayList<GivenName>();
								GivenName given = new GivenName();
								given.setGivenName(((JAXBElement<EnExplicitGiven>) namePart).getValue().getContent());
								givens.add(given);
							} else if(((JAXBElement<?>) namePart).getName().getLocalPart().equalsIgnoreCase("family")) {
								patient.getPatientName().setFamilyName(((JAXBElement<EnExplicitFamily>)namePart).getValue().getContent());
							}
						}
					}
				}
				
				patient.setGender(patientPerson.getValue().getAdministrativeGenderCode().getCode());
				patient.setDateOfBirth(patientPerson.getValue().getBirthTime().getValue());
				
				//TODO: just taking the first listed phone number for now
				//but eventually we should accommodate multiple phone numbers
				List<TELExplicit> tels = patientPerson.getValue().getTelecom();
				if(tels.size() >= 1) {
					patient.setPhoneNumber(tels.get(0).getValue());
				} 
				
				//TODO: just taking the first listed address for now
				//but eventually we should accommodate multiple addresses
				List<ADExplicit> addressList = patientPerson.getValue().getAddr();
				if(addressList.size() >= 1) {
					Address address = new Address();
					ADExplicit addr = addressList.get(0);
					List<Serializable> addressContent = addr.getContent();
					for(Serializable line : addressContent) {
						if(line instanceof JAXBElement<?>) {
							if(((JAXBElement<?>) line).getName().getLocalPart().equalsIgnoreCase("streetAddressLine")) {
								if(StringUtils.isEmpty(address.getStreet1())) {
									address.setStreet1(((JAXBElement<AdxpExplicitStreetAddressLine>)line).getValue().getContent());
								} else if(StringUtils.isEmpty(address.getStreet2())) {
									address.setStreet2(((JAXBElement<AdxpExplicitStreetAddressLine>)line).getValue().getContent());
								}
							} else if(((JAXBElement<?>) line).getName().getLocalPart().equalsIgnoreCase("city")) {
								address.setCity(((JAXBElement<AdxpExplicitCity>)line).getValue().getContent());
							} else if(((JAXBElement<?>) line).getName().getLocalPart().equalsIgnoreCase("state")) {
								address.setState(((JAXBElement<AdxpExplicitState>)line).getValue().getContent());
							}
						}
					}
					patient.setAddress(address);
				}
				
				//look for the SSN
				List<PRPAMT201310UV02OtherIDs> otherIds = patientPerson.getValue().getAsOtherIDs();
				for(PRPAMT201310UV02OtherIDs otherId : otherIds) {
					List<String> classCodes = otherId.getClassCode();
					for(String classCode : classCodes) {
						if(classCode.equalsIgnoreCase("CIT")) {
							List<II> citIds = otherId.getId();
							for(II citId : citIds) {
								if(citId.getRoot().equals("2.16.840.1.113883.4.1")) {
									patient.setSsn(citId.getExtension());
								}
							}
						}
					}
				}
				
				result.add(patient);
			}
		}
		
		return result;
	}
	
	public DocumentQuery convertToDocumentQuery(AdhocQueryRequest request){
		DocumentQuery docQuery = new DocumentQuery();
		List<SlotType1> slots = request.getAdhocQuery().getSlot();
		String patientId = null;
		String documentStatusOriginal;
		String[] documentStatusList = null;
		for(SlotType1 slot : slots){
			if(slot.getName().equals(PATIENT_ID_PARAMETER)){
				patientId = slot.getValueList().getValue().get(0);
			}else if(slot.getName().equals(DOCUMENT_STATUS_PARAMETER)){
				documentStatusOriginal = slot.getValueList().getValue().get(0);
				if(documentStatusOriginal.startsWith("(") && documentStatusOriginal.endsWith(")")){
					documentStatusOriginal.replace("(", "").replace(")", "");
					documentStatusList = documentStatusOriginal.split(",");
					for(String documentString : documentStatusList){
						documentString.replace("'", "").replace("'", "");
					}
				}
			}
		}
		docQuery.setPatientId(patientId);
		docQuery.setDocumentStatuses(documentStatusList);
		return docQuery;
	}
	
	public List<Document> convertToDocumentQueryResponse(AdhocQueryResponse response) {
		List<Document> documents = new ArrayList<Document>();
		RegistryObjectListType regList = response.getRegistryObjectList();
		List<JAXBElement<? extends IdentifiableType>> idTypes = regList.getIdentifiable();
		for(Object obj : idTypes) {
			if(obj instanceof JAXBElement<?>) {
				JAXBElement<?> jaxbObj = (JAXBElement<?>)obj;
				if(jaxbObj != null && jaxbObj.getValue() != null && jaxbObj.getValue() instanceof ExtrinsicObjectType) {
					Document doc = new Document();
					DocumentIdentifier docId = new DocumentIdentifier();
					ExtrinsicObjectType extObj = (ExtrinsicObjectType)jaxbObj.getValue();
					//home community id
					docId.setHomeCommunityId(extObj.getHome());
					
					//doc name
					List<LocalizedStringType> docNameLocals = extObj.getName().getLocalizedString();
					if(docNameLocals != null && docNameLocals.size() > 0) {
						doc.setName(docNameLocals.get(0).getValue());
					}
					//description
					List<LocalizedStringType> descLocals = extObj.getDescription().getLocalizedString();
					if(descLocals != null && descLocals.size() > 0) {
						doc.setDescription(descLocals.get(0).getValue());
					}
					
					//slots
					List<SlotType1> slots = extObj.getSlot();
					for(SlotType1 slot : slots) {
						if(slot.getName().equalsIgnoreCase("repositoryUniqueId")) {
							ValueListType values = slot.getValueList();
							List<String> valueStrings = values.getValue();
							//TODO: just use the first one for now. how do we handle multiples?
							if(valueStrings.size() > 0) {
								docId.setRepositoryUniqueId(valueStrings.get(0));
							}
						} else if(slot.getName().equalsIgnoreCase("creationTime")) {
							ValueListType values = slot.getValueList();
							List<String> valueStrings = values.getValue();
							//TODO: just use the first one for now. how do we handle multiples?
							if(valueStrings.size() > 0) {
								doc.setCreationTime(valueStrings.get(0));
							}
						}  else if(slot.getName().equalsIgnoreCase("size")) {
							ValueListType values = slot.getValueList();
							List<String> valueStrings = values.getValue();
							//TODO: just use the first one for now. how do we handle multiples?
							if(valueStrings.size() > 0) {
								doc.setSize(valueStrings.get(0));
							}
						}
					}
					
					//classifications
					List<ClassificationType> classifications = extObj.getClassification();
					for(ClassificationType classification : classifications) {
						InternationalStringType classStr = classification.getName();
						if(classStr != null) {
							List<LocalizedStringType> classLocal = classStr.getLocalizedString();
							if(classLocal != null && classLocal.size() > 0 && !StringUtils.isEmpty(classLocal.get(0).getValue())) {
								switch(classification.getClassificationScheme()) {
								case "urn:uuid:41a5887f-8865-4c09-adf7-e362475b143a":
									//class code
									doc.setClassName(classLocal.get(0).getValue());
									break;
								case "urn:uuid:f4f85eac-e6cb-4883-b524-f2705394840f":
									//confidentiality
									doc.setConfidentiality(classLocal.get(0).getValue());
									break;
								case "urn:uuid:a09d5840-386c-46f2-b5ad-9c3699a4309d":
									//format
									doc.setFormat(classLocal.get(0).getValue());
									break;
								}		
							}
						}
					}
					
					//set the document unique id. 
					List<ExternalIdentifierType> extIds = extObj.getExternalIdentifier();
					for(ExternalIdentifierType extId : extIds) {
						InternationalStringType nameHolder = extId.getName();
						if(nameHolder != null) {
							List<LocalizedStringType> nameLocals = nameHolder.getLocalizedString();
							for(LocalizedStringType nameLocal : nameLocals) {
								if(nameLocal.getValue().equals("XDSDocumentEntry.uniqueId") &&
									StringUtils.isEmpty(docId.getDocumentUniqueId())) {
									docId.setDocumentUniqueId(extId.getValue());
								}
							}
						}
					}
					doc.setIdentifier(docId);
					documents.add(doc);	
				}
			}
		}
		return documents;
	}
	
	public DocumentRetrieve convertToDocumentSetRequest(RetrieveDocumentSetRequestType request){
		DocumentRetrieve dr = new DocumentRetrieve();
		dr.setDocRequests(request.getDocumentRequest());
		return dr;
	}

	public List<DocumentResponse> convertToDocumentSetResponse(RetrieveDocumentSetResponseType response) {
		return response.getDocumentResponse();
	}
}
