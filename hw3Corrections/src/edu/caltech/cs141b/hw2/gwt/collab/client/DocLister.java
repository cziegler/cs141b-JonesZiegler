package edu.caltech.cs141b.hw2.gwt.collab.client;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import edu.caltech.cs141b.hw2.gwt.collab.shared.DocumentMetadata;

/**
 * Used in conjunction with <code>CollaboratorService.getDocumentList()</code>.
 */
public class DocLister implements AsyncCallback<List<DocumentMetadata>> {
	
	private Collaborator collaborator;
	
	public DocLister(Collaborator collaborator) {
		this.collaborator = collaborator;
	}
	
	public void getDocumentList() {
		collaborator.statusUpdate("Fetching document list.");
		collaborator.refreshList.setEnabled(false);
		collaborator.collabService.getDocumentList(this);
	}

	@Override
	public void onFailure(Throwable caught) {
		collaborator.statusUpdate("Error retrieving document list"
				+ "; caught exception " + caught.getClass()
				+ " with message: " + caught.getMessage());
		GWT.log("Error getting document list.", caught);
		collaborator.refreshList.setEnabled(true);
	}

	@Override
	public void onSuccess(List<DocumentMetadata> result) {
		if (result == null || result.size() == 0) {
			collaborator.statusUpdate("No documents available.");
		}
		else {
			collaborator.statusUpdate("Document list updated.");
			collaborator.documentList.clear();
			
			for (DocumentMetadata meta : result) {
				collaborator.documentList.addItem(
						meta.getTitle(), meta.getKey());
			}
		}
		collaborator.refreshList.setEnabled(true);
	}
	
}

