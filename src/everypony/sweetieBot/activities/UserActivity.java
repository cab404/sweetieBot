package everypony.sweetieBot.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.cab404.libtabun.parts.UserInfo;
import everypony.sweetieBot.R;
import everypony.sweetieBot.U;
import everypony.sweetieBot.other.ImageLoader;
import everypony.sweetieBot.wrappers.UserWrapper;

/**
 * Activity с информацией о пользователе.
 */
public class UserActivity extends Activity {
    String extractedNick = "";
    UserInfo info;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.loading);
        U.dumpAll(this);

        if ("PROFILE".equals(getIntent().getAction())) {
            extractedNick = String.valueOf(getIntent().getData());
        } else {
            extractedNick = com.cab404.libtabun.util.SU.sub(String.valueOf(getIntent().getData()), "profile/", "/");
        }

        if (U.user == null) {
            startActivityForResult(new Intent(getApplicationContext(), Login.class), 0);
        } else getter();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (U.user != null) {
            getter();
        } else {
            finish();
        }
    }

    private void getter() {
        new UserWrapper.UserInfoGetter() {
            @Override protected void onPostExecute(UserInfo userInfo) {
                if (userInfo == null) {
                    U.e("Ошибка при получении данных в UserActivity!");
                    finish();
                } else {
                    info = userInfo;
                    afterInit();
                }
            }
        }.execute(extractedNick);
    }

    private void afterInit() {
        setContentView(R.layout.post_layout);

        // Запихиваем всё в header с именем, кармой и другой фигнёй.
        View header = getLayoutInflater().inflate(R.layout.user_label, (ViewGroup) findViewById(R.id.list), false);
        ((TextView) header.findViewById(R.id.nick)).setText(info.nick);
        ((TextView) header.findViewById(R.id.name)).setText(info.name);
        ((TextView) header.findViewById(R.id.strength)).setText(info.strength + "");
        ((TextView) header.findViewById(R.id.votes)).setText(info.votes + "");
        ImageLoader.loadImage(info.big_icon, new ImageLoader.InsertIntoView((ImageView) header.findViewById(R.id.avatar)));
        ((ListView) findViewById(R.id.list)).addHeaderView(header);


        // Запихиваем адаптер на место.
        ((ListView) findViewById(R.id.list)).setAdapter(new UserWrapper.UserInfoWrapper(info));
    }
}