package edu.caltech.cs141b.hw2.gwt.collab.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import edu.caltech.cs141b.hw2.gwt.collab.shared.LockUnavailable;
import edu.caltech.cs141b.hw2.gwt.collab.shared.LockedDocument;

/**
 * Used in conjunction with <code>CollaboratorService.lockDocument()</code>.
 * This class is used to get the LockedDocument from the Server
 */
public class LockedDocGetter implements AsyncCallback<LockedDocument> {
	
	private Collaborator collaborator;
	
	public LockedDocGetter(Collaborator collaborator) {
		this.collaborator = collaborator;
	}
	
	public void getDocument(String key) {
		collaborator.statusUpdate("Attempting to lock document.");
		collaborator.waitingKey = key;
		collaborator.collabService.lockDocument(key, this);
		//Disable locking and reading while we wait for the document to return
		collaborator.lockButton.setEnabled(false);
		collaborator.readButton.setEnabled(false);
	}

	@Override
	public void onFailure(Throwable caught) {
		if (caught instanceof LockUnavailable) {
			collaborator.statusUpdate("LockUnavailable: " + caught.getMessage());
		} else {
			collaborator.statusUpdate("Error retrieving lock"
					+ "; caught exception " + caught.getClass()
					+ " with message: " + caught.getMessage());
			GWT.log("Error getting document lock.", caught);
		}
		collaborator.lockButton.setEnabled(true);
		collaborator.readButton.setEnabled(true);
	}

	@Override
	public void onSuccess(LockedDocument result) {
		if (result.getKey().equals(collaborator.waitingKey)) {
			GWT.log("Contents: "+ result.getContents());
			collaborator.statusUpdate("Lock retrieved for document.");
			gotDoc(result);
		} else {
			collaborator.statusUpdate("Got lock for document which is "
					+ "no longer active.  Releasing lock.");
		}
	}
	
	/**
	 * Sets the appropriate values and prepares the frontend.
	 * 
	 * @param result
	 */
	protected void gotDoc(LockedDocument result) {
		collaborator.currentReadDoc = null;
		collaborator.currentLockDoc = result;
		collaborator.addTab(result);
		collaborator.setButtons("Lock");
		}
	
}


