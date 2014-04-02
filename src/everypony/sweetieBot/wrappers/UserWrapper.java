package everypony.sweetieBot.wrappers;

import android.os.AsyncTask;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.cab404.libtabun.parts.User;
import com.cab404.libtabun.parts.UserInfo;
import everypony.sweetieBot.R;
import everypony.sweetieBot.U;

import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Класс взаимодействия Android и класса User из libtabun.
 *
 * @author cab404
 */
public class UserWrapper {

    public static abstract class UserAuthenticator extends AsyncTask<UserAuthenticator.AuthData, Void, User> {

        /**
         * Содержит логин и пароль пользователя, класс для удобства запихивания аккаунтов в массивы.
         */
        public static class AuthData {
            private String login, password;

            /**
             * Возвращает логин пользователя.
             */
            public String getLogin() {
                return login;
            }

            public AuthData(String login, String password) {
                this.login = login;
                this.password = password;
            }
        }

        @Override
        protected User doInBackground(AuthData... params) {
            try {
                if (params[0].login.contains("@")) return null; // Шлём юзеров с e-mail в круп.
                User user = new User(params[0].login, params[0].password);
                U.user_info = new UserInfo(user);
                return user;
            } catch (NullPointerException ex) {
                U.e(ex);
                return null;
            }
        }

        /**
         * Что делать, кода юзера нашли?
         *
         * @param user Юзер, которого нашли и аутентифицировали. Или null, если не нашли.
         */
        @Override public abstract void onPostExecute(User user);
    }

    /**
     * Доставалка информации о юзере.
     */
    public static abstract class UserInfoGetter extends AsyncTask<String, Void, UserInfo> {
        @Override protected UserInfo doInBackground(String... params) {
            try {
                return new UserInfo(U.user, params[0]);
            } catch (Throwable t) {
                U.e(t);
                return null;
            }
        }
    }

    /**
     * Страничка с данными пользователя
     */
    public static class UserInfoWrapper extends U.FixedAdapter {
        List<Map.Entry<String, String>> info_list;

        public UserInfoWrapper(UserInfo info) {
            info_list = new Vector<>();

            for (Map.Entry<String, String> entry : info.personal) {
                info_list.add(entry);
            }
            for (Map.Entry<String, String> entry : info.contacts) {
                info_list.add(entry);
            }
        }

        @Override public int getCount() {
            return info_list.size();
        }

        @Override public Object getItem(int position) {
            return info_list.get(position);
        }

        @Override public long getItemId(int position) {
            return info_list.get(position).hashCode();
        }

        @Override public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null)
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.about_entry, parent, false);

            convertView(convertView, info_list.get(position));
            return convertView;
        }

        /**
         * Превращает брюки в элегантные шорты.
         */
        private void convertView(View view, Map.Entry<String, String> kv) {
            TextView key = (TextView) view.findViewById(R.id.key);
            TextView value = (TextView) view.findViewById(R.id.value);

            key.setText(kv.getKey());
            value.setText(Html.fromHtml(kv.getValue().replace("\t", "")));
            value.setMovementMethod(TextWrapper.TheBrandNewCoolMovementMethod.getInstance());
        }
    }
}
