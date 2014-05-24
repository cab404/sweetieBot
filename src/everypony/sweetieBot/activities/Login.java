package everypony.sweetieBot.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import com.cab404.libtabun.pages.ProfilePage;
import com.cab404.libtabun.pages.TabunPage;
import com.cab404.libtabun.util.TabunAccessProfile;
import everypony.sweetieBot.U;
import everypony.sweetieBot.bus.B;
import everypony.sweetieBot.bus.Bus;
import everypony.sweetieBot.bus.events.LoginStatus;

/**
 * Входит за юзера или дает вариант войти readonly.
 */
public class Login extends SweetieBotActivity {

    private static final int TOKEN_REQUEST_CODE = 42;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Пайпим.
        try {
            startActivityForResult(new Intent("everypony.tabun.auth.TOKEN_REQUEST"), TOKEN_REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            B.post(LoginStatus.UNABLE);
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

                        U.c_inf = profile.c_inf;

                        Log.v(tag(), U.c_inf.username);

                        return null;

                    }

                    @Override protected void onPostExecute(Void aVoid) {
                        B.post(LoginStatus.SUCCESS);
                    }

                }.execute();

            }

            if (resultCode == RESULT_CANCELED) {
                U.user = new TabunAccessProfile();
                B.post(LoginStatus.FAILURE);
            }
        }
    }

    @Bus.Handler
    public void login(LoginStatus status) {
        switch (status) {
            case UNABLE:
                Intent download = new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=everypony.tabun.auth")
                );
                startActivity(download);
                setResult(RESULT_CANCELED);
            case SUCCESS:
            case FAILURE:
                finish();
        }
    }

}
