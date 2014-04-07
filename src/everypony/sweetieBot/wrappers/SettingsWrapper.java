package everypony.sweetieBot.wrappers;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.ToggleButton;
import everypony.sweetieBot.R;
import everypony.sweetieBot.U;
import everypony.sweetieBot.activities.PostActivity;
import everypony.sweetieBot.other.ImageLoader;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author cab404
 */
public class SettingsWrapper {
    static List<Map.Entry<String, ?>> settings;

    static {
        settings = new ArrayList<>();

        settings.add(new Map.Entry<String, Boolean>() {
            @Override public String getKey() {
                return "Загрузка картинок";
            }
            @Override public Boolean getValue() {
                return TextWrapper.isPicsDownloading;
            }
            @Override public Boolean setValue(Boolean object) {
                TextWrapper.isPicsDownloading = object;
                SettingsAdapter.write();
                return TextWrapper.isPicsDownloading;
            }
        });

        settings.add(new Map.Entry<String, Boolean>() {
            @Override public String getKey() {
                return "Загрузка gif-анимации";
            }
            @Override public Boolean getValue() {
                return TextWrapper.isGIFsDownloading;
            }
            @Override public Boolean setValue(Boolean object) {
                TextWrapper.isGIFsDownloading = object;
                SettingsAdapter.write();
                return TextWrapper.isGIFsDownloading;
            }
        });

        settings.add(new Map.Entry<String, Boolean>() {
            @Override public String getKey() {
                return "Многопоточная загрузка (для быстрого соединения)";
            }
            @Override public Boolean getValue() {
                return ImageLoader.is_Multi_thread;
            }
            @Override public Boolean setValue(Boolean object) {
                ImageLoader.is_Multi_thread = object;
                SettingsAdapter.write();
                return ImageLoader.is_Multi_thread;
            }
        });

        settings.add(new Map.Entry<String, Boolean>() {
            @Override public String getKey() {
                return "Быстрая прокрутка до новых комментариев";
            }
            @Override public Boolean getValue() {
                return PostActivity.insta_scroll;
            }
            @Override public Boolean setValue(Boolean object) {
                PostActivity.insta_scroll = object;
                SettingsAdapter.write();
                return PostActivity.insta_scroll;
            }
        });

        settings.add(new Map.Entry<String, Boolean>() {
            @Override public String getKey() {
                return "Панель действий в посте справа.";
            }
            @Override public Boolean getValue() {
                return PostActivity.panel_on_right;
            }
            @Override public Boolean setValue(Boolean object) {
                PostActivity.panel_on_right = object;
                SettingsAdapter.write();
                return PostActivity.panel_on_right;
            }
        });

    }

    public static class SettingsAdapter extends U.FixedAdapter {

        @Override
        public int getCount() {
            return settings.size();
        }

        @Override
        public Object getItem(int position) {
            return settings.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }


        @SuppressWarnings("unchecked")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final Map.Entry<String, ?> conv = settings.get(position);
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            Class setting_class = conv.getValue().getClass();

            if (convertView == null)
                convertView = inflater.inflate(R.layout.settings_entry, parent, false);


            ViewGroup content = ((ViewGroup) convertView.findViewById(R.id.content));
            ((TextView) convertView.findViewById(R.id.title)).setText(conv.getKey());

            content.removeAllViews();

            if (setting_class.equals(Boolean.class)) {
                ToggleButton swi = new ToggleButton(convertView.getContext());
                swi.setChecked((Boolean) conv.getValue());
                swi.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        ((Map.Entry<String, Boolean>) conv).setValue(isChecked);
                    }
                });
                content.addView(swi);
            }

            if (setting_class.equals(Number.class)) {
                NumberPicker picker = new NumberPicker(convertView.getContext());
                picker.setValue(((Map.Entry<String, Number>) conv).getValue().intValue());
                picker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                    @Override public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                        ((Map.Entry<String, Number>) conv).setValue(newVal);
                    }
                });
                content.addView(picker);
            }

            return convertView;
        }

        public static void write() {
            try {
                ObjectOutputStream str =
                        new ObjectOutputStream(
                                new BufferedOutputStream(
                                        U.getSettingsO()
                                )
                        );

                for (Map.Entry<String, ?> setting : settings) {
                    str.writeObject(setting.getValue());
                }
                str.flush();

                str.close();
            } catch (IOException e) {
                U.e(e);
            }
        }


        @SuppressWarnings("unchecked")
        public static void read() {
            try {
                ObjectInputStream str =
                        new ObjectInputStream(
                                new BufferedInputStream(U.getSettingsI()
                                )
                        );

                for (Map.Entry<String, ?> setting : settings) {
                    Map.Entry<String, Object> t = (Map.Entry<String, Object>) setting;
                    t.setValue(str.readObject());
                }

                str.close();
            } catch (IOException | ClassNotFoundException e) {
                U.e(e);
                write();
            }
        }
    }

}
