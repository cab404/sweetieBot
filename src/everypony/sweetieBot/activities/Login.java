package everypony.sweetieBot.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import com.cab404.libtabun.pages.TabunPage;
import com.cab404.libtabun.parts.User;
import com.cab404.libtabun.parts.UserInfo;
import com.cab404.libtabun.util.SU;
import com.cab404.moonlight.framework.AccessProfile;
import everypony.sweetieBot.U;
import everypony.tabun.auth.TabunAccount;
import everypony.tabun.auth.TabunTokenGetterActivity;

/**
 * Входит за юзера или дает вариант войти readonly.
 */
public class Login extends Activity {

    private static final int TOKEN_REQUEST_CODE = 42;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Пайпим.
        startActivityForResult(new Intent(this, TabunTokenGetterActivity.class), TOKEN_REQUEST_CODE);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 42) {
            if (resultCode == RESULT_OK) {
                final String token = data.getStringExtra(TabunAccount.COOKIE_TOKEN_TYPE);
                U.v(token);
                new AsyncTask<Void, Void, Void>() {
                    @Override protected Void doInBackground(Void... voids) {
                        U.user = new User(SU.sub(token, "PHPSESSID=", ";"));
                        U.user.isLoggedIn = true;

                        AccessProfile profile = AccessProfile.parseString(token);
                        TabunPage page = new TabunPage();
                        page.fetch(profile);


                        U.user_info = new UserInfo(U.user, page.c_inf.username);
                        return null;
                    }
                    @Override protected void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);
                        finish();
                    }
                }.execute();
            }
            if (resultCode == RESULT_CANCELED) {
                new AsyncTask<Void, Void, Void>() {
                    @Override protected Void doInBackground(Void... voids) {
                        U.user = new User();
                        return null;
                    }
                    @Override protected void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);
                        finish();
                    }
                }.execute();
            }
        }
    }
}
