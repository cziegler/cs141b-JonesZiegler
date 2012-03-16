package edu.caltech.cs141b.hw2.gwt.collab.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import edu.caltech.cs141b.hw2.gwt.collab.shared.LockExpired;
import edu.caltech.cs141b.hw2.gwt.collab.shared.LockedDocument;

public class LockChecker implements AsyncCallback<Boolean> {
	
	private Collaborator collaborator;
	
	public LockChecker(Collaborator collaborator) {
		this.collaborator = collaborator;
	}
	
	public void isValid(LockedDocument lockedDoc) {
		collaborator.collabService.isValid(lockedDoc, this);
	}

	@Override
	public void onFailure(Throwable caught) {
		if (caught instanceof LockExpired) {
			collaborator.statusUpdate("Lock had already expired.");
		} else {
			collaborator.statusUpdate("Error checking lock"
					+ "; caught exception " + caught.getClass()
					+ " with message: " + caught.getMessage());
			GWT.log("Error saving document.", caught);
		}
		
	}


	public void onSuccess(Boolean result) {
		collaborator.docChecked = result;
	}
	
}

