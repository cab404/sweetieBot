package everypony.sweetieBot.other;

import android.app.Activity;
import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.ListIterator;

/**
 * Activity, которое порождает тысячи потоков, которые надо закрыть.
 */
public class MultitaskingActivity extends Activity {

    private final ArrayList<AsyncTask> parallel = new ArrayList<>();


    public synchronized void addTask(AsyncTask task) {
        parallel.add(task);
        checkParallels();
    }

    /**
     * Удаляет законченные параллельные задания
     */
    protected synchronized void checkParallels() {
        synchronized (parallel) {
            ListIterator<AsyncTask> iterator = parallel.listIterator();
            while (iterator.hasNext()) {
                AsyncTask task = iterator.next();
                if (task.getStatus().equals(AsyncTask.Status.FINISHED) || task.isCancelled()) {
                    iterator.remove();
                }
            }
        }
    }

    @Override
    protected synchronized void onDestroy() {
        ImageLoader.dropTasks();
        new CacheCleanerTask().execute(10L * 1024L * 1024L);
        synchronized (parallel) {
            for (AsyncTask task : parallel) task.cancel(true);
        }
        super.onDestroy();
    }
}
