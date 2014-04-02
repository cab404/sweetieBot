package everypony.sweetieBot.wrappers;

import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.cab404.libtabun.parts.BlogList;
import everypony.sweetieBot.R;
import everypony.sweetieBot.U;

import java.util.ArrayList;

/**
 * Классы и функции для обработки списков обложек постов.
 *
 * @author cab404
 */
public class BlogWrapper {

    /**
     * Список блогов с подгрузкой.
     */
    public static abstract class BlogListWrapper extends U.FixedAdapter {
        ArrayList<BlogList.BlogLabel> labels;
        public BlogList list;
        private boolean finished = false, loading = false;

        protected BlogListWrapper() {
            this.list = new BlogList();
            this.labels = new ArrayList<>();
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
         * Загружает новые блоги со следующей странички.
         */
        private void startLoadingInsteadOfPaginator() {
            if (!loading) {
                U.v("Доскролили до низу, загружаю новые блоги...");
                loading = true;
                new AsyncTask<Void, Void, Boolean>() {
                    @Override protected Boolean doInBackground(Void... params) {
                        return list.loadNextPage(U.user);
                    }

                    @Override protected void onPostExecute(Boolean aBoolean) {
                        if (aBoolean) {
                            if (labels.size() > 0 && list.labels.contains(labels.get(0)))
                                finished = true;
                            else if (list.labels.size() == 0)
                                finished = true;
                            else
                                labels.addAll(list.labels);
                        } else finished = true;
                        loading = false;
                        notifyDataSetChanged();
                    }
                }.execute();
            }
        }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (convertView == null || convertView.getId() == R.id.loading) {
                convertView = inflater.inflate(R.layout.blog_label, parent, false);
            }

            if (position == labels.size()) {
                startLoadingInsteadOfPaginator();
                return inflater.inflate(R.layout.loading, parent, false);
            }

            convertView(convertView, labels.get(position));
            return convertView;
        }


        /**
         * Превращает view с блогом во view с другим блогом.
         */
        public void convertView(View view, final BlogList.BlogLabel label) {
            ((TextView) view.findViewById(R.id.name)).setText(label.name);
            ((TextView) view.findViewById(R.id.rating)).setText(label.votes + "");
            ((TextView) view.findViewById(R.id.people)).setText(label.readers + "");
            ((TextView) view.findViewById(R.id.rating)).setTextColor(label.votes < 0 ? U.res.getColor(R.color.Text_Red) : U.res.getColor(R.color.Text_Green));
            view.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    onItemClick(label);
                }
            });
        }

        /**
         * Что происходит, когда по блогу кликают чем-нибудь?
         */
        public abstract void onItemClick(BlogList.BlogLabel blog_url);
    }
}
