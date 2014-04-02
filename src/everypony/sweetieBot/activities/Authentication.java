package everypony.sweetieBot.activities;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import com.cab404.libtabun.parts.User;
import everypony.sweetieBot.R;
import everypony.sweetieBot.U;
import everypony.sweetieBot.authentication.TabunUserAuth;
import everypony.sweetieBot.wrappers.UserWrapper;

import java.util.ArrayList;

/**
 * Классный Login Activity. Этот bg меня умиляет.
 * В Intent под ключём wanna_go_back можно проставить,
 * будет ли Authentication возвращатся в Home
 *
 * @author cab404
 */
public class Authentication extends Activity {

    private String[] error;

    public static final String ARG_TYPE = "type";
    private static final int REQ_SIGN_UP = 1;

    /**
     * Отключает/включает окошки ввода и включает/отключает ProgressBar
     *
     * @param really Первый ли вариант?
     */
    public void setLoggingIn(boolean really) {
        EditText login_edit = ((EditText) findViewById(R.id.login_login));
        EditText pwd_edit = ((EditText) findViewById(R.id.login_pwd));
        login_edit.setEnabled(!really);
        pwd_edit.setEnabled(!really);
        findViewById(R.id.login_progress).setVisibility(really ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        U.dumpAll(this);

        // Включаем окошки, прячем ActionBar.
        setContentView(R.layout.login_screen);
        setLoggingIn(false);
        error = getResources().getStringArray(R.array.error_on_login);
        ((EditText) findViewById(R.id.login_pwd)).setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                // 6 - потому, что enter. 0 - hardware enter
                if (i == 6 || i == 0) {

                    // Достаём логины / пароли, выключаем окошки ввода.
                    EditText login_edit = ((EditText) findViewById(R.id.login_login));
                    EditText pwd_edit = ((EditText) findViewById(R.id.login_pwd));

                    String login = login_edit.getText().toString();
                    String pwd = pwd_edit.getText().toString();

                    setLoggingIn(true);

                    // Отсылаем логины / пароли проверятся.
                    new AsyncTask<String, Void, Intent>() {

                        @Override
                        protected Intent doInBackground(String... data) {
                            Intent res = new Intent();
                            if (data[0].contains("@")) return null;
                            User user;
                            try {
                                user = new User(data[0], data[1]);
                            } catch (NullPointerException ex) {
                                return null;
                            }

                            if (user.isLoggedIn()) {
                                // Если проверились, делаем аккаунт и шлём в onPostExecute
                                res.putExtra(AccountManager.KEY_ACCOUNT_NAME, data[0]);
                                res.putExtra(AccountManager.KEY_ACCOUNT_TYPE, TabunUserAuth.TYPE);
                                res.putExtra(AccountManager.KEY_AUTHTOKEN, user.getSessionID());
                                res.putExtra("password", data[1]);

                                if (data[2].equals("true"))
                                    res.putExtra("wanna_go_back", true);

                                return res;
                            } else {
                                // А если нет - то шлём null.
                                return null;
                            }
                        }

                        @Override
                        protected void onPostExecute(Intent intent) {
                            if (intent == null) {
                                // Если словили null - даём юзеру второй (третий, десятый) шанс.
                                setLoggingIn(false);
                                ((TextView) findViewById(R.id.status)).setText(U.getRandomEntry(error));
                            } else {
                                // Иначе - отправляем данные дальше по конвейру.
                                finishLogin(intent);
                            }
                        }
                    }.execute(login, pwd, getIntent().getBooleanExtra("wanna_go_back", false) + "");

                    return true;
                }
                return false;
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_SIGN_UP && resultCode == RESULT_OK) {
            finishLogin(data);
        } else
            super.onActivityResult(requestCode, resultCode, data);
    }

    @Override protected void onDestroy() {
        setResult(1);
        super.onDestroy();
    }

    private void finishLogin(Intent intent) {
        // Уря, всё прошло успешно, радость, счастье. //

        ((TextView) findViewById(R.id.status)).setText("Yay!");
        boolean goingBack = intent.getBooleanExtra("wanna_go_back", false);
        intent.removeExtra("wanna_go_back");

        AccountManager man = AccountManager.get(getBaseContext());
        String accType = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
        String cookies = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN);
        String pwd = intent.getStringExtra("password");

        // Больше аккаунтов!
        Account account = new Account(accType, TabunUserAuth.TYPE);
        // Там где сейчас null можно пихнуть данных о пользователе
        man.addAccountExplicitly(account, pwd, null);
        // Действительно.
        man.setAuthToken(account, "Печенье", cookies);
        // Не уверен, нужен ли он тут. Вроде выше уже проставил. Лааадно, хуже не будет.
        man.setPassword(account, pwd);

        setResult(RESULT_OK, intent);
        finish();

        // Кстати, можно запустить MainView!
        if (goingBack) {
            Intent run_main = new Intent(this, Home.class);
            startActivity(run_main);
        }
    }

    /**
     * Возвращает список логинов и паролей всех зарегестрированных аккаунтов.
     * Отправлять при первой же возможностиэтот список в dev/null,
     * нафиг надо нам пароли в памяти открытым текстом.
     */
    public static ArrayList<UserWrapper.UserAuthenticator.AuthData> getAccountList(Context context) {
        ArrayList<UserWrapper.UserAuthenticator.AuthData> logins = new ArrayList<>();
        AccountManager man = AccountManager.get(context);
        for (Account account : man.getAccountsByType(TabunUserAuth.TYPE)) {
            logins.add(new UserWrapper.UserAuthenticator.AuthData(account.name, man.getPassword(account)));
        }
        return logins;
    }

    /**
     * Запускает создание аккаунта, после чего возвращается обратно
     */
    public static void startAccountCreation(Activity source) {
        Intent new_account = new Intent(source, Authentication.class);
        new_account.putExtra("wanna_go_back", true);
        source.startActivity(new_account);
        source.finish();
    }
}
