package everypony.sweetieBot.activities;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import com.cab404.libtabun.pages.TabunPage;
import com.cab404.libtabun.parts.User;
import com.cab404.libtabun.parts.UserInfo;
import com.cab404.libtabun.util.SU;
import com.cab404.moonlight.framework.AccessProfile;
import everypony.sweetieBot.U;

/**
 * Входит за юзера или дает вариант войти readonly.
 */
public class Login extends Activity {

    private static final int TOKEN_REQUEST_CODE = 42;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Пайпим.
        try {
            startActivityForResult(new Intent("everypony.tabun.auth.TOKEN_REQUEST"), TOKEN_REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            Intent download = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=everypony.tabun.auth")
            );
            startActivity(download);
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 42) {
            if (resultCode == RESULT_OK) {
                final String token = data.getStringExtra("everypony.tabun.cookie");
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
