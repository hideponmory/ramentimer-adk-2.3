package com.androidtsubu.ramentimer;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * ラーメンを食べた履歴です
 * 
 * @author hide
 * 
 */
public class NoodleHistory implements Parcelable {
	/** 商品マスタ */
	private NoodleMaster noodleMaster;
	/** 計測時間 */
	private int boilTime;
	/** 計測日時 */
	private Date measureTime;
	/** マスタのJANコード */
	private String janCode; // janCodeの型をintからStringに変更 by leibun date
							// 2010.11.01
	/** マスタの名称 */
	private String name;
	/** マスタの画像イメージ */
	private String imageFileName;

	/**
	 * コンストラクタ
	 * 
	 * @param noodleMaster
	 * @param boilTime
	 * @param measureTime
	 */
	public NoodleHistory(NoodleMaster noodleMaster, int boilTime,
			Date measureTime) {
		this.noodleMaster = noodleMaster;
		this.measureTime = measureTime;
		this.boilTime = boilTime;
		this.janCode = noodleMaster.getJanCode();
		this.name = noodleMaster.getName();
		this.imageFileName = noodleMaster.getImageFileName();
	}

	/**
	 * コンストラクタ
	 * 
	 * @param parcel
	 */
	public NoodleHistory(Parcel parcel) {
		this.noodleMaster = parcel.readParcelable(NoodleMaster.class
				.getClassLoader());
		this.boilTime = parcel.readInt();
		this.measureTime = new Date(parcel.readLong());
		this.janCode = parcel.readString();
		this.name = parcel.readString();
		this.imageFileName = parcel.readString();
	}

	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeParcelable(noodleMaster, 0);
		dest.writeInt(boilTime);
		dest.writeLong(measureTime.getTime());
		dest.writeString(janCode);
		dest.writeString(name);
		dest.writeString(imageFileName);
	}

	public int getBoilTime() {
		return boilTime;
	}

	/**
	 * 茹で時間を文字列（分、秒）で返します
	 * 
	 * @return
	 */
	public String getBoilTimeString(String minString, String secString) {
		DecimalFormat df = new DecimalFormat("0");
		int min = boilTime / 60;
		int sec = boilTime % 60;
		StringBuilder buf = new StringBuilder(df.format(min));
		buf.append(minString);
		df = new DecimalFormat("00");
		buf.append(df.format(sec));
		buf.append(secString);
		return buf.toString();
	}

	/**
	 * 計測時間を文字列で返す
	 * 
	 * @return
	 */
	public String getMeasureTimeString() {
		return getSimpleDateFormat().format(getMeasureTime());
	}

	public static SimpleDateFormat getSimpleDateFormat() {
		return new SimpleDateFormat("yyyyMMddHHmm");
	}

	// ゲッター
	public NoodleMaster getNoodleMaster() {
		return noodleMaster;
	}

	public Date getMeasureTime() {
		return measureTime;
	}

	public String getJanCode() {
		return janCode;
	}

	public String getName() {
		return name;
	}

	public Bitmap getImage() {
		return noodleMaster.getImage();
	}

	public static final Parcelable.Creator<NoodleHistory> CREATOR = new Parcelable.Creator<NoodleHistory>() {
		public NoodleHistory createFromParcel(Parcel in) {
			return new NoodleHistory(in);
		}

		public NoodleHistory[] newArray(int size) {
			return new NoodleHistory[size];
		}
	};

}
