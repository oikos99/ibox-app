package edu.csupomona.cs585.ibox;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

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
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import edu.csupomona.cs585.ibox.sync.FileSyncManager;
import edu.csupomona.cs585.ibox.sync.GoogleDriveFileSyncManager;
import edu.csupomona.cs585.ibox.sync.GoogleDriveServiceProvider;

public class WatchDirIntegrationTest {

	static Timer timer = new Timer();
	
	static Drive googleDriveClient = GoogleDriveServiceProvider.get().getGoogleDriveClient();
	static GoogleDriveFileSyncManager fileSyncManager = new GoogleDriveFileSyncManager(googleDriveClient);

	private final static String TEST_DIRECTORY_NAME = "test_WatchDir";
	private final static String TEST_FILE_NAME = "TestWatchDir.txt";

	// Variables from WatchDir
    private static WatchService watcher;
    private static Map<WatchKey,Path> keys;
    private static boolean trace = false;

	
	// Create a test directory path
	static java.io.File testWatchFile = new java.io.File("src/test/resources/" + TEST_DIRECTORY_NAME + "/" + TEST_FILE_NAME);
	
	
	
//    @BeforeClass
	public static void setup() throws IOException {
		// Create a test directory		
		if(testWatchFile.exists() || testWatchFile.getParentFile().exists()) {	
			deleteDir(testWatchFile);
			deleteDir(testWatchFile.getParentFile());
		}
		
		if (!testWatchFile.getParentFile().mkdir())
			throw new IOException("Failed to create directory " + testWatchFile.getParent());	
	}
	
//    @AfterClass
	public static void cleanUp() {
		// Delete the test directory
		deleteDir(testWatchFile);
		deleteDir(testWatchFile.getParentFile());		
	}
	
//    @Test
	public void testWatchDirIntegration() throws IOException, InterruptedException {

		// Creates a WatchService and registers the test directory
		Path dir = Paths.get("src/test/resources/" + TEST_DIRECTORY_NAME);

        watcher = FileSystems.getDefault().newWatchService();
        keys = new HashMap<WatchKey,Path>();
        
		// Register the test directory with the WatchService
        register(dir);
        // enable trace after initial registration
        trace = true;
        
        Thread watchThread = new Thread(new Runnable(){
        	  @Override
			  public void run() {
        		  // Start "watching" test directory
        		  processEvents();
			  }        	
        });
        
        Thread directoryCreationThread = new Thread(new Runnable(){
      	  @Override
			  public void run() {
      		  // Create the testing directory and file after 2 sec delay
      		  timer.schedule(new TimerTask() {
      			  @Override
      			  public void run() {
      				  try {
      					  testWatchFile.createNewFile();
      				  } catch (IOException e) {
      					  // TODO Auto-generated catch block
      					  e.printStackTrace();
      				  }
      			  }
      		  }, 2*1000);
		  }        	
        });
        
        // Run the Watch Service and create the test directory and file concurrently
        watchThread.start();
        directoryCreationThread.start();
        
        // Delay the testing for 5 secs to compensate for synchronizing lag
        Thread.sleep(5000);
        
        Assert.assertTrue(checkFileExist(TEST_FILE_NAME));
	}

	public static void deleteDir(java.io.File file) {
		java.io.File[] contents = file.listFiles();
	    if (contents != null) {
	        for (java.io.File f : contents) {
	            deleteDir(f);
	        }
	    }
	    file.delete();
	}
	
    public boolean checkFileExist(String fileName) {
    	try 
		{
	    	List request = googleDriveClient.files().list();
	    	FileList googleFileList = request.execute();
			for(File googleFile : googleFileList.getItems()) {
				if (googleFile.getTitle().equals(fileName)) 
					return true;
			}
		} 
		catch (IOException e)
		{
			System.out.println("Failed to access Filelist from Google Drive client");
		}
    	return false;
    }   
    
    
    /**
     * WatchDir methods from WatchDir.java
     * 
     * 
     */    

    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>)event;
    }

    // Register the given directory with the WatchService
    private static void register(Path dir) throws IOException {
        WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        if (trace) {
            Path prev = keys.get(key);
            if (prev == null) {
                System.out.format("register: %s\n", dir);
            } else {
                if (!dir.equals(prev)) {
                    System.out.format("update: %s -> %s\n", prev, dir);
                }
            }
        }
        keys.put(key, dir);
    }

    // Process all events for keys queued to the watcher
    @SuppressWarnings("rawtypes")
	public static void processEvents() {
    	for (;;) {

            // wait for key to be signalled
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException x) {
                return;
            }

            Path dir = keys.get(key);
            if (dir == null) {
                System.err.println("WatchKey not recognized!!");
                continue;
            }

            for (WatchEvent<?> event: key.pollEvents()) {
                WatchEvent.Kind kind = event.kind();

                // TBD - provide example of how OVERFLOW event is handled
                if (kind == OVERFLOW) {
                    continue;
                }

                // Context for directory entry event is the file name of entry
                WatchEvent<Path> ev = cast(event);
                Path name = ev.context();
                Path child = dir.resolve(name);

                // print out event
                System.out.format("%s: %s\n", event.kind().name(), child);

                // sync to remote
                try {
	                if (event.kind() == ENTRY_CREATE) {
	                	fileSyncManager.addFile(child.toFile());
	                } else if (event.kind() == ENTRY_MODIFY) {
	                	fileSyncManager.updateFile(child.toFile());
	                } else if (event.kind() == ENTRY_DELETE) {
	                	fileSyncManager.deleteFile(child.toFile());
	                }
                } catch (IOException e) {
                	System.out.println("Failed to sync the file to remote storage");
                	e.printStackTrace();
                }
            }

            // reset key and remove from set if directory no longer accessible
            boolean valid = key.reset();
            if (!valid) {
                keys.remove(key);

                // all directories are inaccessible
                if (keys.isEmpty()) {
                    break;
                }
            } 
        }	// End endless for loop
    }
}