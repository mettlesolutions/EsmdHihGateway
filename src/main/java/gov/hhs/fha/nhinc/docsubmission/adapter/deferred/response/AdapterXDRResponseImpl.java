/*
 * Copyright (c) 2009-2019, United States Government, as represented by the Secretary of Health and Human Services.
 * All rights reserved.
 *  
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above
 *       copyright notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the documentation
 *       and/or other materials provided with the distribution.
 *     * Neither the name of the United States Government nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE UNITED STATES GOVERNMENT BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package gov.hhs.fha.nhinc.docsubmission.adapter.deferred.response;

import gov.hhs.fha.nhinc.async.AsyncMessageIdExtractor;
import gov.hhs.fha.nhinc.common.nhinccommon.AssertionType;
import gov.hhs.fha.nhinc.common.nhinccommonadapter.AdapterRegistryResponseType;
import gov.hhs.fha.nhinc.cxf.extraction.SAML2AssertionExtractor;
import gov.hhs.fha.nhinc.nhinclib.NullChecker;
import gov.hhs.healthit.nhin.XDRAcknowledgementType;
import gov.hhs.fha.nhinc.docsubmission.adapter.OdooConnector;
import gov.hhs.fha.nhinc.docsubmission.adapter.AcknowledgementInformation;
import gov.hhs.fha.nhinc.nhinclib.NhincConstants;
import java.util.List;
import javax.xml.ws.WebServiceContext;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryResponseType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceContext;
import java.io.IOException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


import oasis.names.tc.ebxml_regrep.xsd.rim._3.SlotListType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.SlotType1;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ValueListType;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryError;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryErrorList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dunnek
 */
public class AdapterXDRResponseImpl {
    private static final Logger LOG = LoggerFactory.getLogger(AdapterXDRResponseImpl.class);
    private AsyncMessageIdExtractor extractor = new AsyncMessageIdExtractor();

    public XDRAcknowledgementType provideAndRegisterDocumentSetBResponse(AdapterRegistryResponseType body,
            WebServiceContext context) {
        LOG.debug("Begin AdapterXDRResponseImpl.provideAndRegisterDocumentSetBResponse(unsecured)");
        XDRAcknowledgementType response;

        RegistryResponseType regResponse = null;
        AssertionType assertion = null;
        if (body != null) {
            regResponse = body.getRegistryResponse();
            assertion = body.getAssertion();
        }
        assertion = getAssertion(context, assertion);
        response = provideAndRegisterDocumentSetBResponse(regResponse, assertion);

        LOG.debug("End AdapterXDRResponseImpl.provideAndRegisterDocumentSetBResponse(unsecured)");
        return response;
    }

    public XDRAcknowledgementType provideAndRegisterDocumentSetBResponse(RegistryResponseType body,
            WebServiceContext context) {
        LOG.debug("Begin AdapterXDRResponseImpl.provideAndRegisterDocumentSetBResponse(secured)");
        XDRAcknowledgementType response;

        AssertionType assertion = null;
        assertion = getAssertion(context, assertion);
        response = provideAndRegisterDocumentSetBResponse(body, assertion);

        LOG.debug("End AdapterXDRResponseImpl.provideAndRegisterDocumentSetBResponse(secured)");
        return response;
    }

    private AssertionType getAssertion(WebServiceContext context, AssertionType oAssertionIn) {
        AssertionType assertion;
        if (oAssertionIn == null) {
            assertion = SAML2AssertionExtractor.getInstance().extractSamlAssertion(context);
        } else {
            assertion = oAssertionIn;
        }

        // Extract the message id value from the WS-Addressing Header and place it in the Assertion Class
        if (assertion != null) {
            assertion.setMessageId(extractor.getOrCreateAsyncMessageId(context));
            List<String> relatesToList = extractor.getAsyncRelatesTo(context);
            if (NullChecker.isNotNullish(relatesToList)) {
                assertion.getRelatesToList().add(relatesToList.get(0));
            }
        }

        return assertion;
    }

    protected XDRAcknowledgementType provideAndRegisterDocumentSetBResponse(RegistryResponseType body,
            AssertionType assertion) {
    
    	
        LOG.info("Begin AdapterXDRResponseImpl.provideAndRegisterDocumentSetBResponse");
        System.out.println("Entered into AdapterXDRResponseImpl.provideAndRegisterDocumentSetBResponse");
        String uid = assertion.getSamlAuthzDecisionStatement().getEvidence().getAssertion().getId();
        
        
        printAssertion(assertion);
        
        ArrayList<String> vals = new ArrayList<String>();
        vals.add("=====adding id from assertion========="+uid);
	vals.add("=====done printing assertion=========");
	vals.add("=====body data is=========");
	String request_id = body.getRequestId();
    
	vals.add("The request is " + request_id);
	String status = body.getStatus();
	vals.add("The Status is " + status);
	oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryErrorList list = body.getRegistryErrorList();
	
	
	if (list != null) {
	    List<RegistryError> errors =  list.getRegistryError();
	    String highestSeverity = list.getHighestSeverity();
	    vals.add("The severity is " + highestSeverity);
	    
	    for (Iterator iterator = errors.iterator(); iterator.hasNext();) {
		RegistryError registryError = (RegistryError) iterator.next();
		vals.add("The code context is" + registryError.getCodeContext());
		vals.add("The error code is" + registryError.getErrorCode());
		vals.add("The location is" + registryError.getLocation());
		vals.add("The severity  is" + registryError.getSeverity());
		vals.add("The error value is" + registryError.getValue());
	    }
	}
	
	vals.add("=====slot data is=========");
	SlotListType slotType = body.getResponseSlotList();
	if (slotType != null) {
	    List<SlotType1> slots = slotType.getSlot();
	    for (Iterator iterator = slots.iterator(); iterator.hasNext();) {
		SlotType1 slotType1 = (SlotType1) iterator.next();
		vals.add("The slot name is" + slotType1.getName());
		vals.add("The slot type is" + slotType1.getSlotType());
		ValueListType vlt = slotType1.getValueList();
		List<String> values = vlt.getValue();
		for (Iterator iterator2 = values.iterator(); iterator2.hasNext();) {
		    String value = (String) iterator2.next();
		    vals.add("The value in values is" + value);
		    
		}
		
	    }
	}
	/*try {
	    FileOutputStream fs = new FileOutputStream("/home/ubuntu/out.txt");
	    String output = "";
	    for (Iterator iterator = vals.iterator(); iterator.hasNext();) {
		String val = (String) iterator.next();
		output = output + val + "\n" ;
		
	    }
	    fs.write(output.getBytes());
	    fs.close();
	} catch(IOException e) {
	    e.printStackTrace();
	}*/
	processBody(body,assertion);
	
	
	   RegistryResponseType regResp = new RegistryResponseType();
       regResp.setStatus(NhincConstants.XDR_RESP_ACK_STATUS_MSG);
	XDRAcknowledgementType response = new XDRAcknowledgementType();
	response.setMessage(regResp);
     
        //response.setMessage(regResp);

        return response;
      //  return new AdapterDocSubmissionDeferredResponseOrchImpl().provideAndRegisterDocumentSetBResponse(regResponse,
        //        assertion);
    }
    
    protected void processBody(RegistryResponseType regResponse,AssertionType assertion) {
	String uid = assertion.getSamlAuthzDecisionStatement().getEvidence().getAssertion().getId();
	boolean status = false;
	AcknowledgementInformation ackInfo = new AcknowledgementInformation();
	LOG.info("request is" + regResponse.getRequestId());
	System.out.println("request is" + regResponse.getRequestId());
	LOG.info("status is" + regResponse.getStatus());
	System.out.println("status is" + regResponse.getStatus());
	String notificationType = regResponse.getRequestId();
	if (regResponse.getStatus().equals("urn:oasis:names:tc:ebxml-regrep:ResponseStatusType:Success")){
	    status = true;
	}
	ackInfo.addNotificationDetails("Request Id", regResponse.getRequestId());
	ackInfo.addNotificationDetails("status", regResponse.getStatus());
	
	String esmdClaimId = "";
	String esmdCaseId = "";
	String esMDTransactionId = "";
	SlotListType responseSlotList = regResponse.getResponseSlotList();
	System.out.println("regResponse.getResponseSlotList()");
	if (responseSlotList != null) {
	    List<SlotType1> responseSlots = responseSlotList.getSlot();
	    LOG.info(" Processing response " );
	    System.out.println(" Processing response " );
	    for (Iterator iterator = responseSlots.iterator(); iterator.hasNext();) {
		
		
		String slotValue;
		SlotType1 slot = (SlotType1) iterator.next();
		String slotType = slot.getSlotType();
		String slotName = slot.getName();
		LOG.info("Slot Name=====" + slotName + " Slot Type==== " + slotType);
		 System.out.println("Slot Name=====" + slotName + " Slot Type==== " + slotType);
		
		List<String> vals = slot.getValueList().getValue();
		for (Iterator iterator2 = vals.iterator(); iterator2.hasNext();) {
		    String value = (String) iterator2.next();
		    ackInfo.addNotificationDetails(slotName, value);
		    if (slotName.equals("ClaimId")) {
			LOG.info("setting claim id");
			 System.out.println("setting claim id");
			esmdClaimId = value;
		    }
		    if (slotName.equals("CaseId")) {
LOG.info("setting case id");
System.out.println("setting case id");
esmdCaseId = value;
}
if (slotName.equals("esMDTransactionId")) {
LOG.info("esMDTransactionId");
System.out.println("esMDTransactionId");
esMDTransactionId = value;
}



LOG.info(" Slot value==== " + value);

}

}
    }
	 System.out.println(" Processing errors " );
    LOG.info(" Processing errors " );
    
    
    try {
    OdooConnector o = new OdooConnector();
    
    ackInfo.setEsmdClaimId(esmdClaimId);
    ackInfo.setEsmdCaseId(esmdCaseId);
    ackInfo.setTransactionId(esMDTransactionId);
    System.out.println(" Processing setUniqueId " );
    ackInfo.setUniqueId(uid.substring(1));
    ackInfo.setNotificationType(notificationType);
    System.out.println(" Processing getRegistryErrorList " );
    RegistryErrorList errorList = regResponse.getRegistryErrorList();
        if (errorList != null) {
        List<RegistryError> errors = errorList.getRegistryError();
        ackInfo.setErrors(errors);
        
        }
        ackInfo.setStatus(status);
        System.out.println(" Processing call " );
    o.call(ackInfo);
    } catch(Exception e) {
    LOG.error("error is", e);
    }
    }
    

    public void printAssertion(AssertionType request) {
    
    
JAXBContext jc;
final JAXBElement<AssertionType> requestElement
= new JAXBElement<AssertionType>(
    new QName("root-element-name"), 
    AssertionType.class, 
    request);
try {
jc = JAXBContext.newInstance(AssertionType.class);

Marshaller marshaller = jc.createMarshaller();
marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
FileOutputStream fs = new FileOutputStream("assertion.xml");
marshaller.marshal(requestElement, fs);
fs.close();
} catch (JAXBException e) {
// TODO Auto-generated catch block
e.printStackTrace();
} catch (IOException e1) {
// TODO Auto-generated catch block
e1.printStackTrace();
}

    
}
}
