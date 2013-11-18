package com.example.androidnfcreaderdemo;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;

public class ReaderDemoActivity extends Activity {

	public static final String MIME_TEXT_PLAIN = "text/plain";
	public static final String MIME_MEDIA = "application/vnd.com.example.android.beam";
	public static final String TAG = "NfcDemo";

	private TextView mTextView;
	private NfcAdapter mNfcAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.reader);
		
		Log.i("RRRRRRRRRRRRRRRRRRRRRRR", "this is new idea: ");

		mTextView = (TextView) findViewById(R.id.nfcData);

		mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

		if (mNfcAdapter == null) {
			// Stop here, we definitely need NFC
			Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
			finish();
			return;
		}
	
		if (!mNfcAdapter.isEnabled()) {
			mTextView.setText("NFC is disabled.");
		} else {
			mTextView.setText("explanation");
		}
		
		handleIntent(getIntent());
	}
	
	@Override
	protected void onResume() 
	{
		super.onResume();
	
		setupForegroundDispatch(this, mNfcAdapter);
	}
	
	@Override
	protected void onPause() 
	{
		super.onPause();
		stopForegroundDispatch(this, mNfcAdapter);
	}
	
	@Override
	protected void onNewIntent(Intent intent) 
	{
		handleIntent(intent);
	}
	
	private void handleIntent(Intent intent) 
	{
		String action = intent.getAction();
		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
			
			String type = intent.getType();
			if (MIME_TEXT_PLAIN.equals(type)) {

				Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
				//new NdefReaderTask().execute(tag);
				setViewText(ndefmessage(tag));
				
			} else {
				Log.d(TAG, "Wrong mime type: " + type);
			}
			if(MIME_MEDIA.equals(type))
			{
				Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
				Ndef ndef = Ndef.get(tag);
				
				NdefMessage ndefMessage = ndef.getCachedNdefMessage();

				NdefRecord[] records = ndefMessage.getRecords();
				for (NdefRecord ndefRecord : records) {
					
					byte[] payload = ndefRecord.getPayload();
					String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
					int languageCodeLength = payload[0] & 0063;
					
					try {
						String value = new String(payload, 0, payload.length , textEncoding);
						setViewText(value);
						Log.i(TAG, "mime tag: " + value);
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					if (ndefRecord.getTnf() == NdefRecord.TNF_MIME_MEDIA && Arrays.equals(ndefRecord.getType(), "application/vnd.com.example.android.beam".getBytes(Charset.forName("US-ASCII")))) {
						//Log.i(TAG, "mime tag: " + ndefRecord);
					}
				}
				//Log.i(TAG, "mime tag: " + ndef.getCachedNdefMessage().getRecords());
			}
		} else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
			
			// In case we would still use the Tech Discovered Intent
			Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			String[] techList = tag.getTechList();
			String searchedTech = Ndef.class.getName();
			
			for (String tech : techList) {
				if (searchedTech.equals(tech)) {
					//new NdefReaderTask().execute(tag);
					ndefmessage(tag);
					break;
				}
			}
		}
	}


	public static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {
		final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
		intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

		final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);

		IntentFilter[] filters = new IntentFilter[1];
		String[][] techList = new String[][]{};

		// Notice that this is the same filter as in our manifest.
		filters[0] = new IntentFilter();
		filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
		filters[0].addCategory(Intent.CATEGORY_DEFAULT);
		try {
			filters[0].addDataType(MIME_TEXT_PLAIN);
		} catch (MalformedMimeTypeException e) {
			throw new RuntimeException("Check your mime type.");
		}
		
		adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
	}

	
	public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
		adapter.disableForegroundDispatch(activity);
	}
	
	private String ndefmessage(Tag tag)
	{
		Tag tag1 = tag;
		
		Ndef ndef = Ndef.get(tag);
		if (ndef == null) {
			// NDEF is not supported by this Tag. 
			return null;
		}

		NdefMessage ndefMessage = ndef.getCachedNdefMessage();

		NdefRecord[] records = ndefMessage.getRecords();
		for (NdefRecord ndefRecord : records) {
			if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
				try {
					return readText1(ndefRecord);
				} catch (UnsupportedEncodingException e) {
					Log.e(TAG, "Unsupported Encoding", e);
				}
			}
		}

		return null;
	}
	
	private String readText1(NdefRecord record) throws UnsupportedEncodingException 
	{
		byte[] payload = record.getPayload();
		String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
		int languageCodeLength = payload[0] & 0063;
		
		return new String(payload, 0, payload.length , textEncoding);
	}
	
	private void setViewText(String result)
	{
		mTextView.setText("Read content: " + result);
	}
}

