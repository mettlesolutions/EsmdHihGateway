package gov.hhs.fha.nhinc.docsubmission.adapter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.hhs.fha.nhinc.properties.PropertyAccessException;
import gov.hhs.fha.nhinc.properties.PropertyAccessor;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryError;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

public class OdooConnector {
    private static final Logger LOG = LoggerFactory.getLogger(OdooConnector.class);
    private static final String ODOO_PROPERTIES = "odoo.properties";

    static String url = "http://esmd.mettles.com:8069";
    static String db = "esmd_gateway";
    static String username ="admin";
    static String password = "MettleSolutions123";
    
    public OdooConnector() {
	PropertyAccessor propertyAccessor = PropertyAccessor.getInstance();
	try {
	    Properties odooProperties = propertyAccessor.getProperties(ODOO_PROPERTIES);
	    url = odooProperties.getProperty("odoo_url");
	    db = odooProperties.getProperty("odoo_db");
	    username = odooProperties.getProperty("odoo_username");
	    password = odooProperties.getProperty("odoo_password");
	    
	} catch (PropertyAccessException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }
       
    public void call(AcknowledgementInformation ackInfo) throws Exception {
        //Thread.sleep(60000);
        System.out.println(" Processing XmlRpcClient " );

        final XmlRpcClient authClient = new XmlRpcClient();
        final XmlRpcClientConfigImpl authStartConfig = new XmlRpcClientConfigImpl();
        authStartConfig.setServerURL(
                new URL(String.format("%s/xmlrpc/2/common", url)));
        
        List configList = new ArrayList();
        Map paramMap = new HashMap();
        
        configList.add(db);
        configList.add(username);
        configList.add(password);
        configList.add(paramMap);
        
        System.out.println(" Processing authClient " );
        Object os = authClient.execute(authStartConfig, "authenticate", configList);
        
        LOG.info("Logging =========" + os.toString());
       System.out.println("Logging =========" + os.toString());
        int uid = 1;

        final XmlRpcClient objClient = new XmlRpcClient();
        final XmlRpcClientConfigImpl objStartConfig = new XmlRpcClientConfigImpl();
        objStartConfig.setServerURL(
				    new URL(String.format("%s/xmlrpc/2/object", url)));
        objClient.setConfig(objStartConfig);
        LOG.info("searching--------------" + ackInfo.getUniqueId() );
        System.out.println("searching--------------" + ackInfo.getUniqueId() );
        
        Object[] lista = (Object[]) objClient.execute("execute_kw",
						      Arrays.asList(
								    db,
								    uid,
								    password,
								    "esmd_gateway.split_maps",
								    "search_read",
								    Arrays.asList(Arrays.asList(Arrays.asList("name", "=", ackInfo.getUniqueId()))),
								    new HashMap() {{  put("limit",1); }})                                                                
						      );
	
	if (lista.length == 0) {
	    LOG.info("There are no records matching");
	    System.out.println("There are no records matching");
	}
	
	int matching_id = 0;
	int information = 1;
	

        for (int i = 0; i < lista.length; i++) {
	    HashMap<String,Object> map = (HashMap<String,Object>)lista[i];
	    Object[] o = (Object[])map.get("submission_id");
	    for (int j = 0; j < 1; j++) {
		LOG.info("found---------" + o[j].getClass().getSimpleName());
		System.out.println("found---------" + o[j].getClass().getSimpleName());
		
		matching_id = ((Integer)(o[j])).intValue();
	    }
	    
	    
	    information = ((Integer)map.get("information")).intValue();
	    LOG.info("=======information=======" + information);
	    System.out.println("=======information=======" + information);
	}
	if (matching_id != 0) {
       
       
	    LOG.info("creating error messages");
	    System.out.println("creating error messages");
	    boolean has_errors = false;
	    List<RegistryError> errors = ackInfo.getErrors();
	    if (errors != null) {
		   
		for (Iterator iterator = errors.iterator(); iterator.hasNext();) {
		    has_errors = true;
		    RegistryError registryError = (RegistryError) iterator.next();
		    HashMap<String, String> errorMap = new HashMap<String,String>();
		    errorMap.put("severity",registryError.getSeverity());
		    errorMap.put("error_code",registryError.getErrorCode());
		    errorMap.put("code_context",registryError.getCodeContext());
		    errorMap.put("name",registryError.getCodeContext());
		    errorMap.put("split_information",""+information);
		    errorMap.put("submission_id",new Integer(matching_id).toString());
		    errorMap.put("esmd_transaction_id", ackInfo.getTransactionId());
		    final Integer id = (Integer)objClient.execute("execute_kw", Arrays.asList(
											      db, uid, password,
											      "esmd_gateway.error", "create",
											      Arrays.asList(errorMap)
											      ));
		    LOG.info("Inserted the error message " + id.intValue());
		    System.out.println("Inserted the error message " + id.intValue());
		    
		}
		   
	    }
	    HashMap<String, String> changeMap = new HashMap<String,String>();
	    changeMap.put("name", ackInfo.getNotificationType());
	    changeMap.put("submission_id", new Integer(matching_id).toString());
	    changeMap.put("split_information",""+information);
	    changeMap.put("result","failure");
	    changeMap.put("esmd_transaction_id", ackInfo.getTransactionId());
	    if (ackInfo.isStatus()) {
		changeMap.put("result","success");
	    }
       
	    final Integer id = (Integer)objClient.execute("execute_kw", Arrays.asList(
										      db, uid, password,
										      "esmd_gateway.status.change", "create",
										      Arrays.asList(changeMap)
										      ));
	    LOG.info("Inserted the status message " + id.intValue());
	    System.out.println("Inserted the status message " + id.intValue());
	    HashMap<String, String> notificationMap = new HashMap<String,String>();
	    HashMap<String,String> notifications = ackInfo.getNotificationDetails();
	    Set<String> keys = notifications.keySet();
	    for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
		String key = (String) iterator.next();
		String value = notifications.get(key);
		notificationMap.put("name", key);
		notificationMap.put("value", value);
		notificationMap.put("submission_id", new Integer(matching_id).toString());
		notificationMap.put("split_information",""+information);
		final Integer notification_id = (Integer)objClient.execute("execute_kw", Arrays.asList(
												       db, uid, password,
												       "esmd_gateway.notification_slot", "create",
												       Arrays.asList(notificationMap)
												       ));
		LOG.info("Inserted the notication message " + notification_id.intValue());
	    System.out.println("Inserted the notication message " + notification_id.intValue());
		notificationMap = new HashMap<String,String>();
		
	    }
	    HashMap<String,String> updateMap = new HashMap<String,String>();
	    updateMap.put("stage", ackInfo.getNotificationType() );
	    updateMap.put("esmd_transaction_id", ackInfo.getTransactionId());
       
	    LOG.info("=============Marking the stage as " + ackInfo.getNotificationType());
	    System.out.println("=============Marking the stage as " + ackInfo.getNotificationType());
	    if (has_errors) {
		updateMap.put("status", "error" );
		updateMap.put("message", "There were errors in the submission");
		updateMap.put("has_message", "True");
	    }
       
        
	    objClient.execute("execute_kw", Arrays.asList(
							  db, uid, password,
							  "esmd_gateway.submission", "write",
							  Arrays.asList(
									Arrays.asList(matching_id),
									        updateMap
									)
							  ));
	} else {
	    LOG.info("The not found  unique id is " + ackInfo.getUniqueId());
	    System.out.println("The not found  unique id is " + ackInfo.getUniqueId());
	    CubaConnector cubloc = new CubaConnector();
	    String AuthHeaderToken = cubloc.getResourceCredentials();
	    System.out.println("get resource credentials in cuba"+cubloc.getResourceCredentials());
	    cubloc.updateCubaEntities(ackInfo,AuthHeaderToken);
	 //   cubloc.getResourceCredentials();
	    System.out.println("get resource credentials in cuba"+cubloc.getResourceCredentials());
	}
       
      
    }
    
    public void createEmdr(HashMap<String,String> inputs, ArrayList<String> files_base64) throws Exception {
	final XmlRpcClient authClient = new XmlRpcClient();
        final XmlRpcClientConfigImpl authStartConfig = new XmlRpcClientConfigImpl();
        authStartConfig.setServerURL(
				     new URL(String.format("%s/xmlrpc/2/common", url)));
        
        List configList = new ArrayList();
        Map paramMap = new HashMap();
        
        configList.add(db);
        configList.add(username);
        configList.add(password);
        configList.add(paramMap);
        
        
        Object os = authClient.execute(authStartConfig, "authenticate", configList);
        LOG.info("Logging =========" + os.toString());
        int uid = 1;

        final XmlRpcClient objClient = new XmlRpcClient();
        final XmlRpcClientConfigImpl objStartConfig = new XmlRpcClientConfigImpl();
        objStartConfig.setServerURL(
				    new URL(String.format("%s/xmlrpc/2/object", url)));
        objClient.setConfig(objStartConfig);
        
        final Integer id = (Integer)objClient.execute("execute_kw", Arrays.asList(
										  db, uid, password,
										  "esmd_gateway.emdr", "create",
										  Arrays.asList(inputs)
										  ));
        
        
    }
}
