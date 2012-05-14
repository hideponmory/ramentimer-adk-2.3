package com.androidtsubu.ramentimer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

/**
 * ラーメン商品マスタです
 * 
 * @author hide
 */
public class NoodleMaster implements Parcelable {
	/** JANコード */
	private String janCode; // janCodeの型をintからStringに変更 by leibun date
							// 2010.11.01
	/** 名前 */
	private String name;
	/** 画像イメージファイル名 */
	private String imageFileName;
//	/**画像イメージ*/
//	private Bitmap image = null;

	/** ゆで時間 */
	private int timerLimit;

	/**
	 * コンストラクタ
	 * 
	 * @param janCode
	 * @param name
	 * @param image
	 * @param timerLimit
	 * @param noodleType
	 */
	public NoodleMaster(String janCode, String name, Bitmap image,
			int timerLimit) {
		this.janCode = janCode;
		this.name = name;
		setImage(image);
		this.timerLimit = timerLimit;
	}

	/**
	 * コンストラクタ
	 * 
	 * @param janCode
	 * @param name
	 * @param imagePath
	 * @param timerLimit
	 */
	public NoodleMaster(String janCode, String name, String imagePath,
			int timerLimit) {
		this.janCode = janCode;
		this.name = name;
		this.imageFileName = imagePath;
		this.timerLimit = timerLimit;
	}

	/**
	 * コンストラクタ
	 * 
	 * @param parcel
	 */
	public NoodleMaster(Parcel parcel) {
		this.janCode = parcel.readString();
		this.name = parcel.readString();
		this.imageFileName = parcel.readString();
		this.timerLimit = parcel.readInt();
	}

	public String getJanCode() {
		return janCode;
	}

	public String getName() {
		return name;
	}

	public Bitmap getImage() {

		FileInputStream in = null;
		if(imageFileName == null || imageFileName.equals("")){
			return null;
			//return BitmapFactory.decodeResource(.getResources(), R.drawable.img_ramen_noimage);;
		}
		try {
			// パス名からファイルのInputStreamを生成しBitmapにする。
			// ファイルが見つからなかった場合はそのままnullが入る
			File file = new File(NoodleManager.SAVE_IMAGE_DIRECTORY,
					imageFileName);
			in = new FileInputStream(file);
			return BitmapFactory.decodeStream(in);
		} catch (FileNotFoundException e) {
			// ファイルが見つからなかった
			// TODO Auto-generated catch block
			Log.d("ramentimerbug", ExceptionToStringConverter.convert(e));
			return null;
		} catch (Exception e) {
			Log.d("ramentimerbug", ExceptionToStringConverter.convert(e));
			return null;
		}finally{
			if(in != null){
				try {
					in.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public void setImage(Bitmap image) {
		// バーコードをファイル名としてファイルを作成する
		imageFileName = getJanCode() + ".jpg";
//		//画像も保持しておく
//		this.image = image;
		createImageFile(imageFileName, image);
	}

	/**
	 * Imageをfileにします
	 * 
	 * @param filename
	 * @param bitmap
	 */
	private void createImageFile(String filename, Bitmap bitmap) {
		// jpgファイルを作る
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		FileOutputStream fileOutputStream = null;
		try {
			bitmap.compress(CompressFormat.JPEG, 100, bos);
			// ファイルを書き出す
			File file = new File(NoodleManager.SAVE_IMAGE_DIRECTORY, filename);
			fileOutputStream = new FileOutputStream(file);
			fileOutputStream.write(bos.toByteArray());
			fileOutputStream.flush();
		} catch (FileNotFoundException e) {
			Log.d("ramentimerbug", ExceptionToStringConverter.convert(e));
		} catch (IOException e) {
			Log.d("ramentimerbug", ExceptionToStringConverter.convert(e));
		} finally {
			if (fileOutputStream != null) {
				try {
					fileOutputStream.close();
				} catch (IOException e) {
				}
			}
			if(bos != null){
				try {
					bos.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Imageファイル名を返す
	 * 
	 * @return
	 */
	public String getImageFileName() {
		return imageFileName;
	}

	public int getTimerLimit() {
		return timerLimit;
	}

	public String getTimerLimitString(String minString, String secString) {
		DecimalFormat df = new DecimalFormat("0");
		int min = getTimerLimit() / 60;
		int sec = getTimerLimit() % 60;
		StringBuilder buf = new StringBuilder(df.format(min));
		buf.append(minString);
		df = new DecimalFormat("00");
		buf.append(df.format(sec));
		buf.append(secString);

		return buf.toString();
	}

	/**
	 * 完全なデータかどうかを返す
	 * 
	 * @return
	 */
	public boolean isCompleteData() {
		if (janCode == null || janCode.equals("")) {
			return false;
		}
		if (name == null || name.equals("")) {
			return false;
		}
		if (imageFileName == null || imageFileName.equals("")) {
			return false;
		}
		if (timerLimit <= 0) {
			return false;
		}
		return true;
	}

	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void writeToParcel(Parcel dest, int arg1) {
		dest.writeString(janCode);
		dest.writeString(name);
		dest.writeString(imageFileName);
		dest.writeInt(timerLimit);
	}

	public static final Parcelable.Creator<NoodleMaster> CREATOR = new Parcelable.Creator<NoodleMaster>() {
		public NoodleMaster createFromParcel(Parcel in) {
			return new NoodleMaster(in);
		}

		public NoodleMaster[] newArray(int size) {
			return new NoodleMaster[size];
		}
	};
}
