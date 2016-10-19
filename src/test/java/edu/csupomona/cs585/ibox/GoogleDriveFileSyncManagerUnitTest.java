package edu.csupomona.cs585.ibox;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.*;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files.Delete;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.Drive.Files.Insert;
import com.google.api.services.drive.Drive.Files.List;
import com.google.api.services.drive.Drive.Files.Update;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import edu.csupomona.cs585.ibox.sync.GoogleDriveFileSyncManager;

public class GoogleDriveFileSyncManagerUnitTest {
	
	//Google Drive service
	Drive mockedService = Mockito.mock(Drive.class);
	
	//create an instance of the GoogleDriveFileSyncManager to test
	GoogleDriveFileSyncManager testGoogleDriveFileSyncManager = new GoogleDriveFileSyncManager(mockedService);
	
	// Mock the appropriate class files in every step of execution
	final Files mockedServiceFiles = Mockito.mock(Files.class);
	final Insert mockedInsert = Mockito.mock(Insert.class);
	final Delete mockedDelete = Mockito.mock(Delete.class);
	final Update mockedUpdate = Mockito.mock(Update.class);
	final List mockedServiceFileslist = Mockito.mock(List.class);
	
	File testOutputFile = new File();
	FileList googleServiceFiles;
	
	// 
	java.io.File testLocalFile = new java.io.File("src/test/resources/UnitTestFile.txts");
	//java.io.File testLocalFile = Mockito.mock(java.io.File.class);
	
	// A file that doesn't exist on Google Drive to test the exceptions
	final java.io.File testAbsentFile = new java.io.File("src/test/resources/AbsentFile.txt");
	
	
	@Before
    public void setup() throws IOException {
		googleServiceFiles = initiateGoogleFileList();
		when(mockedService.files()).thenReturn(mockedServiceFiles);
		when(mockedServiceFiles.list()).thenReturn(mockedServiceFileslist);
		when(mockedServiceFileslist.execute()).thenReturn(googleServiceFiles);
		when(mockedServiceFiles.delete(any(String.class))).thenReturn(mockedDelete);
    }	
	
	@Test
	public void testAddFile() throws IOException {
		// Set the return behavior of the service as a mocked object of that type
		when(mockedServiceFiles.insert(any(File.class), any(FileContent.class))).thenReturn(mockedInsert);
		when(mockedInsert.execute()).thenReturn(testOutputFile);

		// Call the method and add a file
		testGoogleDriveFileSyncManager.addFile(testLocalFile);
		
		// Test
		verify(mockedInsert).execute();	
	}

	@Test
	public void testUpdateFile() throws IOException {
		// Set the return behavior of the service as a mocked object of that type
		when(mockedServiceFiles.update(any(String.class), any(File.class), any(FileContent.class))).thenReturn(mockedUpdate);
		when(mockedUpdate.execute()).thenReturn(testOutputFile);
		
		// Call the method and update a file		
		testGoogleDriveFileSyncManager.updateFile(testLocalFile);
		
		// Test		
		verify(mockedUpdate).execute();
	}
	
	@Test(expected=FileNotFoundException.class)
	public void testUpdateFileNotFoundException() throws IOException {
		// Passes in a file that doesn't exist to verify exception
		testGoogleDriveFileSyncManager.deleteFile(testAbsentFile);
	}

	@Test
	public void testDeleteFile() throws IOException {
		// Call the method and delete a file		
		testGoogleDriveFileSyncManager.deleteFile(testLocalFile);
		
		// Test
		verify(mockedDelete).execute();
	}

	@Test(expected = FileNotFoundException.class)
	public void testDeleteFileNotFoundException() throws IOException {
		// Passes in a file that doesn't exist to verify exception
		testGoogleDriveFileSyncManager.deleteFile(testAbsentFile);
	}
	
	@Test()
	public void testGetFileId() throws NullPointerException {
		// Passes in a file name that doesn't exist to verify null return
		String nullReturn = testGoogleDriveFileSyncManager.getFileId("AbsentFile.txt");
		Assert.assertNull(nullReturn);
	}
	
	public FileList initiateGoogleFileList() {
		// Creates a googleFileList with the testing Google File
		java.util.List<File> items = new java.util.ArrayList<File>();
		
		File googleFile = new File();
		googleFile.setTitle(testLocalFile.getName()).setId("1");
		items.add(googleFile);
		
		FileList googleFileList = new FileList();
		googleFileList.setItems(items);
		
		return googleFileList;
	}
}