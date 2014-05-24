package everypony.sweetieBot.activities;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import everypony.sweetieBot.bus.B;

/**
 * Abstract sweetieBot activity.
 *
 * @author cab404
 */
public class SweetieBotActivity extends Activity {

    protected String tag() {
        return this.getClass().getSimpleName();
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            B.register(this);
            Log.v(tag(), "Registered event listener");
        } catch (RuntimeException e) {
            Log.v(tag(), "No event listeners found");
        }

    }

    @Override protected void onDestroy() {
        super.onDestroy();

        B.unregister(this);
        Log.v(tag(), "Unregistered event listener");

    }
}
