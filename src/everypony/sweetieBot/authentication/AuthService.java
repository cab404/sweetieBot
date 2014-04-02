package everypony.sweetieBot.authentication;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Сервис аутентификации.
 *
 * @author cab404
 */
public class AuthService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        TabunUserAuth auth = new TabunUserAuth(this);
        return auth.getIBinder();
    }
}
