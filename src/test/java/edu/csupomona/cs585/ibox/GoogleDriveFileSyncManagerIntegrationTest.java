package edu.csupomona.cs585.ibox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files.List;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import edu.csupomona.cs585.ibox.sync.GoogleDriveFileSyncManager;

public class GoogleDriveFileSyncManagerIntegrationTest {

	//Google Drive API credentials
	private static String CLIENT_ID = "644738993041-sk0b1dnepf92j5kie8lu0uu0p52vfhlb.apps.googleusercontent.com";
	private static String CLIENT_SECRET = "nhYLS13FP6FS0mXJDYj-85mE";
	private static String REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob";

	//Create a global-scope Google API client for current authorized session
	static Drive googleDriveClient;

	//Create a global-scope get file sync manager
	static GoogleDriveFileSyncManager testGoogleDriveFileSyncManager;
	
	public static void initializeGoogleDriveServices() throws IOException 
	{
        HttpTransport httpTransport = new NetHttpTransport();
        JsonFactory jsonFactory = new JacksonFactory();

        try
        {
        	GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
    				httpTransport, jsonFactory, CLIENT_ID, CLIENT_SECRET, Arrays.asList(DriveScopes.DRIVE))
    		.setAccessType("online")
    		.setApprovalPrompt("auto").build();

    		String url = flow.newAuthorizationUrl().setRedirectUri(REDIRECT_URI).build();
    		System.out.println("Please open the following URL in your browser then type the authorization code:");
    		System.out.println("  " + url);
    		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    		String code = br.readLine();

    		GoogleTokenResponse response = flow.newTokenRequest(code).setRedirectUri(REDIRECT_URI).execute();
    		GoogleCredential credential = new GoogleCredential().setFromTokenResponse(response);

    		//New authorized API client based on provided credentials
    		googleDriveClient = new Drive.Builder(httpTransport, jsonFactory, credential).build();
        }
        catch(IOException e) {
			System.out.println("Failed to create the Google drive client. Please check your Internet connection and your Google credentials.");
			e.printStackTrace();
        }     
    }

	// Create integration test files for testing purposes
	java.io.File testFile = new java.io.File("src/test/resources/IntegrationTestFile.txt");
	java.io.File testUpdateFile = new java.io.File("src/test/resources/IntegrationUpdateTestFile.txt");
	java.io.File testDeleteFile = new java.io.File("src/test/resources/IntegrationDeleteTestFile.txt");

//Ignored for maven build    @BeforeClass
	public static void setup() throws IOException  {
		initializeGoogleDriveServices();
		testGoogleDriveFileSyncManager = new GoogleDriveFileSyncManager(googleDriveClient);
	}

//Ignored for maven build    @AfterClass
	public void cleanUp() throws IOException {
		testGoogleDriveFileSyncManager.deleteFile(testFile);
		testGoogleDriveFileSyncManager.deleteFile(testUpdateFile);
	}
	
//Ignored for maven build    @Test
	public void testAddFileIntegration() throws IOException {
    	// Add file to test the file adding functionality
    	testGoogleDriveFileSyncManager.addFile(testFile);
		String newFileID = getFileId("IntegrationTestFile.txt");
		
		Assert.assertNotNull(newFileID);
	}
    
//Ignored for maven build    @Test
    public void testUpdateFileIntegration() throws IOException  {
    	// Add and then update to test the updating functionality
    	testGoogleDriveFileSyncManager.addFile(testUpdateFile);
    	String dateAdded = getFileDate("IntegrationUpdateTestFile.txt");
    	
    	testGoogleDriveFileSyncManager.updateFile(testUpdateFile);
    	String dateUpdated = getFileDate("IntegrationUpdateTestFile.txt");
    	
    	Assert.assertTrue(!dateUpdated.equals(dateAdded));
    }
    
//Ignored for maven build    @Test
    public void testDeleteFileIntegration() throws IOException  {
    	// Add and then delete to test the deleting functionality
    	testGoogleDriveFileSyncManager.addFile(testDeleteFile);
    	
    	testGoogleDriveFileSyncManager.deleteFile(testDeleteFile);

		String deletedFileID = getFileId("IntegrationDeleteTestFile.txt");
		Assert.assertNull(deletedFileID);	
    }
	
    public String getFileDate(String fileName) {
    	try 
		{
			List request = googleDriveClient.files().list();
			FileList googleFileList = request.execute();
			for(File googleFile : googleFileList.getItems()) {
				if (googleFile.getTitle().equals(fileName)) 
					return googleFile.getModifiedDate().toString();
			}
		} 
		catch (IOException e) {
			System.out.println("Failed to access Filelist from Google Drive client");
		}
		return null;
    }
    
	public String getFileId(String fileName) {
		try 
		{
			List request = googleDriveClient.files().list();
			FileList googleFileList = request.execute();
			for(File googleFile : googleFileList.getItems()) {
				if (googleFile.getTitle().equals(fileName)) 
					return googleFile.getId();
			}
		} 
		catch (IOException e) {
			System.out.println("Failed to access Filelist from Google Drive client");
		}
		return null;
	}	
}