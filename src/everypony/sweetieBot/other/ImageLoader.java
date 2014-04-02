package everypony.sweetieBot.other;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.Log;
import android.widget.ImageView;
import everypony.sweetieBot.U;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.AllClientPNames;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.*;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.WeakHashMap;

/**
 * Простой загрузчик изображений из Сети со статичным кэшем.
 * Оный (есс-но, не файловый) неплохо бы сделать нестатичным.
 * А лучше - синглтоном.
 *
 * @author cab404
 */
public class ImageLoader {

    /**
     * <b>Плюсы и минусы:</b>
     * <p/>
     * <b>При многопоточности</b> (true) резко уменьшается скорость загрузки каждого изображения,
     * и изображение может вообще может не прогрузится с Timeout.
     * <p/>
     * <b>При цепной загрузке</b> изображения загружаются по одному и куда быстрее,
     * куда меньше вероятность Timeout-а, но вся цепочка может остановится
     * на каком-нибудь огромном арте.
     */
    public static boolean is_Multi_thread = false;
    public static int retries = 10, timeout = 5000;
    protected static ArrayList<LoadingTask> taskQueue;
    protected static WeakHashMap<String, LoadingImage> cache;
    public static int lim_x = 800;

    static {
        taskQueue = new ArrayList<>();
        cache = new WeakHashMap<>();
    }

    /**
     * Убивает все задания и очищает память от уже загруженных картинок.
     */
    public static void dropProgramCache() {
        dropTasks();
        for (LoadingImage img : cache.values()) {
            if (img != null && img.bitmap != null)
                img.bitmap.clear();
        }
        cache.clear();
    }

    /**
     * Убивает все задания.
     */
    public static void dropTasks() {
        for (AsyncTask task : taskQueue)
            task.cancel(true);
        taskQueue.clear();
    }


    /**
     * Загружает картинку из Сети по данному адресу, и пихает во View.
     *
     * @param address URL картинки. Если есть в файлах, то из кэша.
     * @param handler Метод, выполняемый после загрузки.
     */
    public static AsyncTask loadImage(String address, OnImageLoaded handler) {

        StringBuilder fin = new StringBuilder(address);
        if (address.startsWith("//")) fin.insert(0, "http:");
        address = fin.toString();

        synchronized (cache) {
            LoadingImage img = cache.get(address);

            if (img != null && img.isValid()) {
                // Если в кэше уже есть запись - значит изображение загружено/загружается
                // Если загружено - просто достаём из кэша и пихаем во view
                // Если нет - добавляем задание, дабы после загрузки запихать его во view

                if (img.isLoaded)

                    try {

                        if (img.bitmap.get() != null) {
                            handler.loaded(img.bitmap.get());
                            return null;
                        }

                    } catch (Throwable t) {
                        Log.e("Luna Log", "Случилась какая-то фигня, когда мы отгружали картинку из кэша на место.", t);
                        return null;
                    }

                else {
                    img.toExecUponLoading.add(handler);
                    return null;
                }
            }
            if (isThereAnythingInCache(address, handler.isCaching())) {
                try {
                    handler.loaded(loadBitmap(address, handler.isCaching()));
                    return null;
                } catch (Throwable t) {
                    Log.e("Luna Log", "Случилась какая-то фигня, когда мы отгружали картинку из кэша на место.", t);
                    return null;
                }
            }


            // Если же картинки в кэше нет - делаем запись, отмечая, что картинку будем сейчас обрабатывать.
            // Добавляем в очередь задании новое. Для пущей производительности лучше не загружать что-то
            // одновременно, но можно переключать режимы в is_Multi_thread.
            // Если это единственное задание - запускаем, потом оно по цепочке запустит остальные,
            // если те будут добавлены.

            LoadingTask loadingTask = new LoadingTask(address);
            LoadingImage onLoad = new LoadingImage();

            onLoad.toExecUponLoading.add(handler);
            cache.put(address, onLoad);

            if (!is_Multi_thread) {
                taskQueue.add(loadingTask);

                if (taskQueue.size() == 1) {
                    loadingTask.execute(handler.isCaching());
                }
            } else {
                loadingTask.execute(handler.isCaching());
            }

            return loadingTask;

        }
    }

    private static boolean isThereAnythingInCache(String uri, boolean long_cached) {
        File file;
        try {
            if (long_cached)
                file = new File(U.cacheDir.getPath() + "/Static images/" + URLEncoder.encode(uri, "UTF-8"));
            else
                file = new File(U.cacheDir.getPath() + "/Cached images/" + URLEncoder.encode(uri, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            U.wtf(e);
            return false;
        }

        return file.exists();
    }

    /**
     * Загружает Bitmap
     *
     * @param uri Адрес картинки
     */
    public static Bitmap loadBitmap(String uri, boolean long_cached) {

        Bitmap bitmap = null;
        HttpClient client;
        HttpGet get;
        File file;
        try {
            if (long_cached)
                file = new File(U.cacheDir.getPath() + "/Static images/" + URLEncoder.encode(uri, "UTF-8"));
            else
                file = new File(U.cacheDir.getPath() + "/Cached images/" + URLEncoder.encode(uri, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            U.wtf(e);
            return null;
        }

        try {
            // Если картинки нет - загружаем.
            if (!file.exists()) {
                client = new DefaultHttpClient();
                client.getParams().setLongParameter(AllClientPNames.TIMEOUT, timeout);
                get = new HttpGet(uri);

                // Получаем Response
                HttpResponse response = client.execute(get);

                // Дети, никогда не лезте в предыдущие коммиты, там неприличное зрелище.
                file.delete();
                file.getParentFile().mkdirs();
                file.createNewFile();

                FileOutputStream writer = new FileOutputStream(file);

                // Сохраняем в кэш
                response.getEntity().writeTo(writer);

                writer.close();
            }

            BufferedInputStream input = new BufferedInputStream(new FileInputStream(file));

            // Обрабатываем картинку
            bitmap = BitmapFactory.decodeStream(new BufferedInputStream(input));

            input.close();
        } catch (Throwable e) {
            if (e instanceof HttpHostConnectException) {
                U.e("Проблема с загрузкой картинки, в соединении отказано. Отбой.");
            } else {
                U.w(e);
                if (file.exists() && !file.delete())
                    throw new Error("Ошибка удаления повреждённого файла!");
            }
        }

        return bitmap;
    }

    /**
     * Задание загрузки. Скачивает, и кладёт изображение в обработчик и кэш
     */
    private static class LoadingTask extends AsyncTask<Boolean, Void, Bitmap> {

        private final String address;
        private boolean isCaching;

        public LoadingTask(String from) {
            address = from;
        }

        @Override
        protected Bitmap doInBackground(Boolean... in) {
            isCaching = in[0];
            Bitmap ret;
            int tries = 0;
            do {
                ret = loadBitmap(address, in[0]);
                tries++;
            } while (ret == null && tries < retries);

//            if (ret == null) U.v("Не удалось загрузить изображение, попыток: " + tries);
//            else U.v("Изображение загружено, количество попыток: " + tries);

            return ret;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            try {
                LoadingImage img = cache.get(address);
                if (img == null) return;

                // Пихаем картинку в кэш
                img.bitmap = new WeakReference<>(bitmap);
                // Отдаём себе, и тем, кому она еще понадобилась
                img.execUponLoading(bitmap);
            } catch (Throwable t) {
                U.e("Ошибка при выполнении слушалки загрузки!");
                U.e(t);
            }


            if (!is_Multi_thread) {
                try {
                    // Удаляем себя из очереди заданий
                    taskQueue.remove(0);
                    // Пропускаем законченные задания.
                    while (true) {
                        // Продолжаем цепочку заданий, если есть куда
                        if (taskQueue.size() > 0) {
                            LoadingTask first = taskQueue.get(0);
                            if (first.getStatus() == Status.FINISHED) {
                                taskQueue.remove(0);
                                continue;
                            }
                            first.execute(first.isCaching);
                            break;
                        } else break;
                    }
                } catch (Throwable ex) {
                    U.e(ex);
                    U.e("Видимо, был переключем режим загрузки.");
                }
            }
        }
    }

    /**
     * Класс, хранящий изображение и обработчики. Во время загрузки изображения
     * все обработчики сразу кладутся сюда и выполняются execUponLoading. После того,
     * как Bitmap загружен он хранится здесь, вызываемым обработчикам изображение передаётся
     * напрямую отсюда.
     */
    private static class LoadingImage {
        public Reference<Bitmap> bitmap;
        public boolean isLoaded = false;
        /**
         * Обработчики загруженного Bitmap-а
         *
         * @see Void execUponLoading(Bitmap result)
         */
        public final ArrayList<OnImageLoaded> toExecUponLoading;

        public LoadingImage() {
            bitmap = new WeakReference<>(null);
            toExecUponLoading = new ArrayList<>();
        }

        public boolean isValid() {
            // Что я тут раньше писал, вашу кобылу?!?!
            return isLoaded || bitmap.get() != null;
        }

        /**
         * Функция, выполняющяя все обработчики в toExecUponLoading
         */
        public void execUponLoading(Bitmap result) {
            isLoaded = true;
            boolean memcache = false;
            // Даём картинки страждущим
            for (OnImageLoaded ref : toExecUponLoading) {
                if (ref != null) {
                    memcache |= ref.isCaching();
                    try {
                        ref.loaded(result);
                    } catch (Throwable t) {
                        if (!(t instanceof NullPointerException))
                            U.w(t);
                    }
                }
            }
            // Нужно убить большую часть кэша в RAM, это верный Out Of Memory.
            // Посему, тем, кто не успел - в зубы тапки и за кэшем с диска
            if (!memcache)
                bitmap.clear();
        }
    }

    /**
     * Интерфейс обработчика загруженного изображения
     */
    public interface OnImageLoaded {
        /**
         * Выполняется после загрузки изображения.
         *
         * @param bitmap Загруженный Bitmap
         */
        public abstract void loaded(Bitmap bitmap);

        public abstract boolean isCaching();
    }

    /**
     * Обработчик, пихающий загруженной изображение в ImageView
     */
    public static class InsertIntoView implements OnImageLoaded {
        final WeakReference<ImageView> ref;

        public InsertIntoView(ImageView view) {
            ref = new WeakReference<>(view);
        }

        @Override
        public void loaded(Bitmap bitmap) {
            ref.get().setImageBitmap(bitmap);
//            ref.get().invalidate();
        }

        @Override public boolean isCaching() {
            return true;
        }
    }

    /**
     * Обработчик, пихающий изображение в текст в виде ImageSpan
     */
    public static class InsertIntoSpan implements OnImageLoaded {
        private final WeakReference<Editable> ref;
        private final int sI, fI;

        /**
         * @param into Куда пихать
         * @param sI   Индекс начала ImageSpan-а
         * @param fI   Индекс конца ImageSpan-а
         */
        public InsertIntoSpan(Editable into, int sI, int fI) {
            ref = new WeakReference<>(into);
            this.sI = sI;
            this.fI = fI;
        }


        @Override
        public void loaded(Bitmap bitmap) {
            if (bitmap == null) return;
            bitmap.setDensity(U.res.getDisplayMetrics().densityDpi);

            if (bitmap.getWidth() > lim_x /*|| bitmap.getHeight() > lim_y*/) {
                int sx = bitmap.getWidth(), sy = bitmap.getHeight();

                if (sx > lim_x) {
                    sy *= lim_x / (float) sx;
                    sx = lim_x;
                }
//                if (sy > lim_y) {
//                    sx *= lim_y / (float) sy;
//                    sy = lim_y;
//                }

                if (sx > 800) {
                    sy *= 800 / (float) sx;
                    sx = 800;
                }
//                if (sy > 800) {
//                    sx *= 800 / (float) sy;
//                    sy = 800;
//                }

                bitmap = Bitmap.createScaledBitmap(bitmap, sx, sy, true);
            }

            ImageSpan span = new ImageSpan(U.context, bitmap);
            Editable got = ref.get();
            if (got != null)
                got.setSpan(
                        span,
                        sI,
                        fI,
                        SpannableString.SPAN_INCLUSIVE_EXCLUSIVE
                );
        }

        @Override public boolean isCaching() {
            return false;
        }
    }
}
