package ch.gpschase.app.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.util.Base64;
import android.util.Log;

public class HashUtils {

	/**
	 * Return the SHA-1 hash of the specified password 
	 * @param password
	 * @return
	 */
	public static String computeSha1Hash(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		MessageDigest mdSha1 = null;
		mdSha1 = MessageDigest.getInstance("SHA-1");
		mdSha1.update(password.getBytes("ASCII"));
		byte[] data = mdSha1.digest();
		return Base64.encodeToString(data, 0, data.length, 0);	
	}
}
