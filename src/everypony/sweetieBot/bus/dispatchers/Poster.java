package everypony.sweetieBot.bus.dispatchers;

import com.cab404.libtabun.data.Comment;
import com.cab404.libtabun.data.Letter;
import com.cab404.libtabun.data.Topic;
import com.cab404.libtabun.data.Types;
import com.cab404.libtabun.pages.LetterPage;
import com.cab404.libtabun.pages.TopicPage;
import com.cab404.libtabun.requests.CommentAddRequest;
import com.cab404.libtabun.requests.LSRequest;
import com.cab404.libtabun.requests.LetterAddRequest;
import com.cab404.libtabun.requests.TopicAddRequest;
import everypony.sweetieBot.U;
import everypony.sweetieBot.bus.Bus;
import everypony.sweetieBot.bus.events.PostRequestEvent;

/**
 * @author cab404
 */
public class Poster {

    @Bus.Handler
    public void handle(final PostRequestEvent event) {

        new Thread(new Runnable() {

            @Override public void run() {
                LSRequest request = null;

                if (event.toPost instanceof Topic) {
                    Topic topic = (Topic) event.toPost;
                    request = new TopicAddRequest(topic);
                }

                if (event.toPost instanceof Letter) {
                    Letter topic = (Letter) event.toPost;
                    request = new LetterAddRequest(topic);
                }

                if (event.toPost instanceof Comment) {
                    Comment comment = (Comment) event.toPost;

                    if (event.from instanceof LetterPage) {
                        request = new CommentAddRequest(Types.TALK,
                                ((LetterPage) event.from).header.id,
                                comment.parent,
                                comment.text
                        );
                    }

                    if (event.from instanceof TopicPage) {
                        request = new CommentAddRequest(Types.TALK,
                                ((TopicPage) event.from).header.id,
                                comment.parent,
                                comment.text
                        );
                    }

                }

                assert request != null;
                event.onFinish(request.exec(U.user, event.from).success());

            }

        }).start();
    }


}
