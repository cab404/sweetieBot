package everypony.sweetieBot;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.os.Build;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.cab404.libtabun.parts.User;
import com.cab404.libtabun.parts.UserInfo;
import everypony.sweetieBot.other.ImageLoader;

import java.io.*;

/**
 * U is for the utils, that's good enough for me!
 * Куча статичных методов и переменных.
 *
 * @author cab404
 */
public class U {
    public static Activity current;
    public static Context context;
    public static Resources res;
    public static File cacheDir, files;
    public static User user;
    public static final int SDK = Build.VERSION.SDK_INT;
    /**
     * Информация о текущем пользователе
     * #NSA #lol
     */
    public static UserInfo user_info;

    /**
     * Скидывает данные о Activity в статичное хранилище, дабы ими можно было воспользоваться
     */
    public static void dumpAll(Activity activity) {
        current = activity;
        context = activity.getApplicationContext();
        cacheDir = activity.getCacheDir();
        res = activity.getResources();
        files = activity.getFilesDir();
        ImageLoader.lim_x = res.getDisplayMetrics().widthPixels - U.dp(32);
//        MessageFactory.impl = new MessageListenerImpl();
    }

    public static int dp(float conv) {
        return (int) (U.res.getDisplayMetrics().density * conv);
    }

//    public static class MessageListenerImpl implements MessageFactory.MessageListener {
//
//        @Override public void show(final String header, final String text, boolean isError) {
//            new AsyncTask<Void, Void, Void>() {
//                @Override protected Void doInBackground(Void... params) {
//
//                    return null;
//                }
//
//                @Override protected void onPostExecute(Void aVoid) {
//                    try {
//                        if (context != null)
//                            showOkToast(header, text, context);
//                    } catch (Throwable t) {
//                        // Да, не ловлю.
//                    }
//                }
//            }.execute();
//        }
//    }

    /**
     * Делает абсолютно то же, что и Log.v("Luna Log", obj.toString()),
     * плюс проверяет на null.
     */
    public static void v(Object obj) {
        try {
            Log.v("Luna Log", obj == null ? null : obj.toString());
        } catch (NullPointerException e) {
            w(e);
        }
    }

    /**
     * Делает абсолютно то же, что и Log.e("Luna Log", obj.toString()),
     * плюс проверяет на null.
     */
    public static void e(Object obj) {
        Log.e("Luna Log", obj == null ? null : obj.toString());
    }

    /**
     * Делает абсолютно то же, что и Log.e("Luna Log", obj.toString()),
     * плюс проверяет на null.
     */
    public static void e(Throwable obj) {
        StringWriter writer = new StringWriter();
        PrintWriter out = new PrintWriter(writer);
        if (obj != null) obj.printStackTrace(out);
        Log.e("Luna Log", writer.toString());
    }

    /**
     * Делает абсолютно то же, что и Log.w("Luna Log", obj.toString()),
     * плюс проверяет на null.
     */
    public static void w(Object obj) {
        Log.w("Luna Log", obj == null ? null : obj.toString());
    }

    /**
     * Делает абсолютно то же, что и Log.w("Luna Log", obj.toString()),
     * плюс проверяет на null.
     */
    public static void w(Throwable obj) {
        StringWriter writer = new StringWriter();
        PrintWriter out = new PrintWriter(writer);
        if (obj != null) obj.printStackTrace(out);
        Log.w("Luna Log", writer.toString());
    }

//    public static void v(HttpRequestBase request) {
//        v(request.getRequestLine());
//        for (Header header : request.getAllHeaders())
//            v(header.getName() + ": " + header.getValue());
//        v("");
//    }

    /**
     * Делает абсолютно то же, что и Log.wtf("Luna Log", obj.toString()),
     * плюс проверяет на null.
     */
    public static void wtf(Object obj) {
        Log.wtf("Luna Log", obj == null ? null : obj.toString());
    }

    /**
     * Достаёт рандомный элемент из массива. И всё :D
     */
    public static <T> T getRandomEntry(T[] values) {
        return values[(int) Math.floor(Math.random() * values.length)];
    }

    /**
     * Отрисовывает текст на две секунды в классной рамке
     */
    public static void showOkToast(String header, String text, Context context) {
        View toast = LayoutInflater.from(context).inflate(R.layout.ok_toast, null);
        ((TextView) toast.findViewById(R.id.title)).setText(header);
        ((TextView) toast.findViewById(R.id.text)).setText(text);

        Toast notif = Toast.makeText(context, "", Toast.LENGTH_LONG);
        notif.setView(toast);
        notif.show();
    }

    /**
     * Тот же BaseAdapter, только с профикшенным unregisterDataSetObserver
     */
    public static abstract class FixedAdapter extends BaseAdapter {

        @Override public void unregisterDataSetObserver(DataSetObserver observer) {
            if (observer != null)
                super.unregisterDataSetObserver(observer);
        }
    }

//    public static class ImageLoadingWrapper implements Html.ImageGetter {
//        @Override public Drawable getDrawable(String source) {
//            Picture picture = new Picture();
//            PictureDrawable draw = new PictureDrawable(picture);
//
//            ImageLoader.loadImage(source, new ImageLoader.InsertIntoPictureDrawable(draw));
//
//            return new PictureDrawable(picture);
//        }
//    }


    public static InputStream getSettingsI()
    throws IOException {
        File file = new File(files.getPath(), "config.cfg");
        return new FileInputStream(file);
    }

    public static OutputStream getSettingsO()
    throws IOException {
        File file = new File(files.getPath(), "config.cfg");

        if (file.exists()) file.delete();
        file.createNewFile();

        return new FileOutputStream(file);
    }


    /**
     * Фух. Эта штука меняет все HTML 4.0 и 2.0 entity на нормальный текст.
     */
    public static String deEntity(String in) {
        return in
                .replaceAll("&quot;", "\"").replaceAll("&rlm;", " ‏")
                .replaceAll("&amp;", "&").replaceAll("&ndash;", "–")
                .replaceAll("&lt;", "<").replaceAll("&mdash;", "—")
                .replaceAll("&gt;", ">").replaceAll("&lsquo;", "‘")
                .replaceAll("&OElig;", "Œ").replaceAll("&rsquo;", "’")
                .replaceAll("&oelig;", "œ").replaceAll("&sbquo;", "‚")
                .replaceAll("&Scaron;", "Š").replaceAll("&ldquo;", "“")
                .replaceAll("&scaron;", "š").replaceAll("&rdquo;", "”")
                .replaceAll("&Yuml;", "Ÿ").replaceAll("&bdquo;", "„")
                .replaceAll("&circ;", "ˆ").replaceAll("&dagger;", "†")
                .replaceAll("&tilde;", "˜").replaceAll("&Dagger;", "‡")
                .replaceAll("&ensp;", " ").replaceAll("&permil;", "‰")
                .replaceAll("&emsp;", " ").replaceAll("&lsaquo;", "‹")
                .replaceAll("&thinsp;", " ").replaceAll("&rsaquo;", "›")
                .replaceAll("&zwnj;", " ").replaceAll("&euro;", "€")
                .replaceAll("&zwj;", " ").replaceAll("&lrm;", " ")
                .replaceAll("&#039;", "'")
                ;
    }

    /**
     * Немного переделанный ClickableSpan, распознающий двойное нажатие.
     */
    public static abstract class DoubleClickableSpan extends ClickableSpan {
        /**
         * Максимальная задержка двойного тапа.
         */
        public int delay = 500;
        private long last_click;

        @Override public final void onClick(View widget) {
            if (last_click == 0) last_click = System.currentTimeMillis();
            else {
                if (System.currentTimeMillis() - last_click < delay) {
                    onDoubleClick(widget);
                }
                last_click = 0;
            }
        }

        public abstract void onDoubleClick(View widget);


    }
    /**
     * Возвращает слушалку нажатий, которая игнорит все, не давая нижним слоям вызывать OnTouch и OnClick.
     */
    public static View.OnTouchListener getEmptyListener() {
        return new View.OnTouchListener() {
            @Override public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        };
    }

}
