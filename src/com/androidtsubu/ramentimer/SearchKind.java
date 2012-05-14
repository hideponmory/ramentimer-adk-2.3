package com.androidtsubu.ramentimer;
/**
 * 検索種類
 * @author hide
 *
 */
public enum SearchKind {
	FAVORITE("KEY_FAVORITE"),HISTORY("KEY_HISTORY");

	private String key;
	
	private SearchKind(String key){
		this.key = key;
	}
	
	public String getKey(){
		return key;
	}
	
}
