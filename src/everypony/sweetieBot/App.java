package everypony.sweetieBot;

import android.app.Application;
import everypony.sweetieBot.bus.B;
import everypony.sweetieBot.bus.dispatchers.Poster;
import everypony.sweetieBot.bus.dispatchers.Voter;

/**
 * @author cab404
 */
public class App extends Application {


    @Override public void onCreate() {
        super.onCreate();

        B.register(new Poster());
        B.register(new Voter());
    }


}
