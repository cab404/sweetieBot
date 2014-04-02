package everypony.sweetieBot.wrappers;

import android.content.Intent;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.cab404.libtabun.U;
import com.cab404.libtabun.parts.Blog;
import com.cab404.libtabun.parts.PaWPoL;
import everypony.sweetieBot.R;
import everypony.sweetieBot.activities.PostActivity;
import everypony.sweetieBot.other.ImageLoader;

import java.util.ArrayList;
import java.util.Vector;

/**
 * Классы и функции для обёртки постов и обложек постов.
 *
 * @author cab404
 */
public class PostWrapper {

    /**
     * Оборачивает список постов из блога, результатов поиска, избранного, или где вы там блоги нашли.
     */
    public static class PostLabelList extends everypony.sweetieBot.U.FixedAdapter {
        private ArrayList<PostCache> labels = new ArrayList<>();
        public Blog blog;
        boolean loading = false, finished = false;

        public PostLabelList(Blog blog) {
            this.blog = blog;
        }

        /**
         * Кэширует содержимое поста, включая текст и задания загрузки.
         */
        public static class PostCache {
            Vector<View> views;
            Vector<AsyncTask> tasks;
            PaWPoL.PostLabel label;

            public PostCache(PaWPoL.PostLabel lab) {
                label = lab;
                tasks = new Vector<>();
            }
        }

        /**
         * Добавляет пост в страничку.
         */
        public void add(PaWPoL.PostLabel label) {
            labels.add(new PostCache(label));
        }

        @Override public int getCount() {
            return labels.size() + (finished ? 0 : 1);
        }

        @Override public Object getItem(int position) {
            return labels.get(position);
        }

        @Override public long getItemId(int position) {
            return position;
        }

        /**
         * Выпольняется при достижении конца странички.
         */
        private void startLoadingInsteadOfPaginator() {
            if (!loading) {
                everypony.sweetieBot.U.v("Загружаю новые посты из страницы...");
                loading = true;
                new AsyncTask<Void, Void, Boolean>() {
                    @Override protected Boolean doInBackground(Void... params) {
                        try {
                            return blog.loadNextPage(everypony.sweetieBot.U.user);
                        } catch (Exception ex) {
                            U.w("Интернет плохой, загрузка новой страницы отменена.");
                            return null;
                        }
                    }

                    @Override protected void onPostExecute(Boolean aBoolean) {
                        if (aBoolean == null) {
                            return;
                        }
                        if (aBoolean)
                            for (PaWPoL.PostLabel lab : blog.posts) {
                                labels.add(new PostCache(lab));
                            }
                        else finished = true;
                        loading = false;
                        notifyDataSetChanged();
                    }
                }.execute();
            }
        }

        @Override public View getView(final int position, View convertView, final ViewGroup parent) {

            LayoutInflater inf = LayoutInflater.from(parent.getContext());
            if (convertView == null || convertView.getId() != R.id.blog_label) {
                convertView = inf.inflate(R.layout.post_content, parent, false);
            }

            if (position == labels.size()) {
                startLoadingInsteadOfPaginator();
                return inf.inflate(R.layout.loading, parent, false);
            }


            convertPostLabel(labels.get(position), convertView, new View.OnClickListener() {
                @Override public void onClick(View v) {
                    Intent intent = new Intent(
                            parent.getContext(),
                            PostActivity.class
                    );
                    intent.setAction("post-direct");
                    intent.putExtra("post-id", labels.get(position).label.id);
                    parent.getContext().startActivity(intent);
                }
            });
            return convertView;
        }

        /**
         * Тут ничего нет. Анахронизм из эпохи Незагружающихся Картинок.
         */
        public static class TWILi extends TextWrapper.TWEL {

            @Override public void onEvent(Object object, EventType event) {
//                notifyDataSetInvalidated();
            }
        }

        /**
         * Тут тоже ничего нет. И это тоже анахронизм из эпохи Незагружающихся Картинок.
         */
        private class TextWrapperListenerImplNonStatic extends TextWrapper.TWEL {

            @Override public void onEvent(Object object, EventType event) {
//                notifyDataSetChanged();
            }
        }

        /**
         * Берёт View с постом, и превращает его во View с другим постом.
         *
         * @param listener слушалка нажатия по заголовку
         */
        public void convertPostLabel(final PostCache post, View label, View.OnClickListener listener) {
            convertPostLabel(post, label, new TextWrapperListenerImplNonStatic(), listener);
        }

        /**
         * Берёт View с постом, и превращает его во View с другим постом.
         *
         * @param twel     Выполнятся при загрузке текста поста.
         * @param listener слушалка нажатия по заголовку
         */
        public static void convertPostLabel(final PostCache post, View label, TextWrapper.TWEL twel, View.OnClickListener listener) {
            TextView date = (TextView) label.findViewById(R.id.date);
            TextView tags = (TextView) label.findViewById(R.id.tags);
            TextView votes = (TextView) label.findViewById(R.id.votes);
            TextView title = (TextView) label.findViewById(R.id.title);
            TextView author = (TextView) label.findViewById(R.id.author);
            ImageView avatar = (ImageView) label.findViewById(R.id.avatar);
            TextView comments = (TextView) label.findViewById(R.id.comments);
            TextView blog_name = (TextView) label.findViewById(R.id.blog_name);
            LinearLayout content = (LinearLayout) label.findViewById(R.id.content);

            for (AsyncTask task : post.tasks) task.cancel(false);
            post.tasks.clear();

            date.setText(post.label.time);
            tags.setText("Теги: " + U.join(post.label.tags, ", "));
            title.setText(everypony.sweetieBot.U.deEntity(post.label.name));
            votes.setText(post.label.votes);
            author.setText(post.label.author.nick);
            comments.setText(post.label.comments + (post.label.comments_new > 0 ? "+" + post.label.comments_new : ""));
            blog_name.setText(post.label.blog.name);

            if (post.views == null) post.views = TextWrapper.wrap(post.label.content, content, twel);
            TextWrapper.insert(content, post.views);

            avatar.setImageDrawable(everypony.sweetieBot.U.res.getDrawable(R.drawable.refresh));

            AsyncTask task = ImageLoader.loadImage(post.label.author.mid_icon, new ImageLoader.InsertIntoView(avatar));
            if (task != null)
                post.tasks.add(task);

            try {
                int vote = U.parseInt(post.label.votes);
                votes.setTextColor(vote > 0
                        ? everypony.sweetieBot.U.res.getColor(R.color.Text_Green)
                        : everypony.sweetieBot.U.res.getColor(R.color.Text_Red));
            } catch (Exception ex) {
                votes.setTextColor(everypony.sweetieBot.U.res.getColor(R.color.Text_Default_Black));
            }


            title.setOnClickListener(listener);
        }
    }
}
