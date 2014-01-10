package ch.gpschase.app.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Contain file utility functions
 */
public class FileUtils {

	/**
	 * 
	 * @param src
	 * @param dst
	 * @throws IOException
	 */
	public static void copy(File src, File dst) throws IOException {
		FileInputStream srcStream = new FileInputStream(src);
		FileOutputStream dstStream = new FileOutputStream(dst);
        FileChannel srcChannel = srcStream.getChannel();
        FileChannel dstChannel = dstStream.getChannel();
        
        dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
        
        srcChannel.close();
        dstChannel.close();
        srcStream.close();
        dstStream.close();
     }
	
}
