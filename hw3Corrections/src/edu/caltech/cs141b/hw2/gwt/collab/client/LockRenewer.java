package edu.caltech.cs141b.hw2.gwt.collab.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import edu.caltech.cs141b.hw2.gwt.collab.shared.LockUnavailable;
import edu.caltech.cs141b.hw2.gwt.collab.shared.LockedDocument;

/**
 * Used in conjunction with <code>CollaboratorService.lockDocument()</code>.
 * This is used to renew the lock when tabs are changed
 */
public class LockRenewer implements AsyncCallback<LockedDocument> {
	
	private Collaborator collaborator;
	private LockedDocument lockedDoc;
	
	public LockRenewer(Collaborator collaborator) {
		this.collaborator = collaborator;
	}
	
	public void renewLock() {
		lockedDoc = collaborator.currentLockDoc;
		collaborator.statusUpdate("Attempting to renew lock.");
		collaborator.collabService.renewLock(collaborator.currentLockDoc, this);
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
	}
	
	// When successful, sets the appropriate variable for the client
	@Override
	public void onSuccess(LockedDocument result) {
		if (result.getKey().equals(lockedDoc.getKey())) {
			collaborator.statusUpdate("Lock renewed for document.");
			collaborator.currentLockDoc = result;
		} else {
			collaborator.statusUpdate("Got lock for document which is "
					+ "no longer active.  Releasing lock.");
			collaborator.releaser.releaseLock(result);
		}
	}
	
}


