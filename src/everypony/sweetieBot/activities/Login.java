package everypony.sweetieBot.activities;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import com.cab404.libtabun.pages.ProfilePage;
import com.cab404.libtabun.pages.TabunPage;
import com.cab404.libtabun.util.TabunAccessProfile;
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

                        U.user = TabunAccessProfile.parseString(token);

                        TabunPage page = new TabunPage();
                        page.fetch(U.user);

                        ProfilePage profile = new ProfilePage(page.c_inf.username);
                        profile.fetch(U.user);

                        U.c_inf = page.c_inf;

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
                        U.user = new TabunAccessProfile();
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
