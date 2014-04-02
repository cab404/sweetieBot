package everypony.sweetieBot.activities;

import android.app.Activity;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import com.cab404.libtabun.parts.User;
import everypony.sweetieBot.R;
import everypony.sweetieBot.U;
import everypony.sweetieBot.wrappers.UserWrapper;

import java.util.ArrayList;

/**
 * Входит за юзера или дает вариант войти readonly.
 */
public class Login extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(android.R.style.Theme_Holo_Dialog);
        setContentView(R.layout.account_list);

        // Проверяем Интернеты
        ConnectivityManager activeNetworkInfo = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (activeNetworkInfo.getActiveNetworkInfo() == null || !activeNetworkInfo.getActiveNetworkInfo().isAvailable()) {
            U.showOkToast(
                    "Нет подключения к сети!",
                    "Приложение не может работать без Интернета."
                    , this);
            setResult(1);
            finish();
            return;
        }

        {
            // Достаём аккаунт. Делаем это в отдельном блоке,
            // дабы отправить открытый текст в небытие поскорее.
            // Итак security breach-ей не сосчитать.
            ArrayList<UserWrapper.UserAuthenticator.AuthData> accounts = Authentication.getAccountList(this);
            if (accounts.size() > 0)
                new UserWrapper.UserAuthenticator() {
                    @Override public void onPostExecute(User auth) {
                        U.user = auth;
                        if (U.user != null && U.user.isLoggedIn()) {
                            ok();
                        } else {
                            network_error();
                        }
                    }
                }.execute(accounts.get(0));
            else {
                setContentView(R.layout.login_type);
                final Activity translate_this = this;

                findViewById(R.id.login).setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        Authentication.startAccountCreation(translate_this);
                        setResult(2);
                        finish();
                    }
                });

                findViewById(R.id.readonly).setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        new AsyncTask<Void, Void, User>() {
                            @Override protected User doInBackground(Void... params) {
                                try {
                                    U.user = new User();
                                } catch (NullPointerException ex) {
                                    return null;
                                }
                                return U.user;
                            }

                            @Override protected void onPostExecute(User user) {
                                if (user == null) network_error();
                                else ok();
                            }
                        }.execute();
                        setContentView(R.layout.account_list);
                    }
                });
            }
        }
        // Перестрахуемся.
        System.gc();
    }

    private void network_error() {
        U.showOkToast(
                "Не удалось подключится.",
                "Если вы меняли пароль - удалите аккаунт и создайте заново. \n" +
                        "Но скорее всего, Табун упал. \n" +
                        "С ним иногда бывает, но его починят.\n" +
                        "Еще иногда такое бывает, когда на двух компьютерах входят одновременно.\n" +
                        "А еще, может быть, просто интернет слишком медленный.\n" +
                        "И вообще, что я тут вам рассказываю?\n"
                , getApplicationContext());
        setResult(1);
        finish();
    }

    private void ok() {
        U.showOkToast(
                "Подключение установлено!",
                U.user.getLogin()
                , getApplicationContext());
        setResult(0);
        finish();
    }
}
