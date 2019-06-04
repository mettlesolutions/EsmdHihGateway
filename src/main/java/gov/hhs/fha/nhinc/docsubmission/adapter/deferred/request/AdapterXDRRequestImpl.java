/*
 * Copyright (c) 2009-2018, United States Government, as represented by the Secretary of Health and Human Services.
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
package gov.hhs.fha.nhinc.docsubmission.adapter.deferred.request;

import gov.hhs.fha.nhinc.common.nhinccommon.AssertionType;
import gov.hhs.fha.nhinc.common.nhinccommon.HomeCommunityType;
import gov.hhs.fha.nhinc.common.nhinccommon.NhinTargetCommunitiesType;
import gov.hhs.fha.nhinc.common.nhinccommon.NhinTargetCommunityType;
import gov.hhs.fha.nhinc.common.nhinccommon.UrlInfoType;
import gov.hhs.fha.nhinc.common.nhinccommonadapter.AdapterProvideAndRegisterDocumentSetRequestType;
import gov.hhs.fha.nhinc.docsubmission.DocSubmissionUtils;
import gov.hhs.fha.nhinc.docsubmission.outbound.deferred.request.OutboundDocSubmissionDeferredRequest;
import gov.hhs.fha.nhinc.docsubmission.outbound.deferred.request.StandardOutboundDocSubmissionDeferredRequest;
import gov.hhs.fha.nhinc.messaging.server.BaseService;
import gov.hhs.fha.nhinc.nhinclib.NhincConstants.UDDI_SPEC_VERSION;
import gov.hhs.healthit.nhin.XDRAcknowledgementType;
import ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType;
import javax.xml.ws.WebServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dunnek
 */
public class AdapterXDRRequestImpl extends BaseService {
    private static final Logger LOG = LoggerFactory.getLogger(AdapterXDRRequestImpl.class);
    private OutboundDocSubmissionDeferredRequest outboundDocSubmissionRequest;
    
    AdapterXDRRequestImpl() {
        this.outboundDocSubmissionRequest = new StandardOutboundDocSubmissionDeferredRequest();
    }

    public XDRAcknowledgementType provideAndRegisterDocumentSetBRequest(
            AdapterProvideAndRegisterDocumentSetRequestType body, WebServiceContext context) {
        LOG.info("Begin AdapterXDRRequestImpl.provideAndRegisterDocumentSetBRequest(unsecure)");
        XDRAcknowledgementType response = null;

        ProvideAndRegisterDocumentSetRequestType request = null;
        AssertionType assertion = null;
        if (body != null) {
            request = body.getProvideAndRegisterDocumentSetRequest();
            assertion = body.getAssertion();
        }
        assertion = getAssertion(context, assertion);

        //response = provideAndRegisterDocumentSetBRequest(request, assertion);
        NhinTargetCommunitiesType ntct = new NhinTargetCommunitiesType();
        NhinTargetCommunityType tc = new NhinTargetCommunityType();
        
        HomeCommunityType hc = assertion.getHomeCommunity();
        /*hc.setName("2.16.840.1.113883.3.6037.2");
        hc.setHomeCommunityId("urn:oid:2.16.840.1.113883.3.6037.2");
        hc.setDescription("Home community");*/
        tc.setHomeCommunity(hc);
        ntct.getNhinTargetCommunity().add(tc);
        LOG.info("The hc name is " + hc.getName());
        boolean run = true;
        if (hc.getName().equals("===dummy===")) {
        	run = false;
        	LOG.info("=============Dummy request sent to wake up mysql==============");
        }
        if (run) {
        response = provideAndRegisterDocumentSetBAsyncRequest(request,assertion,
                ntct, null);
        }
        LOG.info("End AdapterXDRRequestImpl.provideAndRegisterDocumentSetBRequest(unsecure)");
        return response;
    }

    public XDRAcknowledgementType provideAndRegisterDocumentSetBRequest(
            gov.hhs.fha.nhinc.common.nhinccommonadapter.AdapterProvideAndRegisterDocumentSetSecuredRequestType body,
            WebServiceContext context) {
        LOG.info("Begin AdapterXDRRequestImpl.provideAndRegisterDocumentSetBRequest(secure)");
        XDRAcknowledgementType response;

        ProvideAndRegisterDocumentSetRequestType request = null;
        AssertionType assertion = null;
        if (body != null) {
            request = body.getProvideAndRegisterDocumentSetRequest();
            LOG.info("got the request");
        }
        assertion = getAssertion(context, assertion);
        

        response = new XDRAcknowledgementType();
        LOG.info("End AdapterXDRRequestImpl.provideAndRegisterDocumentSetBRequest(secure)");
        return response;
    }
    
    

    protected XDRAcknowledgementType provideAndRegisterDocumentSetBRequest(
            ProvideAndRegisterDocumentSetRequestType request, AssertionType assertion) {
        return new AdapterDocSubmissionDeferredRequestOrchImpl().provideAndRegisterDocumentSetBRequest(request,
                assertion);
    }
    
    
    public gov.hhs.healthit.nhin.XDRAcknowledgementType provideAndRegisterDocumentSetBAsyncRequest(
            gov.hhs.fha.nhinc.common.nhinccommonentity.RespondingGatewayProvideAndRegisterDocumentSetRequestType provideAndRegisterAsyncReqRequest,
            WebServiceContext context) {
    	LOG.info("================Logging the deferred response 1==================");

        AssertionType assertion = getAssertion(context, provideAndRegisterAsyncReqRequest.getAssertion());

        return provideAndRegisterDocumentSetBAsyncRequest(
                provideAndRegisterAsyncReqRequest.getProvideAndRegisterDocumentSetRequest(), assertion,
                provideAndRegisterAsyncReqRequest.getNhinTargetCommunities(),
                provideAndRegisterAsyncReqRequest.getUrl());

    }
    /**
     * Route service to either standard or passthrough depend on configuration
     * @param request Document Submission request
     * @param assertion DS assertion
     * @param targets DS gateway targets
     * @param urlInfo DS gateway target urls
     * @return XDRAcknowledgementType
     */
    private XDRAcknowledgementType provideAndRegisterDocumentSetBAsyncRequest(
            ProvideAndRegisterDocumentSetRequestType request, AssertionType assertion,
            NhinTargetCommunitiesType targets, UrlInfoType urlInfo) {
        XDRAcknowledgementType response = null;
        try {
        	LOG.info("================Logging the deferred response==================");
            DocSubmissionUtils.getInstance().setTargetCommunitiesVersion(targets, UDDI_SPEC_VERSION.SPEC_2_0);
            LOG.info("================before checking response==================");
            if (outboundDocSubmissionRequest == null) {
            	LOG.info("NULL outbound request");
            }
            response = outboundDocSubmissionRequest.provideAndRegisterDocumentSetBAsyncRequest(request, assertion,
                    targets, urlInfo);
        } catch (Exception e) {
            LOG.error("Failed to send request to Nwhin.", e);
        }
        return response;
    }
}
