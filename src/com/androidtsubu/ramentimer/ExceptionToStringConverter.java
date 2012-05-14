package com.androidtsubu.ramentimer;
import java.io.CharArrayWriter;
import java.io.PrintWriter;

/**
 * Exceptionの内容を文字列に変えてくれるだけのクラス
 * @author hide
 *
 */
public class ExceptionToStringConverter {
	/**
	 * ExceptionのStackTraceを文字列にconvertします
	 * @param ex
	 * @return
	 */
	public static String convert(Exception ex){
	    CharArrayWriter buf = new CharArrayWriter();
	    PrintWriter writer = new PrintWriter(buf);
	    //コンソールにエラー内容を書く
	    ex.printStackTrace();
	    //ログ保存用の出力先にエラー内容を書く
	    ex.printStackTrace(writer);
	    return buf.toString();		
	}

}
