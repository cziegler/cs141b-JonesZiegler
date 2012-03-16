package edu.caltech.cs141b.hw2.gwt.collab.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import edu.caltech.cs141b.hw2.gwt.collab.shared.UnlockedDocument;

/**
 * Used in conjunction with <code>CollaboratorService.getDocument()</code>.
 * This function is used to get readOnlyDocuments from the server
 */
public class ReadDocGetter implements AsyncCallback<UnlockedDocument> {
	
	private Collaborator collaborator;
	
	public ReadDocGetter(Collaborator collaborator) {
		this.collaborator = collaborator;
	}
	
	public void getDocument(String key) {
		collaborator.statusUpdate("Fetching document " + key + ".");
		collaborator.waitingKey = key;
		collaborator.collabService.getDocument(key, this);
	}

	@Override
	public void onFailure(Throwable caught) {
		collaborator.statusUpdate("Error retrieving document"
				+ "; caught exception " + caught.getClass()
				+ " with message: " + caught.getMessage());
		GWT.log("Error getting document.", caught);
	}

	@Override
	public void onSuccess(UnlockedDocument result) {
		if (result.getKey().equals(collaborator.waitingKey)) {
			collaborator.statusUpdate("Document '" + result.getTitle()
					+ "' successfully retrieved.");
			gotDoc(result);
		} else {
			collaborator.statusUpdate("Returned document that is no longer "
					+ "expected; discarding.");
		}
	}
	
	/**
	 * Sets the clients variables after the document is retrieved from the server
	 * 
	 * @param result the unlocked document that should be displayed
	 */
	protected void gotDoc(UnlockedDocument result) {
		collaborator.currentReadDoc = result;
		collaborator.currentLockDoc = null;
		collaborator.addTab(result);
		collaborator.setButtons("Read");
	}
}
