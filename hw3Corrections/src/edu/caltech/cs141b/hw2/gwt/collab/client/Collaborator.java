package edu.caltech.cs141b.hw2.gwt.collab.client;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DecoratorPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.TabBar;
import com.google.gwt.core.client.GWT;
import java.util.ArrayList;

import edu.caltech.cs141b.hw2.gwt.collab.shared.LockedDocument;
import edu.caltech.cs141b.hw2.gwt.collab.shared.UnlockedDocument;
import edu.caltech.cs141b.hw2.gwt.collab.shared.TabDocHolder;
/**
 * Main class for a single Collaborator widget.
 * 
 * Note: Functions and definitions that were previously included but are no longer
 * necessary have been left in, but commented out.
 * 
 */
public class Collaborator extends Composite implements ClickHandler, ChangeHandler {
	
	protected CollaboratorServiceAsync collabService;
	
	// Setting up local variables
	
	// Track document information.
	protected UnlockedDocument readOnlyDoc = null;
	protected UnlockedDocument currentReadDoc = null; // Main unlocked document
	public LockedDocument lockedDoc = null;
	protected LockedDocument currentLockDoc = null; // Main locked document. One of these is always null
	protected String currentTitle;
	protected String currentContents;
	
	// Managing available documents.
	protected ListBox documentList = new ListBox();
	protected Button refreshList = new Button("Refresh Document List");
	private Button createNew = new Button("Create New Document");
	
	// For displaying document information and editing document content.
	protected TextBox title = new TextBox();
	protected TabBar tabs = new TabBar(); // Implements tabs for multiple items
	protected int activeTabIndex = -1; // Keeps track of current tab
	protected ArrayList<TabDocHolder> tabList = new ArrayList<TabDocHolder>(); /* This is a list associated with
	 														* the tab panel that keeps track of the documents
	 														* associated with each tab */
	protected TextArea contents = new TextArea();
	protected Button refreshDocButton = new Button("Refresh Document");
	protected Button lockButton = new Button("Get Document Lock");
	protected Button saveButton = new Button("Save Document");
	protected Button readButton = new Button("Read Document"); // Used to get read only documents
	protected Button returnButton = new Button("Release Document"); // Releases documents
	
	// Callback objects.
	protected DocLister lister = new DocLister(this);
	//protected DocReader reader = new DocReader(this); Removed: These are no longer necessary
	//private DocLocker locker = new DocLocker(this);
	protected DocReleaser releaser = new DocReleaser(this);
	private DocSaver saver = new DocSaver(this);
	private LockChecker checker = new LockChecker(this); // Checks if the lock is active
	private LockRenewer renewer = new LockRenewer(this); // Renews the lock when a tab is switched to
	private ReadDocGetter readGet = new ReadDocGetter(this); // Gets the Unlocked Documents from the server
	private LockedDocGetter lockGet = new LockedDocGetter(this); // Gets the Locked Documents from the server
	protected String waitingKey = null;
	protected boolean updateTab = true; // Used to enable or disable tab changes when appropriate
	protected boolean createBool = false; // Signifies the creation of a new document
	
	// Status tracking.
	private VerticalPanel statusArea = new VerticalPanel();

	public boolean docChecked; // Is true if the document lock comes back valid.
	
	/**
	 * UI initialization.
	 * 
	 * @param collabService
	 */
	public Collaborator(CollaboratorServiceAsync collabService) {
		// Setting up the frontend interface
		this.collabService = collabService;
		HorizontalPanel outerHp = new HorizontalPanel();
		outerHp.setWidth("100%");
		VerticalPanel outerVp = new VerticalPanel();
		outerVp.setSpacing(20);
		
		VerticalPanel vp = new VerticalPanel();
		vp.setSpacing(10);
		vp.add(new HTML("<h2>Available Documents</h2>"));
		documentList.setWidth("100%");
		vp.add(documentList);
		HorizontalPanel hp = new HorizontalPanel();
		hp.setSpacing(10);
		hp.add(refreshList);
		hp.add(createNew);
		hp.add(readButton);
		hp.add(lockButton);
		vp.add(hp);
		DecoratorPanel dp = new DecoratorPanel();
		dp.setWidth("100%");
		dp.add(vp);
		outerVp.add(dp);
		
		vp = new VerticalPanel();
		vp.setSpacing(10);
		title.setWidth("100%");
		tabs.setWidth("100%");
		contents.setWidth("100%");
		vp.add(tabs);
		vp.add(title);
		vp.add(contents);
		hp = new HorizontalPanel();
		hp.setSpacing(10);
		hp.add(refreshDocButton);
		hp.add(returnButton);
		hp.add(saveButton);
		vp.add(hp);
		dp = new DecoratorPanel();
		dp.setWidth("100%");
		dp.add(vp);
		outerVp.add(dp);
		
		outerHp.add(outerVp);
		outerVp = new VerticalPanel();
		outerVp.setSpacing(20);
		dp = new DecoratorPanel();
		dp.setWidth("100%");
		statusArea.setSpacing(10);
		statusArea.add(new HTML("<h2>Console</h2>"));
		dp.add(statusArea);
		outerVp.add(dp);
		outerHp.add(outerVp);
		
		refreshList.addClickHandler(this);
		createNew.addClickHandler(this);
		refreshDocButton.addClickHandler(this);
		lockButton.addClickHandler(this);
		saveButton.addClickHandler(this);
		readButton.addClickHandler(this);
		returnButton.addClickHandler(this);
		
		documentList.addChangeHandler(this);
		documentList.setVisibleItemCount(10);
		contents.setVisibleLines(10);
		title.addChangeHandler(this);
		contents.addChangeHandler(this);
		contents.setEnabled(true);
		
		/* This sets up the underlying data structure when a tab is selected */
		tabs.addSelectionHandler(new SelectionHandler<Integer>() {
			public void onSelection(SelectionEvent<Integer> event) {
		        if (event.getSelectedItem() >= 0 && updateTab){
		        	changeTab(event.getSelectedItem());
		        }
		      }
		});
		
		
		setButtons("Default");
		initWidget(outerHp);
		
		lister.getDocumentList();
	}
	
	/**
	 * Resets the state of the buttons and edit objects to their default.
	 * 
	 * The state of these objects is modified by requesting or obtaining locks
	 * and trying to or successfully saving.
	 */
	protected void setDefaultButtons() {
		setButtons("Default");
	}
	
	/* Generalises setDefaultButtons to reflect the appropriate state on any change 
	 * Note: isIndex returns true when a valid index is selected in the Document list, 
	 * which allows for a read or write only doc to be accessed */
	protected void setButtons(String input) {
		/* Default Button layout: Used on initialisation and when the list is refreshed */
		if (input.equals("Default")){
			refreshDocButton.setEnabled(false);
			lockButton.setEnabled(isIndex());
			saveButton.setEnabled(false);
			title.setEnabled(false);
			contents.setReadOnly(true);
			contents.setValue("");
			returnButton.setEnabled(false);
			readButton.setEnabled(isIndex());
			createNew.setEnabled(true);
		}
		/* Used when the document is newly creates or locked, or when such a document is 
		 * reselected upon a change of tabs */
		else if(input.equals("Create") || input.equals("Lock")){
			refreshDocButton.setEnabled(false);
			lockButton.setEnabled(isIndex());
			saveButton.setEnabled(true);
			title.setEnabled(true);
			contents.setReadOnly(false);
			title.setValue(currentLockDoc.getTitle());
			contents.setValue(currentLockDoc.getContents());
			currentContents=contents.getValue();
			currentTitle=title.getValue();
			returnButton.setEnabled(true);
			readButton.setEnabled(isIndex());
		/* Used when a ReadOnlyDocument is created or selected */	
		}else if(input.equals("Read")){
			refreshDocButton.setEnabled(true);
			lockButton.setEnabled(isIndex());
			saveButton.setEnabled(false);
			returnButton.setEnabled(true);
			readButton.setEnabled(isIndex());
			contents.setReadOnly(true);
			title.setEnabled(false);
			contents.setValue(currentReadDoc.getContents());
			title.setValue(currentReadDoc.getTitle());
			currentContents=contents.getValue();
			currentTitle=title.getValue();
		/* Updating with regard to a changing list */
		}else if(input.equals("ListChange")){
			lockButton.setEnabled(isIndex());
			readButton.setEnabled(isIndex());
		}
	}
	/**
	 * This function has been removed and is no longer necessary.
	 *
	private void createNewDocument() {
		discardExisting(null);
		lockedDoc = new LockedDocument(null, null, null,
				"Enter the document title.",
				"Enter the document contents.");
		locker.gotDoc(lockedDoc);
		History.newItem("new");
	}
	
	/**
	 * Returns the currently active token.
	 * 
	 * @return history token which describes the current state
	 */
	protected String getToken() {
		if (lockedDoc != null) {
			if (lockedDoc.getKey() == null) {
				return "new";
			}
			return lockedDoc.getKey();
		} else if (readOnlyDoc != null) {
			return readOnlyDoc.getKey();
		} else {
			return "list";
		}
	}
	
	/**
	 * Modifies the current state to reflect the supplied token.
	 * 
	 * @param args history token received
	 */
	protected void receiveArgs(String args) {
		if (args.equals("list")) {
			readOnlyDoc = null;
			lockedDoc = null;
			title.setValue("");
			contents.setValue("");
			setDefaultButtons();
		} /*else if (args.equals("new")) {
			createNewDocument();
		} else {
			reader.getDocument(args);
		} No longer used, but left in for clarity */
	}
	
	/**
	 * Adds status lines to the console window to enable transparency of the
	 * underlying processes.
	 * 
	 * @param status the status to add to the console window
	 */
	protected void statusUpdate(String status) {
		while (statusArea.getWidgetCount() > 22) {
			statusArea.remove(1);
		}
		final HTML statusUpd = new HTML(status);
		statusArea.add(statusUpd);
	}

	/* (non-Javadoc)
	 * Receives button events.
	 * @see com.google.gwt.event.dom.client.ClickHandler#onClick(com.google.gwt.event.dom.client.ClickEvent)
	 */
	@Override
	public void onClick(ClickEvent event) {
		if (event.getSource().equals(refreshList)) {
			History.newItem("list");
			lister.getDocumentList();
		} else if (event.getSource().equals(createNew)) {
			createBool = true; // This is used to set a different type in the underlying tabArray
			currentReadDoc = null;
			currentLockDoc = new LockedDocument(null, null, null, 
					"Enter the document title.", "Enter the document contents.");
			addTab(currentLockDoc);
			setButtons("Create");
			createBool = false;
		} else if (event.getSource().equals(refreshDocButton)) {
			/* Can only be used on read only documents
			 * Removes the old tab and adds a new one with the updated document */
			activeTabIndex = tabs.getSelectedTab();
			removeTab();
			readGet.getDocument(documentList.getValue(documentList.getSelectedIndex()));
		} else if (event.getSource().equals(lockButton)) {
			// Creates an editable tab with the selected document from the document list
			activeTabIndex = tabs.getSelectedTab();
			lockGet.getDocument(documentList.getValue(documentList.getSelectedIndex()));
		} else if (event.getSource().equals(saveButton)) {
			// Sends the document to the server and saves the changes
			currentLockDoc.setTitle(currentTitle);
			currentLockDoc.setContents(currentContents);
			activeTabIndex = tabs.getSelectedTab();
			saver.saveDocument(currentLockDoc);
		} else if (event.getSource().equals(returnButton)){
			removeTab();
		} else if (event.getSource().equals(readButton)){
			// Creates a read only tab with the selected document from the document list
			activeTabIndex = tabs.getSelectedTab();
			readGet.getDocument(documentList.getValue(documentList.getSelectedIndex()));
		}
	}

	/* (non-Javadoc)
	 * Intercepts events from the list box.
	 * @see com.google.gwt.event.dom.client.ChangeHandler#onChange(com.google.gwt.event.dom.client.ChangeEvent)
	 */
	@Override
	public void onChange(ChangeEvent event) {
		if (event.getSource().equals(documentList)) {
			// Updates the buttons to reflect if an index is selected
			setButtons("ListChange");
		}
		if (event.getSource().equals(title)){
			/* Updates the title tab and makes sure the program has the newest state of the title */ 
			tabs.setTabText(tabs.getSelectedTab(), title.getText());
			currentTitle=title.getText();
		}
		if (event.getSource().equals(contents)){
			/* Makes sure the program has the newest state of the contents */
			currentContents = contents.getValue();
		}
	}
	
	
	
	
	/**
	 * Used to release existing locks when the active document changes.
	 * 
	 * @param key the key of the new active document or null for a new document
	 * This is no longer necessary as everything is handled at the tab level
	 *
	private void discardExisting(String key) {
		if (lockedDoc != null) {
			if (lockedDoc.getKey() == null) {
				statusUpdate("Discarding new document.");
			}
			else if (!lockedDoc.getKey().equals(key)) {
				releaser.releaseLock(lockedDoc);
			}
			else {
				// Newly active item is the currently locked item.
				return;
			}
			lockedDoc = null;
			setDefaultButtons();
		} else if (readOnlyDoc != null) {
			if (readOnlyDoc.getKey().equals(key)) return;
			readOnlyDoc = null;
		}
	}
	
	/* This checks if the List of Documents currently has something selected
	 * If this is the case, then locking or reading a document will work correctly */
	private boolean isIndex(){
		boolean val = true;
		if (documentList.getSelectedIndex()< 0){
			val = false;
		}
		return val;
	}
	
	/* This function adds a tab to the interface and deals with the underlying 
	 * data structure of storing the document when we are dealing with a
	 * LockedDocument. There is a second function for UnlockedDocuments */
	
	public void addTab(LockedDocument lDoc){
		if (tabs.getTabCount() == tabList.size()){
			if (tabs.getTabCount()==0){
				/* This means the tabBar was empty, so we want to avoid
				 * having changeTab take effect. */
				activeTabIndex = 0;
			}
			// Add a tab to the tab panel
			tabs.addTab(lDoc.getTitle());
			// Set up the datastructure associated with the new document
			TabDocHolder docHold = new TabDocHolder();
			docHold.setLocked(lDoc);
			if(createBool){
				/* A new document has a different type compared to the others */
				docHold.setNewType();
			}
			// Update the datastructure
			tabList.add(docHold);
			tabs.selectTab(tabs.getTabCount()-1);
			changeTab(tabs.getTabCount()-1);
		} else {
			/* This would only occur is somehow the tabs got out of sync 
			 * with the datastructure which should never occur since tab
			 * manipulation only occurs with a paired action */
			tabPanic();
		}
	}
	
	/* This is the unlocked version of addTab */
	public void addTab(UnlockedDocument uDoc){
		if (tabs.getTabCount() == tabList.size()){
			if (tabs.getTabCount()==0){
				/* This means the tabBar was empty, so we want to avoid
				 * having changeTab take effect. */
				activeTabIndex = 0;
			}
			tabs.addTab(uDoc.getTitle());
			TabDocHolder docHold = new TabDocHolder();
			docHold.setUnlocked(uDoc);
			tabList.add(docHold);
			tabs.selectTab(tabs.getTabCount()-1);
			changeTab(tabs.getTabCount()-1);
		} else {
			tabPanic();
		}
	}
	
	/* This changes from the current tab to the new tab, handling the underlying
	 * datastructure, keeping them in sync. */
	public void changeTab(int newIndex){
		if (newIndex != activeTabIndex){ // This is the case whenever we change from one tab to another
			if(tabList.get(activeTabIndex).Type()!= 0){
				// This means the current tab is open to a locked document and we should save the contents
				LockedDocument temp = tabList.get(activeTabIndex).getLocked();
				temp.setContents(currentContents);
				temp.setTitle(currentTitle);
				tabList.get(activeTabIndex).setLocked(temp);
			}
			// Updating the datastructure with the new active values
			TabDocHolder newHold = tabList.get(newIndex);
			currentReadDoc = newHold.getUnlocked();
			currentLockDoc = newHold.getLocked();
			if (newHold.Type()==1){
				/* If we get here, the tab we change to is a locked document so we should 
				 * renew the lock if it is not expired */
				checker.isValid(currentLockDoc); // If the lock is valid, this sets docChecked to true, otherwise false
				if (docChecked){
					// Renew Lock
					renewer.renewLock();
				}
				setButtons("Lock");
			}else if(newHold.Type()==2){
				// If we are here, then we are switching to a created document
				setButtons("Lock");
			}else{
				// If we are here, we are switching to a readOnlyDocument
				setButtons("Read");
			}
			activeTabIndex = tabs.getSelectedTab();
		}
	}
	
	/* This removes a tab from the tab panel and removes the associated values in the
	 * datastructure, then switched to the first tab */
	public void removeTab(){
		activeTabIndex = tabs.getSelectedTab();
		if (tabList.get(activeTabIndex).Type()==1){
			// If we are here, then the current tab has a locked document that needs to be unlocked
			releaser.releaseLock(currentLockDoc);
		}	
		updateTab = false; // This disables the selection handler, which would active change tab
		/* Remove the tab and the associated object from the datastructure */
		tabList.remove(activeTabIndex);
		tabs.removeTab(activeTabIndex);
		if (tabs.getTabCount()>0){
			/* If we are here, then there are still remaining tabs, and we should switch to the first tab */
			activeTabIndex = 0;
			tabs.selectTab(0);
			currentLockDoc = tabList.get(activeTabIndex).getLocked();
			currentReadDoc = tabList.get(activeTabIndex).getUnlocked();
			if (tabList.get(activeTabIndex).Type() == 0){
				setButtons("Read");
			} else if (tabList.get(activeTabIndex).Type() == 2){
				setButtons("Lock");
			} else {
				checker.isValid(currentLockDoc);
				if (docChecked){
					setButtons("Lock");
					}else{
					// If we are here the lock expired, so we turn the locked document into a readOnly document
						currentReadDoc = new UnlockedDocument(currentLockDoc.getKey(), 
									currentLockDoc.getTitle(), currentLockDoc.getContents());
						tabList.get(activeTabIndex).setUnlocked(currentReadDoc);
						currentLockDoc = null;
						setButtons("Read");
					}
				}	
		}else{
			// If we are here, the tab list is empty
			setButtons("Default");
		}
		updateTab = true;
	}
	
	
	/* This function removes everything from the tab panel and datastore. This only gets called 
	 * if the number of tabs and the number of entries in the datastructure become uneven, which 
	 * can only occur if there is a malfunction, since tabs are added and removed by functions 
	 * which work with the datastructure */
	private void tabPanic(){
		statusUpdate("Something went wrong with the tab panel");
		for(int i=0; i<tabs.getTabCount(); i++){
			tabs.removeTab(0);
		}
		for(int i=0; i<tabList.size(); i++){
			if(tabList.get(0).Type()!=0){
				releaser.releaseLock(tabList.get(0).getLocked());
			}
			tabList.remove(0);
		}
	}
}


