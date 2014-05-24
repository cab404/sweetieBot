package everypony.sweetieBot.activities;

import android.animation.Animator;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.cab404.libtabun.data.Comment;
import com.cab404.libtabun.data.Topic;
import com.cab404.libtabun.data.Types;
import com.cab404.libtabun.pages.TopicPage;
import com.cab404.libtabun.requests.CommentAddRequest;
import com.cab404.libtabun.requests.VoteRequest;
import com.cab404.moonlight.util.SU;
import com.cab404.moonlight.util.exceptions.MoonlightFail;
import everypony.sweetieBot.R;
import everypony.sweetieBot.U;
import everypony.sweetieBot.other.MultitaskingActivity;
import everypony.sweetieBot.wrappers.CommentWrapper;
import everypony.sweetieBot.wrappers.PostWrapper;

import java.util.*;

/**
 * Всё тут ясно.
 *
 * @author cab404
 */
public class PostActivity extends MultitaskingActivity {
    ImageView fav, vote, plus, minus, update, reply, back, down, share;
    TextView new_counter;
    ListView list;
    View header, loading, comment_bar;
    public static boolean insta_scroll = true;
    public static boolean panel_on_right = true;
    public CommentWrapper.CommentTreeAdapter comments;


    // Что делаем в поле.
    int action = 0;
    int ACTION_ADD = 0;
    int ACTION_REPLY = 1;
    int ACTION_EDIT = 2;
    // На что отвечаем в поле ответа.
    Comment selected_comment;
    int index_selected = -1;
    boolean loaded = false;

    // Инфа о посте. id - полученное при старте значение.
    Topic post;
    TopicPage page;
    int id;

    static String[] reply_to;

    private int getPanelGravity() {
        return panel_on_right ? GravityCompat.END : GravityCompat.START;
    }

    private void closeDrawer() {
        ((DrawerLayout) findViewById(R.id.drawer)).closeDrawer(getPanelGravity());
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        scroll = new ArrayList<>();

        // Запихаем папку с кэшем в U
        U.dumpAll(this);

        // Ну и закинемся парой ответов из файла.
        reply_to = U.res.getStringArray(R.array.reply_to);

        setContentView(R.layout.loading);

        // Смотрим, что там у нас в intent-ах, и откуда мы пришли.
        int post_id;
        if (getIntent().getAction().equals("post-direct")) {
            post_id = getIntent().getIntExtra("post-id", -1);
        } else {
            post_id = Integer.parseInt(SU.bsub(getIntent().getData().toString(), "/", ".html"));
        }
        id = post_id;

        // Тут настройка ответов и лайков по комментариям.
        comments = new CommentWrapper.CommentTreeAdapter() {

            @Override public void onReplyAction(View comment_view, Comment comment) {
                super.onReplyAction(comment_view, comment);
                selected_comment = comment;
                index_selected = comments.getIndexByID(comment.id);

                action = ACTION_REPLY;

                setCommentonatorTo(U.getRandomEntry(reply_to) + comment.author);
                comment_bar.findViewById(R.id.text).requestFocus();
            }

            @Override public void onEditAction(View comment_view, Comment comment) {
                super.onEditAction(comment_view, comment);
                selected_comment = comment;
                index_selected = comments.getIndexByID(comment.id);

                action = ACTION_EDIT;

                setCommentonatorTo("Редактируем свои ошибки.");

                ((TextView) comment_bar.findViewById(R.id.text)).setText(comment.text);
                comment_bar.findViewById(R.id.text).requestFocus();
            }

            @Override public void onVoteAction(int vote, View comment_view, Comment comment) {
                addTask(new VoteForComment().execute(new CommentVote(comment.id, vote)));
            }
        };

        if (U.user == null)
            startActivityForResult(new Intent(getApplicationContext(), Login.class), 0);
        else afterInit();
    }

    @Override public void onBackPressed() {
        finish();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (U.user != null) {
            U.v("Запущен afterInit в PostAcivity из onActivityResult.");
            afterInit();
        } else {
            finish();
        }
    }

    private void afterInit() {
        addTask(new FetchPost().execute());
    }

    private void initPseudoBar() {

        // Достаём кнопки.
        View actions = findViewById(R.id.action_bar);
        fav = (ImageView) actions.findViewById(R.id.fav);
        back = (ImageView) actions.findViewById(R.id.back);
        vote = (ImageView) actions.findViewById(R.id.rate);
        plus = (ImageView) actions.findViewById(R.id.plus);
        down = (ImageView) actions.findViewById(R.id.down);
        minus = (ImageView) actions.findViewById(R.id.minus);
        reply = (ImageView) actions.findViewById(R.id.reply);
        share = (ImageView) actions.findViewById(R.id.share);
        update = (ImageView) actions.findViewById(R.id.refresh);

        new_counter = (TextView) actions.findViewById(R.id.new_comments);

        // Ставим слушалки
        vote.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent event) {
                if (post != null && post.vote_enabled && loaded) {
                    if (event.getAction() == 0) {
                        fav.setVisibility(View.INVISIBLE);
                        plus.setVisibility(View.VISIBLE);
                        minus.setVisibility(View.VISIBLE);
                        update.setVisibility(View.INVISIBLE);
                    }
                    if (event.getAction() == 1) {
                        fav.setVisibility(View.VISIBLE);
                        plus.setVisibility(View.INVISIBLE);
                        minus.setVisibility(View.INVISIBLE);
                        update.setVisibility(View.VISIBLE);

                        float x = event.getX();
                        float y = event.getY();

                        if (x >= U.dp(0) && x <= U.dp(36)) {
                            if (y < 0 && y > U.dp(-36 - 16)) {
                                vote.setImageDrawable(U.res.getDrawable(R.drawable.rate_down));
                                minus();
                            } else if (y >= U.dp(36) && y <= U.dp(36 * 2 + 16)) {
                                vote.setImageDrawable(U.res.getDrawable(R.drawable.rate_up));
                                plus();
                            }
                        }
                    }
                } else return false;
                return true;
            }
        });


        fav.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (loaded)
                    fav();
            }
        });

        down.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                list.setSelection(comments.getCount() - 1);
                closeDrawer();
            }
        });

        update.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (loaded)
                    update(false, false);
            }
        });

        update.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) {
                if (loaded) {
                    update(true, false);
                    return true;
                }
                return false;
            }
        });

        reply.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (loaded) {
                    if (comment_bar.getVisibility() == View.VISIBLE) {
                        action = ACTION_ADD;
                        selected_comment = null;
                        index_selected = -1;
                        setCommentonatorTo("Отвечаем в пост.");
                    }
                    comment_bar.findViewById(R.id.text).requestFocus();
                    closeDrawer();
                }
            }
        });

        share.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= 11) {
                    ClipboardManager man = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    man.setPrimaryClip(ClipData.newPlainText(post.title, "http://" + U.user.getHost() + "/blog/" + post.id + ".html"));
                } else {
                    @SuppressWarnings("deprecation")
                    android.text.ClipboardManager old = (android.text.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    old.setText("http://" + U.user.getHost() + "/blog/" + post.id + ".html");
                }
                U.showOkToast("Eeyup.", "URL поста скопирован в буфер обмена.", getBaseContext());
                closeDrawer();

            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { onBackPressed();}
        });

    }

    private boolean __plusomet_safelock = false;

    private void plus() {
        if (!__plusomet_safelock) {
            __plusomet_safelock = true;
            addTask(new VoteForPost().execute(1));
            closeDrawer();
        }
    }

    private void minus() {
        if (!__plusomet_safelock) {
            __plusomet_safelock = true;
            addTask(new VoteForPost().execute(-1));
            closeDrawer();
        }
    }

    private boolean __fav_adder = false;

    private void fav() {
        if (!__fav_adder) {
            __fav_adder = true;
            addTask(new ToggleFavs().execute());
            fav.clearAnimation();
            closeDrawer();
        }
    }

    private boolean __update_lock = false;
    private ArrayList<Integer> scroll;

    private void update(boolean force, boolean added_comment) {
        if (!__update_lock) {
            setNewCommentsNum(scroll.size());
            if (scroll.size() == 0 || force || added_comment) {

                if (force) {
                    comments.removeAllNew();
                    scroll.clear();
                }

                __update_lock = true;

                if (U.SDK >= 12)
                    update.animate().rotationBy(180).setListener(new LoopListener());

                addTask(new UpdateComments().execute());

            } else {

                Collections.sort(scroll, new Comparator<Integer>() {
                    @Override public int compare(Integer a, Integer b) {
                        return comments.getIndexByID(a) - comments.getIndexByID(b);
                    }
                });

                int id = comments.getIndexByID(scroll.remove(0));
                comments.get(id).comment.is_new = false;
                comments.get(id).clearCache();
                comments.notifyDataSetChanged();

                if (insta_scroll)
                    list.setSelection(id + 1);
                else {
                    if (U.SDK >= 11)
                        list.smoothScrollToPositionFromTop(id + 1, 8);
                    else if (U.SDK >= 8)
                        list.smoothScrollToPosition(id + 1);
                }

            }
            setNewCommentsNum(scroll.size());
        }
    }

    private void setCommentonatorTo(String to) {
        ((TextView) comment_bar.findViewById(R.id.to)).setText(to);
    }

    private PostWrapper.PostLabelList.PostCache cache;

    private void synchronizeHeader() {
        if (cache == null) cache = new PostWrapper.PostLabelList.PostCache(post);
        PostWrapper.PostLabelList.convertPostLabel(cache, header, new PostWrapper.PostLabelList.TWILi(), new View.OnClickListener() {
            @Override public void onClick(View v) {
            }
        });
    }

    private void setNewCommentsNum(int new_comments) {
        new_counter.setText(new_comments > 0 ? new_comments + "" : "");
    }

    // ==-==-==-==-==-==-==-==-== Тут всякие local классы ==-==-==-==-==-==-==-==-==-==

    private class LoopListener implements Animator.AnimatorListener {
        @Override public void onAnimationStart(Animator animation) {
        }

        @Override public void onAnimationEnd(Animator animation) {
            if (__update_lock)
                update.animate().rotationBy(180).setListener(new LoopListener());
        }

        @Override public void onAnimationCancel(Animator animation) {
        }

        @Override public void onAnimationRepeat(Animator animation) {
        }
    }

    private class FetchPost extends AsyncTask<Void, Comment, Void> {
        @Override protected void onProgressUpdate(Comment... values) {
            if (values[0] == null) {

                // Ставим layout поста.
                setContentView(R.layout.post_layout);

                findViewById(R.id.action_bar).setOnTouchListener(U.getEmptyListener());

                DrawerLayout.LayoutParams params = (DrawerLayout.LayoutParams) findViewById(R.id.action_bar).getLayoutParams();
                params.gravity = getPanelGravity();

                // Ставим PseudoBar (tm) NavSystems (c)
                initPseudoBar();

                // Кидаем лист в переменные
                list = (ListView) findViewById(R.id.list);
                list.setDividerHeight(0);

                header = getLayoutInflater().inflate(R.layout.post_content, list, false);
                loading = getLayoutInflater().inflate(R.layout.loading, list, false);
                list.addHeaderView(header);

                try {
                    list.addFooterView(loading);
                } catch (NullPointerException e) {
                    U.w("Странная ошибка с footer-ом.");
                }
                list.setAdapter(comments);
                synchronizeHeader();

                if (post.in_favourites)
                    fav.setImageDrawable(U.res.getDrawable(R.drawable.favourites_active));

                if (!post.vote_enabled || !U.isLoggedIn()) {
                    if (post.your_vote == 1) vote.setImageDrawable(U.res.getDrawable(R.drawable.rate_up));
                    if (post.your_vote == 0) vote.setImageDrawable(U.res.getDrawable(R.drawable.rate_active));
                    if (post.your_vote == -1) vote.setImageDrawable(U.res.getDrawable(R.drawable.rate_down));
                }
            } else {

                synchronized (comments) {
                    comments.add(values[0]);
                    if (values[0].is_new) {
                        scroll.add(comments.get(comments.size() - 1).comment.id);
                        setNewCommentsNum(scroll.size());
                    }
                }

            }
        }

        @Override protected Void doInBackground(Void... params) {
            try {
                page = new TopicPage(id) {
                    @Override public void handle(Object object, int key) {
                        super.handle(object, key);
                        switch (key) {
                            case BLOCK_COMMENT:
                                publishProgress((Comment) object, null);
                                break;
                            case BLOCK_TOPIC_HEADER:
                                publishProgress(null, null);
                                break;
                        }
                    }
                };
                page.fetch(U.user);
            } catch (MoonlightFail e) {
                Log.e("Luna Log", "Ошибка Moonlight!", e);
            }

            return null;
        }

        @Override protected void onPostExecute(Void aVoid) {

            // =-=-=-=-=-=-=-=- //
            // -= Тут init() -= //
            // =-=-=-=-=-=-=-=- //

            // Убираем окошко загрузки.
            try {
                list.removeFooterView(loading);
            } catch (Exception e) {
                U.w(e);
                return;
            }

            // Добавляем комментонатор
            RelativeLayout things = (RelativeLayout) findViewById(R.id.things);
            comment_bar = getLayoutInflater().inflate(R.layout.comment_bar, things, false);

            // Настраиваем различные слушалки
            final EditText text = (EditText) comment_bar.findViewById(R.id.text);
            text.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override public void onFocusChange(View v, boolean hasFocus) {
                    EditText text = (EditText) v;
                    text.setCursorVisible(true);
                    InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (!hasFocus) {
                        // Скрываем клавиатуру. Вот прям так.
                        inputManager.hideSoftInputFromWindow(text.getWindowToken(), 0);
                        text.setSingleLine(true);
                        // И скрываем всякие там редакторы.
                        comment_bar.findViewById(R.id.tools).setVisibility(View.GONE);
                    } else {
                        comment_bar.setVisibility(View.VISIBLE);
                        // Показываем клавиатуру.
                        inputManager.showSoftInput(text, 0);
                        text.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                        text.setSingleLine(false);
                        // И редактор
                        comment_bar.findViewById(R.id.tools).setVisibility(View.VISIBLE);
                    }
                }
            });


            list.setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override public void onScrollStateChanged(AbsListView view, int scrollState) {
                    int firstVisibleItem = view.getFirstVisiblePosition();
                    if (firstVisibleItem >= comments.size()) return;

                    if (scrollState == SCROLL_STATE_IDLE) {
                        int list_offset = (-comments.get(firstVisibleItem).offset + 3) * U.dp(16);
                        list_offset = list_offset > 0 ? 0 : list_offset;

                        if (list.getLayoutParams().width != U.res.getDisplayMetrics().widthPixels + -list_offset) {
                            list.getLayoutParams().width = U.res.getDisplayMetrics().widthPixels + -list_offset;
                            list.invalidateViews();

                            if (U.SDK >= 12) {
                                list.clearAnimation();
                                list.animate().x(list_offset);
                            } else {
                                list.scrollTo(-list_offset, 0);
                            }

                        }
                    }
                }


                @Override public void onScroll(final AbsListView list, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
//                  Не, прости s3ious, но это полная фигня.
//                    if (index_selected != -1) {
//                        if (firstVisibleItem >= index_selected || firstVisibleItem + visibleItemCount < index_selected) {
//                            text.clearFocus();
//                        }
//                    }
                }
            });

            ////////////////////////
            // Настройка редактора /
            ////////////////////////
// Автоскроллинг до комментария, на который отвечаешь.
//            text.addTextChangedListener(new TextWatcher() {
//                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//                }
//
//                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
//                    if (index_selected != -1)
//                        if (U.SDK >= 11)
//                            list.smoothScrollToPositionFromTop(index_selected + 1, 8);
//                }
//
//                @Override public void afterTextChanged(Editable s) {
//                }
//            });

            ImageView bold, italic, href, quote, strthr, img, down, spoiler, underlined;

            underlined = (ImageView) comment_bar.findViewById(R.id.underlined);
            spoiler = (ImageView) comment_bar.findViewById(R.id.spoiler);
            italic = (ImageView) comment_bar.findViewById(R.id.italic);
            strthr = (ImageView) comment_bar.findViewById(R.id.strike);
            quote = (ImageView) comment_bar.findViewById(R.id.quote);
            bold = (ImageView) comment_bar.findViewById(R.id.bold);
            href = (ImageView) comment_bar.findViewById(R.id.link);
            down = (ImageView) comment_bar.findViewById(R.id.down);
            img = (ImageView) comment_bar.findViewById(R.id.pic);

            underlined.setOnClickListener(new EditorListener(text, "<u>", "</u>"));
            spoiler.setOnClickListener(new EditorListener(text, "<sp><->", "</sp>", "<sp>".length()));
            italic.setOnClickListener(new EditorListener(text, "<em>", "</em>"));
            strthr.setOnClickListener(new EditorListener(text, "<s>", "</s>"));
            quote.setOnClickListener(new EditorListener(text, "<blockquote>", "</blockquote>"));
            href.setOnClickListener(new EditorListener(text, "<a href=\"\">", "</a>", "<a href=\"".length()));
            bold.setOnClickListener(new EditorListener(text, "<strong>", "</strong>"));
            img.setOnClickListener(new EditorListener(text, "<img src=\"", "\"/>"));


            down.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    comment_bar.setVisibility(View.GONE);
                }
            });

            ///////////////////////
            ///////////////////////
            ///////////////////////

            ImageView comment_button = (ImageView) comment_bar.findViewById(R.id.apply);
            comment_button.setOnClickListener(
                    new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            U.v("Отвечаем на комментарий " + (selected_comment == null ? 0 : selected_comment.id));
                            U.v(text.getText().toString());
                            new CommentAction().execute(new CommentActionData(selected_comment, text.getText().toString()));
                        }
                    }
            );

            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            comment_bar.setLayoutParams(params);
            comment_bar.setVisibility(View.GONE);
            setCommentonatorTo("");


            // Добавляем пустоту под комментариями.
            View space = new View(list.getContext());
            space.setLayoutParams(
                    new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            Math.max(
                                    U.res.getDisplayMetrics().heightPixels,
                                    U.res.getDisplayMetrics().widthPixels) / 2
                    )
            );
            space.setBackgroundColor(U.res.getColor(R.color.Text_Default_White));
            list.addFooterView(space);

            things.addView(comment_bar);
//            comment_bar.setY(U.res.getDisplayMetrics().heightPixels);

            // И в конце концов, включаем действия.
            if (U.isLoggedIn())
                loaded = true;
        }
    }

    private class UpdateComments extends AsyncTask<Void, Comment, Void> {

        @Override protected void onProgressUpdate(Comment... values) {
            synchronized (comments) {
                comments.add(values[0]);
                scroll.add(values[0].id);
                setNewCommentsNum(scroll.size());
            }
        }

        @Override protected Void doInBackground(Void... params) {
//            try {
//                post.fetchNewComments(U.user, new Post.CommentListener() {
//                    @Override public void onCommentLoad(Comment comment) {
//                        publishProgress(comment);
//                    }
//                });
//            } catch (Exception ex) {
//                U.v("Timeout while updating comments");
//            }
            __update_lock = false;
            return null;
        }

        @Override protected void onPostExecute(Void aVoid) {
        }
    }

    private class VoteForPost extends AsyncTask<Integer, Void, Boolean> {

        @Override protected Boolean doInBackground(Integer... params) {
            int vote = params[0];
            return new VoteRequest(post.id, vote, Types.TOPIC).exec(U.user, page).success();
        }

        @Override protected void onPostExecute(Boolean err) {
            if (err) {
                U.showOkToast("...", "Плюсомёт сломали пегасы. \nНечего не меня так смотреть!", getApplicationContext());
                vote.setImageDrawable(U.res.getDrawable(R.drawable.rate_active));
            } else {
                post.vote_enabled = false;
                if (post.your_vote == 1) vote.setImageDrawable(U.res.getDrawable(R.drawable.rate_up));
                if (post.your_vote == 0) vote.setImageDrawable(U.res.getDrawable(R.drawable.rate_active));
                if (post.your_vote == -1) vote.setImageDrawable(U.res.getDrawable(R.drawable.rate_down));
                synchronizeHeader();
            }
            __plusomet_safelock = false;
        }
    }

    private class ToggleFavs extends AsyncTask<Void, Void, Void> {

        @Override protected Void doInBackground(Void... params) {
//            boolean err;
//
//            if (post.in_favourites)
//                err = U.user.removeFromFavs(post);
//            else
//                err = U.user.addToFavs(post);
//
//            if (!err)
//                post.isInFavs = !post.isInFavs;
            return null;
        }

        @Override protected void onPostExecute(Void aVoid) {
            if (post.in_favourites)
                fav.setImageDrawable(U.res.getDrawable(R.drawable.favourites_active));
            else
                fav.setImageDrawable(U.res.getDrawable(R.drawable.favourites_not_active));
            __fav_adder = false;
        }
    }

    boolean reply_lock = false;

    private static class CommentActionData {

        private static final Map<String, String> replacer = new HashMap<>();
        static {
            replacer.put("<sp>", "<span class=\"spoiler\"><span class=\"spoiler-title\" onclick=\"return true;\">");
            replacer.put("<->", "</span><span class=\"spoiler-body\">");
            replacer.put("</sp>", "</span></span>");
        }

        Comment in_reply_to;
        String reply_body;

        private CommentActionData(Comment parent, String body) {
            this.in_reply_to = parent;

            StringBuilder edit = new StringBuilder(body);

            for (Map.Entry<String, String> e : replacer.entrySet()) {
                String k = e.getKey();
                String v = e.getValue();

                int i = edit.indexOf(k);

                while (i != -1) {
                    edit.replace(i, i + k.length(), v);
                    i = edit.indexOf(k);
                }

            }

            this.reply_body = edit.toString();
        }
    }

    private class CommentAction extends AsyncTask<CommentActionData, Void, Boolean> {
        CommentActionData data;
        @Override protected void onPreExecute() {
            comment_bar.findViewById(R.id.text).setEnabled(false);
            comment_bar.findViewById(R.id.text).clearFocus();
        }

        @Override protected Boolean doInBackground(CommentActionData... params) {
            if (reply_lock) return null;
            else reply_lock = true;

            data = params[0];

            assert data != null;

            if (action == ACTION_EDIT) {

//                String redacted = data.in_reply_to.edit(U.user, post, data.reply_body);
//
//                if (redacted != null) {
//                    data.in_reply_to.body = redacted;
//                    comments.get(comments.getIndexByID(data.in_reply_to.id)).clearCache();
//                }

//                return redacted == null;
                return false;
            } else {
                new CommentAddRequest(
                        Types.TOPIC,
                        post.id,
                        action == ACTION_ADD ? 0 : data.in_reply_to.id,
                        data.reply_body
                ).exec(U.user, page);
            }


            throw new RuntimeException("Попытка вызвать неизвестное действие!");

        }

        @Override protected void onPostExecute(Boolean err) {
            comments.notifyDataSetChanged();
            if (err == null) return;

            reply_lock = false;
            setCommentonatorTo("Отвечаем в пост.");
            action = ACTION_ADD;
            selected_comment = null;

            comment_bar.findViewById(R.id.text).setEnabled(true);
            if (!err) {
                update(false, true);
                ((EditText) comment_bar.findViewById(R.id.text)).setText("");
            } else {
                U.showOkToast("Что-то пошло не так!", "Комментарий не отправлен!", getApplicationContext());
            }
        }
    }

    private class CommentVote {
        int id, vote;

        private CommentVote(int id, int vote) {
            this.id = id;
            this.vote = vote;
        }
    }

    private class VoteForComment extends AsyncTask<CommentVote, Void, Boolean> {

        @Override protected Boolean doInBackground(CommentVote... params) {
            CommentVote vote = params[0];
            assert vote != null;

            return new VoteRequest(vote.id, vote.vote, Types.COMMENT).exec(U.user, page).success();
        }

        @Override protected void onPostExecute(Boolean status) {
            if (status) {
                list.invalidateViews();
                U.showOkToast("Бздынь!", "Засчитано!", getApplicationContext());
            } else {
                U.showOkToast("Осечка!", "Не удалось выстрелить из плюсомёта!", getApplicationContext());
            }
        }
    }

    private class EditorListener implements View.OnClickListener {
        EditText text;
        String ot, ct;
        int seql;

        private EditorListener(EditText text, String ot, String ct, int l) {
            this.text = text;
            this.ot = ot;
            this.ct = ct;
            this.seql = l;
        }

        private EditorListener(EditText text, String ot, String ct) {
            this.ct = ct;
            this.ot = ot;
            this.text = text;
            seql = ot.length();
        }

        @Override public void onClick(View v) {
            CharSequence seq = text.getText();
            int s = text.getSelectionStart(), f = text.getSelectionEnd(), l = seq.length();
            text.setText(
                    (s > 0 ? seq.subSequence(0, s) : "")
                            + ot
                            + seq.subSequence(s, f)
                            + ct
                            + (f < l ? seq.subSequence(f, l) : "")
            );
            text.setSelection(s + seql);
        }
    }
}
