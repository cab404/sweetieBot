package everypony.sweetieBot.authentication;

import android.accounts.*;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import com.cab404.libtabun.parts.User;
import everypony.sweetieBot.U;
import everypony.sweetieBot.activities.Authentication;

/**
 * Аутентификатор пользователя Табуна.
 *
 * @author cab404
 */
public class TabunUserAuth extends AbstractAccountAuthenticator {

    // Да, вот такой вот тип аккаунта.
    public static final String TYPE = "tabun.everypony";

    public TabunUserAuth(Context context) {
        super(context);
        if (U.context == null)
            U.context = context;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse accountAuthenticatorResponse, String s) {
        // Пока не нать.
        return null;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse aar, String accType, String authTokenType, String[] requiredFeatures, Bundle bundle) throws NetworkErrorException {
        // authTokenType, что бы у нас не спросили - Печеньки. Ну а там Орхид решит.

        final Intent intent = new Intent(U.context, Authentication.class);

        // Сбснна, обычно accType у нас равен TabunUserAuth.TYPE. Точнее всегда.
        intent.putExtra(Authentication.ARG_TYPE, accType);

        // aar, если я что-нибудь в чём-нибудь пони, собрался после Authentication. При помощи магии аликорнов.
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, aar);

        bundle = bundle == null ? new Bundle() : bundle;
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);

        return bundle;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, Bundle bundle) throws NetworkErrorException {
        // Тоже пока нафиг.
        return null;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse aar, Account account, String tokenType, Bundle opt) throws NetworkErrorException {
        // Не так нужно, но пусть живёт. Вдруг пригодится.
        // Делает юзера, достаёт печеньки. Ничего особенного.

        AccountManager man = AccountManager.get(U.context);
        String token = man.peekAuthToken(account, TYPE);
        User user;

        if (TextUtils.isEmpty(token)) {
            String password = man.getPassword(account);
            if (!password.isEmpty()) {
                user = new User(account.name, password);
                if (user.isLoggedIn()) {
                    token = user.getSessionID();
                }
            }
        }

        if (!TextUtils.isEmpty(token)) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            result.putString(AccountManager.KEY_AUTHTOKEN, token);
            return result;
        }

        final Intent intent = new Intent(U.context, Authentication.class);

        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, aar);
        intent.putExtra(Authentication.ARG_TYPE, account.type);

        opt = opt == null ? new Bundle() : opt;
        opt.putParcelable(AccountManager.KEY_INTENT, intent);
        return opt;
    }

    @Override
    public String getAuthTokenLabel(String s) {
        // Да, пока всегда Печенье :D
        return "Печенье";
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, String s, Bundle bundle) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, String[] strings) throws NetworkErrorException {
        return null;
    }
}