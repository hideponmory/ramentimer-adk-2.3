package com.androidtsubu.ramentimer;

/**
 * 商品マスタが重複している場合のExceptionです
 * @author hide
 *
 */
public class DuplexNoodleMasterException extends GaeException {

	private static final long serialVersionUID = 2332994598354016927L;
	
	public DuplexNoodleMasterException(){
		
	}
	
	/**
	 * コンストラクタ
	 * @param throwable
	 */
	public DuplexNoodleMasterException(Throwable throwable) {
		super(throwable);
	}


}
