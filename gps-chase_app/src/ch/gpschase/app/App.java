package ch.gpschase.app;

import ch.gpschase.app.data.ImageManager;
import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.os.Debug;

public class App extends Application {

    //sensible place to declare a log tag for the application
    public static final String LOG_TAG = "myapp";

    //instance 
    private static App instance = null;

    //keep references to our global resources
    private static ImageManager imageManager = null;

    
    /**
     * 
     */
    public static ImageManager getImageManager() {
        if (imageManager == null) {
            if (instance == null)
                throw new IllegalStateException("Application not created yet!");
            imageManager = new ImageManager(instance);
        }
        return imageManager;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        //provide an instance for our static accessors
        instance = this;
    }

    
    public static boolean isDebuggable() {
        if (instance == null)
            throw new IllegalStateException("Application not created yet!");
        return Debug.isDebuggerConnected();
    }
  
}