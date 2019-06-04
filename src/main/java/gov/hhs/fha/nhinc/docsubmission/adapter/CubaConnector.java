package gov.hhs.fha.nhinc.docsubmission.adapter;

import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.hhs.fha.nhinc.properties.PropertyAccessException;
import gov.hhs.fha.nhinc.properties.PropertyAccessor;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryError;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONObject;

public class CubaConnector {
	 private static final Logger LOG = LoggerFactory.getLogger(CubaConnector.class);
	 private static final String CUBA_PROPERTIES = "cuba.properties";
	   static String cuba_rest_token_url = "http://localhost:8080/app/rest/v2/oauth/token";
	    static String secret_key = "mettles";
	    static String client_id = "mettles";
	    static String username ="admin";
	    static String password = "admin";
	    private static final String auth = client_id + ":" + secret_key;
	    private static String authentication = Base64.getEncoder().encodeToString(auth.getBytes());
	    private static final Pattern pat = Pattern.compile(".*\"access_token\"\\s*:\\s*\"([^\"]+)\".*");
	     static String cuba_get_submission_id_url = "http://localhost:8080/app/rest/v2/queries/rioc_Submission/getSubmission?uniqueIdList=";
        static String cuba_error_update_url = "http://localhost:8080/app/rest/v2/entities/rioc_Error";
        static String cuba_status_change_update_url = "http://localhost:8080/app/rest/v2/entities/rioc_StatusChange";
        static String cuba_notification_update_url = "http://localhost:8080/app/rest/v2/entities/rioc_NotificationSlot";
        static String cuba_update_submission_id_url = "http://localhost:8080/app/rest/v2/entities/rioc_Submission/";
        static boolean has_errors = false;
	     public CubaConnector() {
	PropertyAccessor propertyAccessor = PropertyAccessor.getInstance();
	try {
	    Properties cubaProperties = propertyAccessor.getProperties(CUBA_PROPERTIES);
	    cuba_rest_token_url = cubaProperties.getProperty("cuba_rest_token_url");
	    secret_key = cubaProperties.getProperty("secret_key");
	    client_id = cubaProperties.getProperty("client_id");
	    username = cubaProperties.getProperty("cuba_username");
	    password = cubaProperties.getProperty("cuba_password");
	    authentication = Base64.getEncoder().encodeToString(auth.getBytes());
	    cuba_get_submission_id_url = cubaProperties.getProperty("cuba_get_submission_id_url");
	    cuba_error_update_url =  cubaProperties.getProperty("cuba_error_update_url");
	    cuba_status_change_update_url =  cubaProperties.getProperty("cuba_status_change_update_url");
	    cuba_notification_update_url =  cubaProperties.getProperty("cuba_notification_update_url");
	    cuba_update_submission_id_url =  cubaProperties.getProperty("cuba_update_submission_id_url");
	} catch (PropertyAccessException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }
    public String getResourceCredentials() {
        String content = "grant_type=password&username=" + username + "&password=" + password;
        BufferedReader reader = null;
        HttpURLConnection connection = null;
        String returnValue = "";
        try {
            URL url = new URL(cuba_rest_token_url);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            
            connection.setRequestProperty("Authorization", "Basic " + authentication);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Accept", "application/json");
            PrintStream os = new PrintStream(connection.getOutputStream());
            os.print(content);
            os.close();
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line = null;
            StringWriter out = new StringWriter(connection.getContentLength() > 0 ? connection.getContentLength() : 2048);
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }
            String response = out.toString();
            Matcher matcher = pat.matcher(response);
            if (matcher.matches() && matcher.groupCount() > 0) {
                returnValue = matcher.group(1);
            }
        } catch (Exception e) {
            System.out.println("Error : " + e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
            connection.disconnect();
        }
        return returnValue;
    }
    private Long getSubmssionId(String authToken,String UniqueID) {
        BufferedReader reader = null;
        HttpURLConnection connection = null;
        long subId = -1;
        String response = null;
        try {
        	String tempUrl = cuba_get_submission_id_url + UniqueID;
        	System.out.println(tempUrl);
            URL url = new URL(cuba_get_submission_id_url + UniqueID);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Authorization", "Bearer " + authToken);
            connection.setDoOutput(true);
            connection.setRequestMethod("GET");
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line = null;
            StringWriter out = new StringWriter(connection.getContentLength() > 0 ? connection.getContentLength() : 2048);
            
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }
             response = out.toString();
            System.out.println(response);
            if(response != null) {
          	  JSONObject myResponse = new JSONObject(response.substring(1, response.length()-1));
          	     System.out.println("result after Reading JSON Response");
          	     subId = myResponse.getLong("id");
          	     System.out.println("submission id- "+subId);
          }
        } catch (Exception e) {
         e.printStackTrace();
        }finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
            connection.disconnect();
        }
      return subId;
    }
    public void updateErrorMap(AcknowledgementInformation ackInfo,String AuthToken,Long SubmissionId)throws Exception {
    	   LOG.info("creating error messages");
   	    System.out.println("creating error messages");
   	 
   	    List<RegistryError> errors = ackInfo.getErrors();
   	    JSONObject tempJsonObj = new JSONObject();
   	    int information = 1;
   	    if (errors != null) {
   		   
   		for (Iterator iterator = errors.iterator(); iterator.hasNext();) {
   		    has_errors = true;
   		    RegistryError registryError = (RegistryError) iterator.next();
   		
   		   tempJsonObj.put("error",registryError.getCodeContext());
   		tempJsonObj.put("severity", registryError.getSeverity());
   		tempJsonObj.put("codeContext", registryError.getCodeContext());
   		tempJsonObj.put("errorCode", registryError.getErrorCode());
   		tempJsonObj.put("esmdTransactionId", ackInfo.getTransactionId());
   		tempJsonObj.put("splitInformation", information);
   		Map custmap = new LinkedHashMap(1);
   		custmap.put("id",SubmissionId);
   		tempJsonObj.put("submissionId", custmap);
   		    {
   		     BufferedReader reader = null;
   	        HttpURLConnection connection = null;
   	        String returnValue = "";
   	        try {
   	            URL url = new URL(cuba_error_update_url);
   	            connection = (HttpURLConnection) url.openConnection();
   	            connection.setRequestMethod("POST");
   	            connection.setDoOutput(true);
   	            
   	            connection.setRequestProperty("Authorization", "Bearer " + AuthToken);
   	            connection.setRequestProperty("Content-Type","application/json");
   	           
   	            PrintStream os = new PrintStream(connection.getOutputStream());
   	            os.print(tempJsonObj.toString());
   	            os.close();
   	            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
   	            String line = null;
   	            StringWriter out = new StringWriter(connection.getContentLength() > 0 ? connection.getContentLength() : 2048);
   	            while ((line = reader.readLine()) != null) {
   	                out.append(line);
   	            }
   	            String response = out.toString();
   	            System.out.println("response string for cuba error update"+response);
   	        } catch (Exception e) {
   	            System.out.println("Error : " + e.getMessage());
   	        } finally {
   	            if (reader != null) {
   	                try {
   	                    reader.close();
   	                } catch (IOException e) {
   	                }
   	            }
   	            connection.disconnect();
   	        }	
   		    }
   		  
   		    System.out.println("Inserted the error message ");
   		    
   		}
   	    }
    }
 public void updateNotificationMap(AcknowledgementInformation ackInfo,String AuthToken,Long SubmissionId)throws Exception {
	 HashMap<String,String> notifications = ackInfo.getNotificationDetails();
	 int information = 1;
	   JSONObject tempJsonObj = new JSONObject();
	   if(notifications != null) {
	    Set<String> keys = notifications.keySet();
	    for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
		String key = (String) iterator.next();
		String value = notifications.get(key);
		tempJsonObj.put("field", key);
		tempJsonObj.put("value", value);
		tempJsonObj.put("splitInformation", information);
		Map custmap = new LinkedHashMap(1);
   		custmap.put("id",SubmissionId);
   		tempJsonObj.put("submissionId", custmap);
	
		
	  
		
		{
			 BufferedReader reader = null;
		        HttpURLConnection connection = null;
		        String returnValue = "";
		        try {
		            URL url = new URL(cuba_notification_update_url);
		            connection = (HttpURLConnection) url.openConnection();
		            connection.setRequestMethod("POST");
		            connection.setDoOutput(true);
		            
		            connection.setRequestProperty("Authorization", "Bearer " + AuthToken);
		            connection.setRequestProperty("Content-Type","application/json");
		           
		            PrintStream os = new PrintStream(connection.getOutputStream());
		            os.print(tempJsonObj.toString());
		            os.close();
		            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		            String line = null;
		            StringWriter out = new StringWriter(connection.getContentLength() > 0 ? connection.getContentLength() : 2048);
		            while ((line = reader.readLine()) != null) {
		                out.append(line);
		            }
		            String response = out.toString();
		            System.out.println("response string for cuba notification slot update"+response);
		        } catch (Exception e) {
		            System.out.println("Error : " + e.getMessage());
		        } finally {
		            if (reader != null) {
		                try {
		                    reader.close();
		                } catch (IOException e) {
		                }
		            }
		            connection.disconnect();
	    }
	    
	    
		}
		  System.out.println("Inserted the notication message " );
	    
	   }
	   }
	    
	
    }
 public void updateStatusChange(AcknowledgementInformation ackInfo,String AuthToken,Long SubmissionId)throws Exception {
	    HashMap<String, String> changeMap = new HashMap<String,String>();
	    JSONObject tempJsonObj = new JSONObject();
	    tempJsonObj.put("status", ackInfo.getNotificationType());
	    if (ackInfo.isStatus()) {
	    	 tempJsonObj.put("result", "success");
		
		    }else {
		   	 tempJsonObj.put("result", "failure");
		    }
	    tempJsonObj.put("status", ackInfo.getNotificationType());
	    tempJsonObj.put("esmdTransactionId", ackInfo.getTransactionId());
	    tempJsonObj.put("splitInformation", "1");
		Map custmap = new LinkedHashMap(1);
   		custmap.put("id",SubmissionId);
   		tempJsonObj.put("submissionId", custmap);
    
	    {
		     BufferedReader reader = null;
	        HttpURLConnection connection = null;
	        String returnValue = "";
	        try {
	            URL url = new URL(cuba_status_change_update_url);
	            connection = (HttpURLConnection) url.openConnection();
	            connection.setRequestMethod("POST");
	            connection.setDoOutput(true);
	            
	            connection.setRequestProperty("Authorization", "Bearer " + AuthToken);
	            connection.setRequestProperty("Content-Type","application/json");
	           
	            PrintStream os = new PrintStream(connection.getOutputStream());
	            os.print(tempJsonObj.toString());
	            os.close();
	            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
	            String line = null;
	            StringWriter out = new StringWriter(connection.getContentLength() > 0 ? connection.getContentLength() : 2048);
	            while ((line = reader.readLine()) != null) {
	                out.append(line);
	            }
	            String response = out.toString();
	            System.out.println("response string for cuba notification slot update"+response);
	        } catch (Exception e) {
	            System.out.println("Error : " + e.getMessage());
	        } finally {
	            if (reader != null) {
	                try {
	                    reader.close();
	                } catch (IOException e) {
	                }
	            }
	            connection.disconnect();
	        }	
	    }

 }
 public void updateSubmissionEntity(AcknowledgementInformation ackInfo,String AuthToken,Long SubmissionId) throws Exception {
	    HashMap<String,String> updateMap = new HashMap<String,String>();
	    JSONObject tempJsonObj = new JSONObject();
	    tempJsonObj.put("stage", ackInfo.getNotificationType());
	    tempJsonObj.put("esmdTransactionId", ackInfo.getTransactionId());
	  
       
	    LOG.info("=============Marking the stage as " + ackInfo.getNotificationType());
	    System.out.println("=============Marking the stage as " + ackInfo.getNotificationType());
	    if (has_errors) {
		    tempJsonObj.put("status",  "error");
		    tempJsonObj.put("message", "There were errors in the submission");
		    tempJsonObj.put("hasMessage", "True");
		
	    }
	    
	    {
		     BufferedReader reader = null;
	        HttpURLConnection connection = null;
	        String returnValue = "";
	        try {
	            URL url = new URL(cuba_update_submission_id_url+SubmissionId);
	            connection = (HttpURLConnection) url.openConnection();
	            connection.setRequestMethod("PUT");
	            connection.setDoOutput(true);
	            
	            connection.setRequestProperty("Authorization", "Bearer " + AuthToken);
	            connection.setRequestProperty("Content-Type","application/json");
	           
	            PrintStream os = new PrintStream(connection.getOutputStream());
	            os.print(tempJsonObj.toString());
	            os.close();
	            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
	            String line = null;
	            StringWriter out = new StringWriter(connection.getContentLength() > 0 ? connection.getContentLength() : 2048);
	            while ((line = reader.readLine()) != null) {
	                out.append(line);
	            }
	            String response = out.toString();
	            System.out.println("response string for cuba notification slot update"+response);
	        } catch (Exception e) {
	            System.out.println("Error : " + e.getMessage());
	        } finally {
	            if (reader != null) {
	                try {
	                    reader.close();
	                } catch (IOException e) {
	                }
	            }
	            connection.disconnect();
	        }	
	    }
 }
    public void updateCubaEntities(AcknowledgementInformation ackInfo,String AuthToken) throws Exception {
    	Long retSubID = getSubmssionId(AuthToken,ackInfo.getUniqueId());
    	if(retSubID != -1) {
    		updateStatusChange(ackInfo, AuthToken,retSubID);
    		updateNotificationMap(ackInfo, AuthToken,retSubID);
    		updateErrorMap(ackInfo, AuthToken,retSubID);
    		updateSubmissionEntity(ackInfo, AuthToken,retSubID);
    	}else {
    		System.out.println("No Valid Submission Id Found");
    	}
    }
}
