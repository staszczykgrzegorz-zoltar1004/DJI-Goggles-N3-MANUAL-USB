package pl.stasz.n3manual;

import android.app.Activity;
import android.app.LocaleManager;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;
import java.util.Locale;

/** One picker, native Android 13+ preferences, private preferences on Android 6–12. */
public final class AppLanguage {
    private AppLanguage() {}
    public static String choice(Context context) {
        if(Build.VERSION.SDK_INT>=33) {
            LocaleList list=context.getSystemService(LocaleManager.class).getApplicationLocales();
            return list.isEmpty()?"system":("pl".equals(list.get(0).getLanguage())?"pl":"en");
        }
        String saved=context.getSharedPreferences("language",Context.MODE_PRIVATE).getString("choice","system");
        return "en".equals(saved)||"pl".equals(saved)?saved:"system";
    }
    public static Context wrap(Context context) {
        String choice=choice(context);
        Configuration system=Resources.getSystem().getConfiguration();
        Locale primary=Build.VERSION.SDK_INT>=24?system.getLocales().get(0):system.locale;
        Locale locale="pl".equals(choice)||("system".equals(choice)&&"pl".equals(primary.getLanguage()))?new Locale("pl"):Locale.ENGLISH;
        Configuration config=new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        if(Build.VERSION.SDK_INT>=24)config.setLocales(new LocaleList(locale));
        return context.createConfigurationContext(config);
    }
    public static void set(Activity activity,String value) {
        if(!"en".equals(value)&&!"pl".equals(value)&&!"system".equals(value))throw new IllegalArgumentException("language");
        String old=choice(activity);
        if(Build.VERSION.SDK_INT>=33) {
            LocaleList locales="system".equals(value)?LocaleList.getEmptyLocaleList():LocaleList.forLanguageTags(value);
            activity.getSystemService(LocaleManager.class).setApplicationLocales(locales);
            if(old.equals(value))activity.recreate();
        } else {
            activity.getSharedPreferences("language",Context.MODE_PRIVATE).edit().putString("choice",value).apply();
            activity.recreate();
        }
    }
}
