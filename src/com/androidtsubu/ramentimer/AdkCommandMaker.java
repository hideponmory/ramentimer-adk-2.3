package com.androidtsubu.ramentimer;

import java.util.ArrayList;
import java.util.List;

/**
 * ADKに送るコマンドを作成する
 * 
 * @author hide
 * 
 */
public class AdkCommandMaker {
	/** Timerコマンド */
	private static final int sCOMMAND_TIMER = 1;
	/** ブザーコマンド */
	private static final int sCOMMAND_BUZZER = 2;
	/** ボタンコマンド */
	private static final int sCOMMAND_BUTTON = 3;
	private static final int APP_CONNECT = (int) 0xFE;
	private static final int APP_DISCONNECT = (int) 0xFF;

	private static final byte digit12 = (byte)0x40;
	private static final byte digit34 = (byte)0x80;

	/**
	 * 時間設定コマンドを作成する
	 * 
	 * @param m
	 * @param s
	 * @return
	 */
	public static byte[] makeMinCommand(int m) {
		byte[] commands = new byte[2];
		// 3-4桁目
		commands[0] = sCOMMAND_TIMER;
		commands[1] = digit34;
		commands[1] |= m;
		return commands;
	}
	
	public static byte[] makeSecCommand(int s){
		byte[] commands = new byte[2];
		// 1-2桁目
		commands[0] = sCOMMAND_TIMER;
		commands[1] = digit12;
		commands[1] |= s;
		return commands;
	}

	/**
	 * ブザー設定コマンドを作成する
	 * 
	 * @param on
	 * @return
	 */
	public static byte[] makeBuzzerCommand(boolean on) {
		byte[] commands = new byte[2];
		commands[0] = sCOMMAND_BUZZER;
		commands[1] = on ? (byte) 1 : (byte) 0;
		return commands;
	}

	/**
	 * 接続コマンドを作成する
	 * 
	 * @return
	 */
	public static byte[] makeConnectCommand() {
		byte[] commands = new byte[2];
		commands[0] = (byte) APP_CONNECT;
		commands[1] = 0;
		return commands;
	}

	public static byte[] makeDisConnectCommand() {
		byte[] commands = new byte[2];
		commands[0] = (byte) APP_DISCONNECT;
		commands[1] = 0;
		return commands;
	}
}
