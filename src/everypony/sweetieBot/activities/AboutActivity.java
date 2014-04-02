package everypony.sweetieBot.activities;

import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;
import everypony.sweetieBot.R;

/**
 * @author cab404
 */
public class AboutActivity extends Activity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_layout);
        ((TextView) findViewById(R.id.about)).setText(Html.fromHtml(getResources().getString(R.string.tyftf)));
    }
}
