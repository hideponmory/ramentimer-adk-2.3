package com.androidtsubu.ramentimer;

import android.app.AlertDialog;
import android.content.Context;

/**
 * カスタムダイアログ
 * 
 * @author otori
 * 
 */
public class CustomAlertDialog extends AlertDialog {

	protected CustomAlertDialog(Context context) {
		super(context);
	}

	protected CustomAlertDialog(Context context, int theme) {
		super(context, theme);
	}

	protected CustomAlertDialog(Context context, boolean cancelable,
			OnCancelListener cancelListener) {
		super(context, cancelable, cancelListener);
	}
}
