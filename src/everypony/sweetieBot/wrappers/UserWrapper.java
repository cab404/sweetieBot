package everypony.sweetieBot.wrappers;

import android.os.AsyncTask;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.cab404.libtabun.data.Profile;
import com.cab404.libtabun.pages.ProfilePage;
import everypony.sweetieBot.R;
import everypony.sweetieBot.U;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Класс взаимодействия Android и класса User из libtabun.
 *
 * @author cab404
 */
public class UserWrapper {

    /**
     * Доставалка информации о юзере.
     */
    public static abstract class UserInfoGetter extends AsyncTask<String, Void, Profile> {
        @Override protected Profile doInBackground(String... params) {
            try {
                ProfilePage page = new ProfilePage(params[0]);
                page.fetch(U.user);

                return page.user_info;
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

        public UserInfoWrapper(Profile info) {
            info_list = new Vector<>();

            for (Map.Entry<Profile.UserInfoType, String> entry : info.personal.entrySet()) {
                info_list.add(new AbstractMap.SimpleEntry<>(entry.getKey().name, entry.getValue()));
            }
            for (Map.Entry<Profile.ContactType, String> entry : info.contacts.entrySet()) {
                info_list.add(new AbstractMap.SimpleEntry<>(entry.getKey().name, entry.getValue()));
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
