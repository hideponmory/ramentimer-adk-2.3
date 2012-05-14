package com.androidtsubu.ramentimer;

/**
 * 麺種類です
 * @author hide
 *
 */
public enum NoodleType{
	UNKNOWN("不明"),RAW("生麺"),DRIED("乾麺");
	
	private String name;
	
	/**
	 * コンストラクタ
	 * @param id
	 * @param name
	 */
	private NoodleType(String name){
		this.name = name;
	}
	
	
	public String getName(){
		return name;
	}

	
}
