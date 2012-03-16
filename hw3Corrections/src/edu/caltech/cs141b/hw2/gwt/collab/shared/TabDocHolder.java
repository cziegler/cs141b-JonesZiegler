package edu.caltech.cs141b.hw2.gwt.collab.shared;

import com.google.gwt.user.client.rpc.IsSerializable;
import edu.caltech.cs141b.hw2.gwt.collab.shared.LockedDocument;
import edu.caltech.cs141b.hw2.gwt.collab.shared.UnlockedDocument;

public class TabDocHolder implements IsSerializable {
	
	private LockedDocument lockedPart = null;
	private UnlockedDocument unlockedPart = null;
	private int type = 0;
	
	public TabDocHolder(){

	}
	
	
	public int Type(){ // 0 if unlocked 1 if locked 2 if newly created
		return type;
	}
	
	public LockedDocument getLocked(){
		return lockedPart;
	}
	
	public UnlockedDocument getUnlocked(){
		return unlockedPart;
	}
	
	public void setLocked(LockedDocument lDoc){
		lockedPart = lDoc;
		unlockedPart = null;
		if (lockedPart.getKey()== null){
			type = 2;
		}else{
			type = 1;
		}
	}
	
	public void setUnlocked(UnlockedDocument uDoc){
		unlockedPart = uDoc;
		lockedPart = null;
		type = 0;
	}
	
	public void setNewType(){
		type = 2;
	}
	
	public void clearDocHolder(){
		unlockedPart = null;
		lockedPart = null;
		type = 0;
	}

}
