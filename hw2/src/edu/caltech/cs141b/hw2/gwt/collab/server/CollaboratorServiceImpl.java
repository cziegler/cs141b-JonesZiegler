package edu.caltech.cs141b.hw2.gwt.collab.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.logging.Logger;
import javax.jdo.annotations.Persistent;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Transaction;



import edu.caltech.cs141b.hw2.gwt.collab.client.CollaboratorService;
import edu.caltech.cs141b.hw2.gwt.collab.shared.DocumentMetadata;
import edu.caltech.cs141b.hw2.gwt.collab.shared.LockExpired;
import edu.caltech.cs141b.hw2.gwt.collab.shared.LockUnavailable;
import edu.caltech.cs141b.hw2.gwt.collab.shared.LockedDocument;
import edu.caltech.cs141b.hw2.gwt.collab.shared.UnlockedDocument;


import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class CollaboratorServiceImpl extends RemoteServiceServlet implements
		CollaboratorService {
	@Persistent
	public List<DocumentMetadata> metadataList = new ArrayList<DocumentMetadata>(); // List of metadata
	private static final Logger log = Logger.getLogger(CollaboratorServiceImpl.class.toString());
	DatastoreService datastore = DatastoreServiceFactory.getDatastoreService(); // Stores all documents
											/* Documents will be stored as Entities with properties:
											 * Date, Title, Contents, LockedBy representing respectively 
											 * Time Until Unlocked, Title, Contents, and ID of the locking client*/

	@Persistent
	public int count=0; //This is used to assign a unique ID to each instance of a lock
	
	/* This function returns the list of metadata 
	 * Arguments: None
	 * Return: ArrayList<DocumentMetadata>*/
	@Override
	public List<DocumentMetadata> getDocumentList() {
		return metadataList;
	}


	/* This function takes a key and locks a document if the document is available
	 * Arguments: String (serves as a key) 
	 * Returns: LockedDocument */
	@SuppressWarnings("deprecation") // Because the operations to set the time on the Date class are deprecated
	@Override
	public LockedDocument lockDocument(String documentKey)
			throws LockUnavailable {
		Key docKey = KeyFactory.stringToKey(documentKey);
		Transaction txn = datastore.beginTransaction(); //Implemented as a transaction for concurrency
		try{
		Entity document = datastore.get(docKey);
		Date currentTime = new Date(); // Sets current time
		Date lockedTime = (Date) document.getProperty("Date"); // Gets the time stored in the lock
		if ((lockedTime==null) || lockedTime.before(new Date())){
			// getting here means the document is unlocked or has expired lock
			currentTime.setMinutes(currentTime.getMinutes()+10);
			document.setProperty("Date", currentTime); // Sets the time for the new lock 10 minutes in the future 
			count++; // Gets a new process ID
			document.setProperty("LockedBy", Integer.toString(count));
			String title = (String) document.getProperty("Title");
			String contents = (String) document.getProperty("Contents");
			// Creates a new document with the above values.
			LockedDocument newLock = new LockedDocument(Integer.toString(count), currentTime, documentKey, title, contents);
			datastore.put(txn,document); // stores the Entity back in the datastore with updated values
			txn.commit(); // ends the transaction
			return newLock;						
			} else {
				throw new LockUnavailable("Document currently locked");
			}
		} catch (EntityNotFoundException e) {
			/* This should never occur as it would require picking a nonexistent key out of the list */
			throw new LockUnavailable("Key not found");
		} finally{
			if (txn.isActive()){
				txn.rollback();
			}
		}
	}


	/* This function takes a document key and returns the document (if found) in read only 
	 * Arguments: String
	 * Returns: UnlockedDocument */
	@Override
	public UnlockedDocument getDocument(String documentKey) {
		
		if (isKeyPresent(documentKey)){ // Checks if the key is present in the datastore. This prevents the caught error from ever occurring.
			try{
			Key docKey = KeyFactory.stringToKey(documentKey); 
			Entity document = datastore.get(docKey);
			UnlockedDocument newDoc = new UnlockedDocument(documentKey, (String) document.getProperty("Title"), (String) document.getProperty("Contents"));
			return newDoc;
			}catch(EntityNotFoundException e){
				// This should never occur
			}
		}
		return new UnlockedDocument();
		
	}


	/* This function takes a LockedDocument and if the key and lockedBy ID matches that of the document in 
	 * the list, unlocks the document and puts it in the unlocked list. If no key matches the LockedDocuments'
	 * key, then this saves a new document in the unlocked list.
	 * Arguments: LockedDocument
	 * Returns: UnlockedDocument */
	@Override
	public UnlockedDocument saveDocument(LockedDocument doc)
			throws LockExpired {
		// Defining the parameters of the document
		String title = doc.getTitle(); 
		String contents = doc.getContents();
		String lockedBy = doc.getLockedBy();
		Date lockedUntil = doc.getLockedUntil();
		Transaction txn = datastore.beginTransaction();
		if(isKeyPresent(doc.getKey())){ // This prevents the caught error from ever occurring.
			Key docKey = KeyFactory.stringToKey(doc.getKey());
			try{
				Entity document = datastore.get(docKey);
				if ((document.getProperty("LockedBy").equals(lockedBy))&&(isValid(lockedUntil)) ){
					// Getting here means there is a document with the same key in the datastore and the lock matches up
					// Setting the properties in the datastore
					document.setProperty("Contents", contents);
					if(!(document.getProperty("Title").equals(title))){ // This updates the list of metadata if the titles are different
						metaUpdate(doc.getKey(),title);
					}
					document.setProperty("Title", title);
					document.setProperty("Date", null);
					document.setProperty("LockedBy", null);
					datastore.put(txn, document);
					txn.commit();
					}else{
						throw new LockExpired("Lock Expired");
						}
			}catch(EntityNotFoundException e) {
				// This should never occur
			}finally{
				if (txn.isActive()){
					txn.rollback();
				}
			}
		} else {
			// Getting here means there is no key matching the document in the list ie new document
			// Setting properties in the datastore
			Entity newDoc = new Entity("Document");
			newDoc.setProperty("Title", title);
			newDoc.setProperty("Contents", contents);
			newDoc.setProperty("Date", null);
			newDoc.setProperty("LockedBy", null);
			Key keys = datastore.put(txn, newDoc);
			txn.commit();
			// Updating list of metadata
			DocumentMetadata meta= new DocumentMetadata(KeyFactory.keyToString(keys), title);
			metadataList.add(meta);
			return new UnlockedDocument(KeyFactory.keyToString(keys), title, contents);
		}
		
		
		return new UnlockedDocument(doc.getKey(), title, contents);
	}


	/* This function takes a LockedDocument, checks that it is still locked with the same id
	 * and if so, returns it to the list of UnlockedDocuments */	
	@Override
	public void releaseLock(LockedDocument doc) throws LockExpired {
		String lockedBy = doc.getLockedBy();
		Date lockedUntil = doc.getLockedUntil();
		Transaction txn = datastore.beginTransaction();
		try{
			Key docKey = KeyFactory.stringToKey(doc.getKey());
			Entity document = datastore.get(docKey);
			if ((document.getProperty("LockedBy").equals(lockedBy))&&(isValid(lockedUntil))){
				// Getting here means the document lock matches and hasn't expired
				document.setProperty("LockedBy",null);
				document.setProperty("Date", null);
				datastore.put(txn, document);
				txn.commit();
			}else{
				throw new LockExpired("Lock has expired");
			}
		}catch(EntityNotFoundException e){
			/* This should never occur because LockedDocuments can only be created from the datastore and getting here means there
			 * are no matching documents in the datastore. */
		}finally{
			if(txn.isActive()){
				txn.rollback();
			}
		}
	}

	/* Helper function: Checks if the key matches any documents in the system */	
	public boolean isKeyPresent(String documentKey) {
		for (int i=0; i<metadataList.size(); i++){
			if (metadataList.get(i).getKey().equals(documentKey)){
				return true;
			}
		}
		return false;
	}
	
	/* Helper function: Checks if lock is expired */
	public boolean isValid(Date lockBy){
		Date currentTime = new Date();
		return currentTime.before(lockBy);
	}
	
	/* Helper function: Updates metadata */
	public void metaUpdate(String key, String newTitle){
		for(int i=0; i<metadataList.size(); i++){
			if (metadataList.get(i).getKey().equals(key)){
					metadataList.get(i).title = newTitle;
			}
		}
		return;
	}
}

