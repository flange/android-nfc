package com.example.nfc_02;

import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity {

	/* UI Elements */
	private static TextView textview_guthaben;
	private static TextView namens_feld;
	
	/* NFC Utilities */
	private static NfcAdapter nfc_adapter;
	private static PendingIntent pending_intent;
	private static IntentFilter ndef;
	private static IntentFilter[] intent_filter;
	private static String[][] tech_lists;
	private static Intent intent;
	
	/* Macros */
	private static final int AUTH_ERROR = 1;
	private static final int READ_ERROR = 2;
	
	private static final int OFFSET = 256;
	
	/* Key of the 'Mensacard' */
	private static byte[] mensa_key = { (byte) 0xa2, (byte) 0x7d, (byte) 0x38, (byte) 0x04, (byte) 0xc2, (byte) 0x59 };
	
	/** Called when the Activity is first created */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		ImageView image = (ImageView) findViewById(R.id.hu_logo);
		
		
		/* initialize global defined stuff */
		textview_guthaben = (TextView) findViewById(R.id.textview_guthaben);
		
		nfc_adapter = NfcAdapter.getDefaultAdapter(this);
		
		pending_intent = PendingIntent.getActivity(this, 0, new Intent(this,
				getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
		
		ndef = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
		
		try {
			ndef.addDataType("*/*");
		} catch (MalformedMimeTypeException e) {
			throw new RuntimeException("fail", e);
		}
		
		intent_filter = new IntentFilter[] { ndef, };
		
		/* Setup a tech list for all NfcF tags */
		tech_lists = new String[][] { new String[] { MifareClassic.class.getName() } };
		
		intent = getIntent();
		
		handleIntent(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		getMenuInflater().inflate(R.menu.activity_main, menu);
		
		return true;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		nfc_adapter.enableForegroundDispatch(this, pending_intent, intent_filter,
				tech_lists);
	}

	@Override
	public void onNewIntent(Intent intent) {
		handleIntent(intent);
	}

	@Override
	public void onPause() {
		super.onPause();
		nfc_adapter.disableForegroundDispatch(this);
	}
	
	public void handleIntent(Intent intent) {
		
		byte[] data;	   	/* buffer for the data read from the mifare card */
		
		boolean auth = false;  	/* used to determine whether auth was successful */
		
		String balance = null; 
		
		Tag intent_tag;		
		
		MifareClassic mfc;
		
		/* parse the intent */
		String intent_action = intent.getAction();
		
		if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent_action)) {
			
			intent_tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			
			mfc = MifareClassic.get(intent_tag);
			
			
			try {
				/* connect to the card and auth for block 13 i.e. sector 3, overall block 12 */
				mfc.connect();
				auth = mfc.authenticateSectorWithKeyA(3, mensa_key);
				
				if (auth) { /* auth successful */
					data = mfc.readBlock(12);
					
					balance = retBalance(data);
					
					if (balance != null) { /* meaningful balance result was returned, print it */
						textview_guthaben.setText("Guthaben: "+balance+" Euro");
					} else {
						showError(READ_ERROR);
					}				
					
				} else {
					showError(AUTH_ERROR);
				}
				
			} catch(IOException e) {
				showError(READ_ERROR);
			}
		} else {
			textview_guthaben.setText("Scan Card");
		}
	}
	
	/* byte 2 und 3 werden vom block gebraucht */
	private String retBalance(byte[] data) {
		
		String ret = null;
		
		double balance;
		int euro, cent;
		
		/* get values from block and convert if neccessary to the right value with the offset of 256 */
		euro = (int) data[2];
		cent = (int) data[3];
		
		if (euro < 0) 
			euro += OFFSET;

		if (cent < 0)
			cent += OFFSET;
		
		
		/* calculate balance */
		balance = euro*255 + cent + euro;
		balance = balance/100;	
		
		ret = String.valueOf(balance);
		
		
		return ret;
		
	}
	
	
	
	private void showError(int err) {
		
		new AlertDialog.Builder(this)
		.setMessage(R.string.err_msg)
		.setNeutralButton(R.string.err_ok, null)
		.show();
		
		return;
	}
	
	
	
	
	
	
	

}
