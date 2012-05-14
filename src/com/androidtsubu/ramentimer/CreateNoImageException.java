package com.androidtsubu.ramentimer;
/**
 * 登録するときに画像がセットされてないときに画像が無いときのexceptionです
 * @author otori
 *
 */
public class CreateNoImageException extends Exception {

	private static final long serialVersionUID = -4808215196788142640L;

	public CreateNoImageException(){
		
	}
	
	/**
	 * コンストラクタ
	 * @param throwable
	 */
	public CreateNoImageException(Throwable throwable){
		super(throwable);
	}
	
}
