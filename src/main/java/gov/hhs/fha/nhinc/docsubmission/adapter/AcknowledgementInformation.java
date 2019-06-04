package gov.hhs.fha.nhinc.docsubmission.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryError;

public class AcknowledgementInformation {
	private String esmdClaimId;
	private String esmdCaseId;
	private String notificationType;
	private boolean status;
	private String UniqueId;
	private String messageId;
	private List<RegistryError> errors;
    private String transactionId;
    private HashMap<String, String>notificationDetails;
	
    public AcknowledgementInformation() {
	notificationDetails = new HashMap<>();
    }
    
    public void addNotificationDetails(String key, String value) {
	notificationDetails.put(key, value);
    }
    
    public HashMap<String, String> getNotificationDetails() {
	return notificationDetails;
    }
    
    
    public String getTransactionId() {
	return transactionId;
    }
    public void setTransactionId(String transactionId) {
	this.transactionId = transactionId;
    }
	
	public String getEsmdClaimId() {
		return esmdClaimId;
	}
	public void setEsmdClaimId(String esmdClaimId) {
		this.esmdClaimId = esmdClaimId;
	}
	public String getEsmdCaseId() {
		return esmdCaseId;
	}
	public void setEsmdCaseId(String esmdCaseId) {
		this.esmdCaseId = esmdCaseId;
	}
	public String getNotificationType() {
		return notificationType;
	}
	public void setNotificationType(String notificationType) {
		this.notificationType = notificationType;
	}
	public boolean isStatus() {
		return status;
	}
	public void setStatus(boolean status) {
		this.status = status;
	}
	public String getUniqueId() {
		return UniqueId;
	}
	public void setUniqueId(String uniqueId) {
		UniqueId = uniqueId;
	}
	public String getMessageId() {
		return messageId;
	}
	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}
	public List<RegistryError> getErrors() {
		return errors;
	}
	public void setErrors(List<RegistryError> errors) {
		this.errors = errors;
	}
	
	

}
