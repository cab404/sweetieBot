package everypony.sweetieBot.other;

import android.os.AsyncTask;
import everypony.sweetieBot.U;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Задание очистки кэша картинок до заданного размера
 *
 * @author Pahtet
 */
public class CacheCleanerTask extends AsyncTask<Long, Void, Boolean> {
    protected Boolean doInBackground(Long... args) {
        try {
            File target = new File(U.cacheDir.getPath() + "/Cached images/");

            long folderSize = getFolderSize(target); //Получаем размер папки с кэшем
            List<File> allFiles = getAllFilesInFolder(target);

            // Если лимит кэша 0, то есть он не ограничен, ничего не делаем.
            if (args[0] <= 0) return true;

            // Если размер папки с кэшем меньше лимита, то его и чистить не надо.
            if (args[0] > folderSize) return true;

            allFiles = (sortByDate(allFiles));

            while (folderSize > args[0]) {
                File f = allFiles.get(allFiles.size() - 1);
                folderSize -= f.length();
                f.delete();
                allFiles.remove(allFiles.size() - 1);
            }
        } catch (Exception e) {
            U.e(e);
            return false;
        }

        return true;
    }

    protected void onPostExecute(Boolean result) {
        if (result) {
            U.v("Кэш очищен!");
        } else {
            U.e("Кэш не был очищен!");
        }
    }

    /**
     * Возвращает размер папки вместе со всеми вложенными файлами.
     */
    private long getFolderSize(File directory) {
        long length = 0;

        if (directory.exists()) {
            for (File file : directory.listFiles()) {
                if (file.isFile())
                    length += file.length();
                else
                    length += getFolderSize(file);
            }
        }

        return length;
    }

    /**
     * Возвращает список всех файлов в папке.
     */
    private List<File> getAllFilesInFolder(File directory) {
        List<File> out = new ArrayList<>();

        if (directory.exists()) {
            for (File file : directory.listFiles()) {
                if (file.isFile())
                    out.add(file);
                else
                    out.addAll(getAllFilesInFolder(file));
            }
        }
        return out;
    }

    /**
     * Сортирует список файлов по дате изменения
     */
    private List<File> sortByDate(List<File> in) {

        Collections.sort(in, new Comparator<File>() {
            public int compare(File o1, File o2) {
                return (int) Math.signum(o1.lastModified() - o2.lastModified());
            }
        });

        return in;
    }
}
