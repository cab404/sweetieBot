package everypony.sweetieBot.wrappers;

import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.cab404.libtabun.parts.Comment;
import everypony.sweetieBot.R;
import everypony.sweetieBot.U;
import everypony.sweetieBot.other.ImageLoader;

import java.util.ArrayList;
import java.util.Vector;

/**
 * Всякие действия с комментариями.
 *
 * @author cab404
 */
public class CommentWrapper {
    /**
     * Оборачивает дерево комментариев в лист. Довольно суровый класс.
     */
    public static class CommentTreeAdapter extends U.FixedAdapter {
        /**
         * Тут лежит дерево комментариев.
         */
        private ArrayList<CommentInterface> tree;

        /**
         * Создаёт пустой адаптер
         */
        public CommentTreeAdapter() {
            tree = new ArrayList<>();
        }


        /**
         * Возвращает индекс комментария в листе по его ID-шнику.
         */
        public int getIndexByID(int id) {
            for (int i = 0; i < tree.size(); i++)
                if (tree.get(i).comment.id == id) return i;
            return 0; // ВНЕЗАПНО может появится ошибка в родителях. См. http://tabun.everypony.ru/comments/4032348
        }

        /**
         * Доавляет комментарий в лист.
         */
        public synchronized void add(Comment comment) {
            if (comment.parent == 0) {
                tree.add(new CommentInterface(comment));
            } else {

                // Находим родительский комментарий
                int i = getIndexByID(comment.parent);
                CommentInterface inf = new CommentInterface(comment);
                inf.offset = tree.get(i).offset + 1;

                i++;
                // Пропускаем комментарии на род. комментарий
                while (i < tree.size()) {
                    if (tree.get(i).offset <= inf.offset && tree.get(i).comment.parent != comment.parent) {
                        tree.add(i, inf);
                        break;
                    } else {
                        i++;
                    }
                }
                if (i == tree.size()) tree.add(inf);
            }
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return tree.size();
        }

        @Override
        public Object getItem(int i) {
            return tree.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        /**
         * Снимает со всех комментариев метки is_new
         */
        public void removeAllNew() {
            for (CommentInterface comm : tree)
                comm.comment.is_new = false;
        }


        @Override
        public View getView(int i, View view, ViewGroup group) {
            LayoutInflater inflater = LayoutInflater.from(group.getContext());
            if (view == null || view.findViewById(R.id.avatar) == null)
                view = inflater.inflate(R.layout.comment, group, false);

            if (tree.get(i).comment.MODERASTIA) {
                view = inflater.inflate(R.layout.dead_comment, group, false);
            } else
                convert(tree.get(i), view);

            return view;
        }

        /**
         * Конвертирует уже существующий комментарий в новый.
         *
         * @param old     Что конвертируем
         * @param comment Во что конвертируем
         */
        public void convert(final CommentInterface comment, final View old) {
            comment.clearTasks();

            LinearLayout content = (LinearLayout) old.findViewById(R.id.content);
            ImageView avatar = (ImageView) old.findViewById(R.id.avatar);
            TextView author = (TextView) old.findViewById(R.id.author);
            TextView vote = (TextView) old.findViewById(R.id.votes);
            TextView date = (TextView) old.findViewById(R.id.date);

            if (comment.comment.is_new)
                old.findViewById(R.id.comment_body).setBackgroundColor(U.res.getColor(R.color.Text_LightGreen));
            else
                old.findViewById(R.id.comment_body).setBackgroundColor(U.res.getColor(R.color.Text_Default_White));

            if (comment.views == null)
                comment.views = TextWrapper.wrap(comment.comment.body.trim(), content, new TextWrapperListenerImpl());
            TextWrapper.insert(content, comment.views);

            String avatar_url = comment.comment.avatar.replace("24x24", "48x48");
            ImageLoader.InsertIntoView job = new ImageLoader.InsertIntoView(avatar);
            AsyncTask task = ImageLoader.loadImage(avatar_url, job);
            if (task != null) {
                avatar.setImageDrawable(U.res.getDrawable(R.drawable.refresh));
                comment.tasks.add(task);
            }


            author.setText(String.valueOf(comment.comment.author));
            vote.setText(String.valueOf(comment.comment.votes));
            date.setText(String.valueOf(comment.comment.time));

            if (comment.comment.votes > 0)
                vote.setTextColor(everypony.sweetieBot.U.res.getColor(R.color.Text_Green));
            if (comment.comment.votes < 0)
                vote.setTextColor(everypony.sweetieBot.U.res.getColor(R.color.Text_Red));
            if (comment.comment.votes == 0)
                vote.setTextColor(everypony.sweetieBot.U.res.getColor(R.color.Text_Default_Black));

            LinearLayout padding = (LinearLayout) old.findViewById(R.id.padding);
            padding.removeViews(0, padding.getChildCount() - 1);

            old.setBackgroundColor(U.res.getColor(R.color.Comments_Levels_1 - 1 + (comment.offset > 40 ? 40 : comment.offset)));
            for (int i = comment.offset; i >= 1; i--) {
                ImageView col = new ImageView(old.getContext());
                col.setLayoutParams(new AbsListView.LayoutParams(U.dp(16), ViewGroup.LayoutParams.MATCH_PARENT));
                col.setImageDrawable(new ColorDrawable(U.res.getColor(R.color.Comments_Levels_1 - 1 + (i > 40 ? 40 : i))));
                padding.addView(col, 0);
            }

            // Actions!

            final LinearLayout actions = (LinearLayout) old.findViewById(R.id.actions);
            actions.setVisibility(View.GONE);

            old.findViewById(R.id.activate).setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {actions.setVisibility(View.VISIBLE); }
            });

            ImageView rate_down = (ImageView) actions.findViewById(R.id.minus);
            ImageView rate_up = (ImageView) actions.findViewById(R.id.plus);
            ImageView close = (ImageView) actions.findViewById(R.id.cancel);
            ImageView reply = (ImageView) actions.findViewById(R.id.reply);
            ImageView edit = (ImageView) actions.findViewById(R.id.edit);

            if (U.user.isLoggedIn() && U.user_info.nick.equals(comment.comment.author)) {
                edit.setVisibility(View.VISIBLE);
                edit.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        onEditAction(old, comment.comment);
                    }
                });
            } else edit.setVisibility(View.GONE);

            close.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    actions.setVisibility(View.GONE);
                }
            });

            reply.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    onReplyAction(old, comment.comment);
                }
            });

            rate_up.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    onVoteAction(1, old, comment.comment);
                }
            });

            rate_down.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    onVoteAction(-1, old, comment.comment);
                }
            });

        }

        public int size() {
            return tree.size();
        }

        public CommentInterface get(int i) {
            return tree.get(i);
        }

        /**
         * Анахронизм. Но пусть пока будет, вдруг понадобится.
         */
        private class TextWrapperListenerImpl extends TextWrapper.TWEL {
            @Override public void onEvent(Object object, EventType event) {
            }
        }

        /**
         * Выполняется при нажатии кнопки ответа в комментарии
         */
        public void onReplyAction(View comment_view, Comment comment) {
            comment_view.findViewById(R.id.actions).setVisibility(View.GONE);
        }

        /**
         * Выполняется при нажатии кнопки редактирования в комментарии
         */
        public void onEditAction(View comment_view, Comment comment) {
            comment_view.findViewById(R.id.actions).setVisibility(View.GONE);
        }


        /**
         * Выполняется при нажатии плюса или минуса у комментария
         *
         * @param vote 1, если плюс; -1, если минус.
         */
        public void onVoteAction(int vote, View comment_view, Comment comment) {
            comment_view.findViewById(R.id.actions).setVisibility(View.GONE);
        }

        /**
         * Кэш комментария.
         */
        public class CommentInterface {
            /**
             * Задания загрузки.
             */
            private Vector<AsyncTask> tasks;
            /**
             * Комментарий, который оборачивается этим объектом.
             */
            public final Comment comment;
            /**
             * Уровень вложения комментария.
             */
            public int offset = 0;
            /**
             * Список viewшек.
             */
            private Vector<View> views;

            /**
             * Низвергает задания загрузки во тьму внешнюю.
             */
            public void clearTasks() {
                for (AsyncTask task : tasks) task.cancel(false);
                tasks.clear();
            }

            /**
             * Убивает сохраннный кэш.
             */
            public void clearCache() {
                views = null;
            }

            public CommentInterface(Comment from) {
                comment = from;
                tasks = new Vector<>();
            }
        }
    }
}
