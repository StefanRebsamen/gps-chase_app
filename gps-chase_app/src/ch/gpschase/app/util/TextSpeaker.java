package ch.gpschase.app.util;

import java.util.Locale;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

/**
 * Encapsulated TextToSpeech details
 */
public class TextSpeaker implements TextToSpeech.OnInitListener {

	// TextToSpeech instance
	private TextToSpeech tts;		

	// flag to indicate if TTS is available
	private boolean available;
	
	/**
	 * Interface for callbacks
	 */
	public interface Listener {
		void onSpeakerInitialized(boolean available);
	}

	private Listener listener = null;
	
	/**
	 * Constructor
	 */
	public TextSpeaker(Context context, Listener listener) {
		this.listener = listener;
		
		tts = new TextToSpeech(context, this);
	}
	
	/**
	 * Should be called to shutdown TTS properly
	 */
	public void shutdown() {
        // shutdown TTS
        tts.stop();
        tts.shutdown();
	}
	
	/**
	 * Callback from TTS
	 */
	public void onInit(int status) {
		if (status == TextToSpeech.SUCCESS) {
			int result = tts.setLanguage(Locale.getDefault());
			if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
				Log.e("EditTrailActivity", "This language is not supported");
			}
			else {
				available = true;
			}
		} 
		else {
			Log.e("EditTrailActivity", "Initilization failed!");
		}

		// callback
		if (listener != null) {
			listener.onSpeakerInitialized(available);
		}
	}
	
	/**
	 * Returns if TTS is available
	 * @return True if available
	 */
	public boolean isAvailable() {
		return available;
	}
	
	/**
	 * Speaks the passed text immediately
	 * @param text
	 */
	public void speak(String text) {
		tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
	}
	
}
