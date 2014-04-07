package everypony.sweetieBot.wrappers;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.text.*;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.text.style.StrikethroughSpan;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.cab404.libtabun.util.html_parser.HTMLParser;
import com.cab404.libtabun.util.html_parser.Tag;
import everypony.sweetieBot.R;
import everypony.sweetieBot.U;
import everypony.sweetieBot.other.ImageLoader;
import org.xml.sax.XMLReader;

import java.util.Vector;

/**
 * Оборачивает html во View.
 *
 * @author cab404
 */
public class TextWrapper {
    public static boolean isPicsDownloading = true;

    /**
     * TextWrappingEventListener
     *
     * @see TextWrapper#wrap(String, android.widget.LinearLayout, everypony.sweetieBot.wrappers.TextWrapper.TWEL)  Где используется.
     */
    public static abstract class TWEL {
        public enum EventType {
            IMAGE_LOADED,
        }

        public abstract void onEvent(Object object, EventType event);
    }

    /**
     * Оборачивает текст в массив Viewшек, попутно обрабатывая спойлеры и картинки.
     *
     * @param twel   Слушалка загрузки. В процессе загрузки будет выполнятся
     *               {@link TWEL#onEvent(Object, everypony.sweetieBot.wrappers.TextWrapper.TWEL.EventType) onEvent}.
     * @param text   Текст, который будем оборачивать.
     * @param target LinearLayout, из которого мы сделаем LayoutInflater :D
     * @see TextWrapper#insert Как быстро вставлять этот массив Viewшек в LinearLayout
     */
    public static Vector<View> wrap(String text, LinearLayout target, TWEL twel) {
        return wrap(LayoutInflater.from(target.getContext()), text, twel, 0);
    }

    /**
     * Оборачивает текст в массив Viewшек, попутно обрабатывая спойлеры и картинки.
     * Используется рекурсивно.
     *
     * @param twel  Слушалка загрузки. В процессе загрузки будет выполнятся
     *              {@link TWEL#onEvent(Object, everypony.sweetieBot.wrappers.TextWrapper.TWEL.EventType) onEvent}.
     * @param text  Текст, который будем оборачивать.
     * @param inf   Чем мы будем эти самые вьюшки создавать.
     * @param level Уровень вложения тега.
     * @see TextWrapper#insert Как быстро вставлять этот массив Viewшек в LinearLayout
     */
    private static Vector<View> wrap(LayoutInflater inf, String text, TWEL twel, int level) {
        HTMLParser parser = new HTMLParser(text);
        Vector<View> out = new Vector<>();

        int currently_parsing = 0;

        // cab404:
        //     Do a spoiler roll!
        //
        int sublevel = level;
        for (Tag tag : parser.tags) {
            if (!tag.isStandalone) {
                if (tag.isClosing) level--;
                else level++;
            }
            try {
//                if ("img".equals(tag.name)) U.v(level - 1 + ":" + sublevel + ":" + tag.get("src"));
                if (level - (tag.isStandalone ? 0 : 1) != sublevel) continue;
                if ("spoiler".equals(tag.props.get("class"))) {
                    HTMLParser spoiler = parser.getParserForIndex(parser.getIndexForTag(tag));

                    // Проверяем на повторную обработку.

                    if (currently_parsing < tag.start)
                        out.add(wrapText(inf, text.substring(currently_parsing, tag.start), twel));


                    RelativeLayout spoiler_view = (RelativeLayout) inf.inflate(R.layout.spoiler, null, false);

                    // Специально для тех упоротых, не умеющих в банальную разметку, прости меня Селестия.
                    // А если конкретнее, некоторые личности пихают спойлеры так:
                    //
                    //<spoiler>
                    //  <spoiler-title>...</spoiler-title>
                    //  <spoiler-content>...</spoiler-content>
                    //  <spoiler>
                    //      <spoiler-title>...</spoiler-title>
                    //      <spoiler-content>...</spoiler-content>
                    //  </spoiler>
                    //</spoiler>

//                    check_in.add(spoiler.getParserForIndex(spoiler.getTagIndexByProperty("class", "spoiler-title")));
//                    check_in.add(spoiler.getParserForIndex(spoiler.getTagIndexByProperty("class", "spoiler-body")));

                    LinearLayout title = (LinearLayout) spoiler_view.findViewById(R.id.label);
                    LinearLayout content = (LinearLayout) spoiler_view.findViewById(R.id.content);

                    // Закрываем спойлер.
                    content.getLayoutParams().height = 0;
                    spoiler_view.updateViewLayout(content, content.getLayoutParams());

                    // Находим тайтл и контент спойлера
                    String str_title = spoiler.getContents(spoiler.getTagIndexByProperty("class", "spoiler-title"));
                    String str_content = spoiler.getContents(spoiler.getTagIndexByProperty("class", "spoiler-body"));

                    // Слушалка нажатий по заголовку
                    SpoilerHeaderListener listener = new SpoilerHeaderListener();
                    listener.content = content;
                    listener.spoiler = spoiler_view;
                    listener.wrapping = str_content;
                    listener.twel = twel;
                    listener.level = level;

                    // Пытаемся повешать листенер на вьюшке, которая вообще над всем. В спойлере.
                    spoiler_view.findViewById(R.id._switch).setOnClickListener(listener);


                    // Вешаем слушалки на вьюшки в тайтле.
                    Vector<View> title_views = wrap(LayoutInflater.from(content.getContext()), str_title, twel, level + 1);
                    for (View view : title_views) {
                        view.setOnClickListener(listener);
                    }

                    // Всё ставим по местам...
                    insert(title, title_views);

                    // ...и пихаем спойлер на место
                    out.add(spoiler_view);

                    // Не должно быть никаких ошибок в тегах, Табун проверяет.
                    currently_parsing = parser.tags.get(parser.getClosingTag(parser.getIndexForTag(tag))).end;
                }
                // Обработка видео
                if ("iframe".equals(tag.name) && !tag.isClosing) {

                    if (currently_parsing < tag.start)
                        out.add(wrapText(inf, text.substring(currently_parsing, tag.start), twel));


                    String src = tag.props.get("src");
                    WebView view = new WebView(inf.getContext());

                    view.getSettings().setJavaScriptEnabled(true);
                    view.setBackgroundColor(Color.TRANSPARENT);
                    view.getSettings().setPluginState(WebSettings.PluginState.ON);
                    view.loadUrl(src);
                    view.setWebChromeClient(new WebChromeClient());

                    view.setLayoutParams(new LinearLayout.LayoutParams(
                            (int) (ImageLoader.lim_x * 0.7f),
                            ImageLoader.lim_x / 2
                    ));
                    out.add(view);

                    currently_parsing = parser.tags.get(parser.getClosingTag(parser.getIndexForTag(tag))).end;
                }

                // Обработка кода
                if ("pre".equals(tag.name) && !tag.isClosing) {

                    if (currently_parsing < tag.start)
                        out.add(wrapText(inf, text.substring(currently_parsing, tag.start), twel));

                    String code = parser.getContents(tag);
                    View pre = inf.inflate(R.layout.code_block, null);
                    ((TextView) pre.findViewById(R.id.content)).setText(U.deEntity(code));
                    out.add(pre);

                    currently_parsing = parser.tags.get(parser.getClosingTag(parser.getIndexForTag(tag))).end;
                }

                // Обработка гифок.
                if (
                        "img".equals(tag.name) &&
                                tag.props.containsKey("title") &&
                                tag.get("title").equals("animated")
                        ) {

                    if (currently_parsing < tag.start)
                        out.add(wrapText(inf, text.substring(currently_parsing, tag.start), twel));


                    String src = tag.get("src");
                    WebView view = new WebView(inf.getContext());

                    view.loadUrl(src);
                    view.setWebChromeClient(new WebChromeClient());
                    view.getSettings().setSupportZoom(false);

//                    view.setInitialScale((int) (((float) ImageLoader.lim_x / w) * 100));
                    view.setInitialScale(70);
                    view.setBackgroundColor(Color.TRANSPARENT);
                    view.setLayoutParams(new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    ));
                    out.add(view);

                    currently_parsing = tag.end;
                }

            } catch (Throwable t) {
                U.e("Ошибка при парсировании текста!");
                U.e(t);
            }
        }

        // Нарезка текста на куски (тут кусок после спойлера пихается в шредер).

        out.add(wrapText(inf, text.substring(currently_parsing), twel));

        return out;
    }


    /**
     * Оборачивает текст в массив Viewшек, попутно обрабатывая спойлеры и картинки.
     *
     * @param twel Слушалка загрузки. В процессе загрузки будет выполнятся
     *             {@link TWEL#onEvent(Object, everypony.sweetieBot.wrappers.TextWrapper.TWEL.EventType) onEvent}.
     * @param text Текст, который будем оборачивать.
     * @param inf  ем мы будем эти самые вьюшки создавать.
     * @see TextWrapper#insert Как быстро вставлять этот массив Viewшек в LinearLayout
     */
    private static View wrapText(LayoutInflater inf, String text, final TWEL twel) {
        TextView text_view = (TextView) inf.inflate(R.layout.text, null, false).findViewById(R.id.text);

        SpannableStringBuilder b = new SpannableStringBuilder();

        b.append(Html.fromHtml(text, null, new Html.TagHandler() {
            int last = 0;
            @Override public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
                if (tag.equals("s"))
                    if (opening)
                        last = output.length();
                    else
                        output.setSpan(new StrikethroughSpan(), last, output.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

                if (tag.equals("li"))
                    if (opening)
                        output.append("\n ⚫ ");
                    else
                        output.append("\n");

            }
        }));

        text_view.setText(b);

        startLoadingIntoSpans(isPicsDownloading, text_view, b, twel);

        text_view.setLinksClickable(true);
        text_view.setMovementMethod(TheBrandNewCoolMovementMethod.getInstance());

        return text_view;
    }

    /**
     * Вставляет все Viewшки из массива в LinearLayout
     */
    public static void insert(LinearLayout layout, Vector<View> views) {
        layout.removeAllViews();
        for (View view : views) {
            if (view.getParent() != null)
                ((LinearLayout) view.getParent()).removeView(view);
            layout.addView(view);
        }
    }

    /**
     * Запускает задания загрузки картинок в текст.
     */
    public static void startLoadingIntoSpans(boolean download, final TextView subtarget, final Editable target, final TWEL twel) {

        for (ImageSpan span : target.getSpans(0, target.length(), ImageSpan.class)) {
            final int start = target.getSpanStart(span);
            final int end = target.getSpanEnd(span);
            final String source = span.getSource();

            ImageSpan replacer = new ImageSpan(subtarget.getContext(), R.drawable.no_image);

            ClickableSpan sp = new U.DoubleClickableSpan() {
                @Override public void onDoubleClick(View widget) {
                    StringBuilder fin = new StringBuilder(source);
                    if (source.startsWith("//")) fin.insert(0, "http:");
                    U.current.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(fin.toString())));
                }
            };

            target.setSpan(sp, start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            target.setSpan(replacer, start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

            if (download)
                ImageLoader.loadImage(source, new ImageLoader.InsertIntoSpan(target, start, end) {
                    TextView upd = subtarget;
                    Editable tg = target;

                    @Override
                    public void loaded(Bitmap bitmap) {
//                        target.removeSpan(replacer);
                        super.loaded(bitmap);
                        upd.setText(tg);
                        twel.onEvent(bitmap, TWEL.EventType.IMAGE_LOADED);
                    }
                });
        }
        subtarget.setText(target);
    }

    /**
     * Слушалка нажатия на спойлер.
     */
    private static class SpoilerHeaderListener implements View.OnClickListener {
        RelativeLayout spoiler;
        LinearLayout content;
        String wrapping;
        TWEL twel;
        int level;
        boolean isOpened = false;
        boolean first = true;

        @Override public void onClick(View v) {

            if (first) {
                first = false;
                insert(content, wrap(LayoutInflater.from(content.getContext()), wrapping, twel, level + 1));
            }

            if (isOpened)
                content.getLayoutParams().height = 0;
            else
                content.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
            isOpened = !isOpened;
            spoiler.updateViewLayout(content, content.getLayoutParams());
        }
    }

    /**
     * Одновременно позволяет слушать нажатия, переходить по ссылкам и выделять текст.
     */
    public static class TheBrandNewCoolMovementMethod extends LinkMovementMethod {

        private static TheBrandNewCoolMovementMethod sInstance;

        public static MovementMethod getInstance() {
            if (sInstance == null)
                sInstance = new TheBrandNewCoolMovementMethod();

            return sInstance;
        }


        @Override public boolean canSelectArbitrarily() {
            return false;
        }

        @Override public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
//
//            int s = widget.getSelectionStart();
//            int f = widget.getSelectionEnd();

//            boolean ret = super.onTouchEvent(widget, buffer, event);

//            if (ret && s > 0 && f > 0) {
//                U.v("start " + s + " " + f);
//                ((EditText) widget).setSelection(s, f);
//            } else {
//                U.v("finish " + s + " " + f);
//                widget.clearFocus();
//            }

            return super.onTouchEvent(widget, buffer, event);
        }
    }
}
