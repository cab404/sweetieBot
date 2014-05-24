package everypony.sweetieBot.bus.events;

import com.cab404.libtabun.pages.TabunPage;

/**
 * @author cab404
 */
public class PostRequestEvent {
    public final Object toPost;
    public final TabunPage from;

    public PostRequestEvent(Object toPost, TabunPage from) {
        this.toPost = toPost;
        this.from = from;
    }

    public void onFinish(boolean success) {}

}
