package everypony.sweetieBot.activities;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.view.*;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import com.cab404.libtabun.facility.HTMLParser;
import com.cab404.libtabun.parts.Blog;
import com.cab404.libtabun.parts.BlogList;
import com.cab404.libtabun.parts.PaWPoL;
import com.cab404.libtabun.parts.UserInfo;
import everypony.sweetieBot.R;
import everypony.sweetieBot.U;
import everypony.sweetieBot.other.ImageLoader;
import everypony.sweetieBot.other.MultitaskingActivity;
import everypony.sweetieBot.wrappers.BlogWrapper;
import everypony.sweetieBot.wrappers.PostWrapper;
import everypony.sweetieBot.wrappers.SettingsWrapper;

import java.util.Vector;

/**
 * Главный экран.
 *
 * @author cab404
 */
public class Home extends MultitaskingActivity {

    private DrawerLayout drawer;
    private View action_bar_view;
    private ListView content_list, navigation_list;

    public void onCreate(Bundle savedInstanceState) {
        U.v("==-==-== Запуск! ==-==-==");
        super.onCreate(savedInstanceState);
        ImageLoader.dropProgramCache();
        setContentView(R.layout.loading);


        // Запихаем папку с кэшем в U
        U.dumpAll(this);
        // Загружаем настройки.
        SettingsWrapper.SettingsAdapter.read();

        if (U.user == null)
            startActivityForResult(new Intent(getApplicationContext(), Login.class), 0);
        else afterInit();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0)
            afterInit();
    }

    public void afterInit() {
        if (U.user == null) {
            finish();
            return;
        }

        setContentView(R.layout.home_layout);

        // Пихаем всё, что нужно, в переменные.
        drawer = (DrawerLayout) findViewById(R.id.drawer);
        content_list = (ListView) findViewById(R.id.content);
        action_bar_view = findViewById(R.id.action_bar);
        navigation_list = (ListView) findViewById(R.id.navigation);
        navigation_list.setAdapter(new EmptyLoadingAdapter());

        findViewById(R.id.drawer_content).findViewById(R.id.settings).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                onSettingsInvoked();
            }
        });

        final Context up = this;
        findViewById(R.id.drawer_content).findViewById(R.id.about).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(up, AboutActivity.class));
            }
        });

        initPseudoBar();

        if (U.user.isLoggedIn()) {
            // Выставляем список в NavList
            generateNavList();
        } else {
            initNavList();
            navigation_list.setAdapter(nav_adapter);
        }


        // Достаём главную
        AsyncTask<String, Void, Blog> task = new GetPage().execute("/index/");
        addTask(task);

    }

    public void initPseudoBar() {
        action_bar_view = findViewById(R.id.action_bar);

        // Настраиваем панельку
        action_bar_view.findViewById(R.id.icon).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (drawer.isDrawerOpen(Gravity.START))
                    drawer.closeDrawer(Gravity.START);
                else
                    drawer.openDrawer(Gravity.START);
            }
        });


        // Ставим слушалку drawer-у
        drawer.setDrawerListener(new DrawerLayout.DrawerListener() {
            @Override public void onDrawerSlide(View view, float v) {
                if (U.SDK >= 11) {
                    View nav = drawer.findViewById(R.id.drawer_content);
                    int width = nav.getWidth();
                    nav.setX((width - width * v) * 0.5f);
                    content_list.setX(width * v);
                    action_bar_view.findViewById(R.id.drawer_indicator).setX(-U.dp(8) * v);
                } else
                    action_bar_view.offsetLeftAndRight((int) (-U.dp(8) * v));

            }

            @Override public void onDrawerOpened(View view) {
            }

            @Override public void onDrawerClosed(View view) {
            }

            @Override public void onDrawerStateChanged(int i) {
            }
        });


        // Настраиваем поиск

        if (Build.VERSION.SDK_INT >= 11) {
            final SearchView search_bar = (SearchView) action_bar_view.findViewById(R.id.search);

            search_bar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override public boolean onQueryTextSubmit(String query) {
                    content_list.setAdapter(null);
                    search_bar.clearFocus();
                    AsyncTask task = new GetPage().execute("/search/topics/?q=" + com.cab404.libtabun.U.rl(search_bar.getQuery() + "") + "&/");
                    addTask(task);
                    return true;
                }

                @Override public boolean onQueryTextChange(String newText) {
                    return false;
                }
            });

            search_bar.setOnSearchClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    action_bar_view.findViewById(R.id.title).setVisibility(View.GONE);
                }
            });

            search_bar.setOnCloseListener(new SearchView.OnCloseListener() {
                @Override public boolean onClose() {
                    action_bar_view.findViewById(R.id.title).setVisibility(View.VISIBLE);
                    return false;
                }
            });
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Слушаем кнопки на баре
        switch (item.getItemId()) {
//            case R.id.search_action_bar:
//                search();
//                break;
//            case R.id.write_action_bar:
//                write();
//                break;
            default:
                // По идее, всё, что мы не знаем - иконка.
                if (drawer.isDrawerOpen(Gravity.START))
                    drawer.closeDrawer(Gravity.START);
                else drawer.openDrawer(Gravity.START);
        }
        return super.onOptionsItemSelected(item);
    }

    private NavListAdapter nav_adapter;

    private void initNavList() {
        navigation_list.setAdapter(null);
        nav_adapter = new NavListAdapter();

        nav_adapter.labels.add(new PageLink("Главная", "/index/"));

        // Закидываем небольшую страничку с блогами.
        nav_adapter.labels.add(new ListLink("Блоги") {
            @Override void onInvoke() {
                drawer.closeDrawer(Gravity.START);
                final BlogWrapper.BlogListWrapper wrapper = new BlogWrapper.BlogListWrapper() {
                    @Override public void onItemClick(BlogList.BlogLabel blog_url) {
                        new PageLink(blog_url.name, "/blog/" + blog_url.url_name + "/").onInvoke();
                    }
                };
                content_list.setAdapter(wrapper);
            }
        });
    }

    /**
     * Создаём панель навигации.
     */
    private void generateNavList() {
        initNavList();

        nav_adapter.labels.add(new PageLink("Новые", "/index/newall/"));
        try {
            for (UserInfo.Userdata ud : U.user_info.personal) {
                HTMLParser blogs = null;

                if (ud.data_type == UserInfo.Userdata.UserdataType.BELONGS)
                    blogs = new HTMLParser(ud.value.replaceAll("\t", ""));
                if (ud.data_type == UserInfo.Userdata.UserdataType.ADMIN)
                    blogs = new HTMLParser(ud.value.replaceAll("\t", ""));
                if (ud.data_type == UserInfo.Userdata.UserdataType.CREATED)
                    blogs = new HTMLParser(ud.value.replaceAll("\t", ""));

                if (blogs == null) continue;

                for (HTMLParser.Tag blog : blogs.getAllTagsByName("a")) {
                    if (blog.isClosing) continue;
                    String name = blogs.getContents(blog);
                    String link = blog.props.get("href").replaceFirst("\\Qhttp://tabun.everypony.ru\\E", "");
                    nav_adapter.labels.add(new PageLink(name, link));
                }
            }
        } catch (NullPointerException e) {
            U.w("Пользователь не подписан никуда?");
            U.w(e);
        }


        // Добавляем юзерокошко.
        View view = getLayoutInflater().inflate(R.layout.user_label, navigation_list, false);
        ((TextView) view.findViewById(R.id.name)).setText(U.user_info.name);
        ((TextView) view.findViewById(R.id.nick)).setText(U.user_info.nick);
        ((TextView) view.findViewById(R.id.strength)).setText(U.user_info.strength + "");
        ((TextView) view.findViewById(R.id.votes)).setText(U.user_info.votes + "");
        ImageLoader.loadImage(U.user_info.big_icon, new ImageLoader.InsertIntoView((ImageView) view.findViewById(R.id.avatar)));
        navigation_list.addHeaderView(view);

        navigation_list.setAdapter(nav_adapter);
    }

    private void onSettingsInvoked() {
        content_list.setAdapter(new SettingsWrapper.SettingsAdapter());
        drawer.closeDrawer(Gravity.START);
        ((TextView) action_bar_view.findViewById(R.id.title)).setText("Настройки");
    }

    // ==-==-==-==-==-==-==-==-== Тут всякие local классы ==-==-==-==-==-==-==-==-==-==

    private boolean _lock_getpage = false;

    /**
     * Загружает и пихает страницу с лэйблами постов в content_view
     */
    private class GetPage extends AsyncTask<String, Void, Blog> {

        @Override protected void onPreExecute() {
            super.onPreExecute();
            content_list.setAdapter(new EmptyLoadingAdapter());
        }

        @Override protected Blog doInBackground(String... params) {
            if (_lock_getpage) return null;
            _lock_getpage = true;
            final String page_name = params[0];

            Blog page;

            page = new Blog(U.user, "") {
                @Override public String getUrl() {
                    return page_name;
                }
            };

            return page;
        }

        @Override protected void onPostExecute(Blog blog) {
            if (blog == null) return;
//            U.v("Пришла страничка!");
            PostWrapper.PostLabelList list = new PostWrapper.PostLabelList(blog);

            for (PaWPoL.PostLabel post : blog.posts) {
                list.add(post);
            }

            content_list.setAdapter(list);
            _lock_getpage = false;
        }
    }

    /**
     * Адаптер-заглушка со спиннером.
     */
    public static class EmptyLoadingAdapter extends U.FixedAdapter {

        public String title = "Что-то загружаем.", description = "Может и загрузим.";

        @Override public int getCount() {
            return 1;
        }

        @Override public Object getItem(int position) {
            return null;
        }

        @Override public long getItemId(int position) {
            return 0;
        }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.loading, parent, false);
                // Хватит на несколько часов, а больше и не надо.
//                convertView.findViewById(R.id.icon).animate().rotationBy(360000000L).setDuration(10000000L);
            }
            ((TextView) convertView.findViewById(R.id.title)).setText(title);
            ((TextView) convertView.findViewById(R.id.description)).setText(description);
            return convertView;
        }
    }

    /**
     * Небольшой интерфейсный класс для элементов drawer-а
     */
    private abstract class ListLabel {
        String title;

        abstract void onInvoke();
    }

    /**
     * ListLabel с ссылкой на страницу.
     */
    private class PageLink extends ListLabel {

        private String linked_to;

        private PageLink(String label, String linked_to) {
            this.linked_to = linked_to;
            this.title = label;
        }

        @Override void onInvoke() {
            drawer.closeDrawer(Gravity.START);
            ImageLoader.dropTasks();
            AsyncTask task = new GetPage() {
                @Override protected void onPostExecute(Blog blog) {
                    U.v("Загружено!");
                    super.onPostExecute(blog);
                    if (blog != null)
                        ((TextView) action_bar_view.findViewById(R.id.title)).setText(title);
                }
            }.execute(linked_to);
            addTask(task);
        }
    }

    private abstract class ListLink extends ListLabel {
        protected ListLink(String title) {
            this.title = title;
        }
    }

    /**
     * Адаптер drawer-а
     */
    private class NavListAdapter extends U.FixedAdapter {
        Vector<ListLabel> labels;

        private NavListAdapter() {
            labels = new Vector<>();
        }

        @Override public int getCount() {
            return labels.size();
        }

        @Override public Object getItem(int position) {
            return labels.get(position);
        }

        @Override public long getItemId(int position) {
            return labels.get(position).hashCode();
        }

        @Override public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inf = LayoutInflater.from(parent.getContext());
                convertView = inf.inflate(R.layout.drawer_element, parent, false);
            }
            TextView title = (TextView) convertView.findViewById(R.id.title);
            title.setText(labels.get(position).title);
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    labels.get(position).onInvoke();
                }
            });

            return convertView;
        }
    }
}