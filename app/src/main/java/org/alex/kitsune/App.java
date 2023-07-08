package org.alex.kitsune;

import android.app.Application;
import androidx.preference.PreferenceManager;
import org.acra.ACRA;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.data.StringFormat;
import org.alex.kitsune.logs.Logs;
import org.alex.kitsune.services.BookService;
import org.alex.kitsune.ui.main.Constants;
import org.alex.kitsune.utils.Updater;
import org.alex.kitsune.utils.Utils;
import org.alex.kitsune.utils.acra.JSONFormatter;
import org.alex.kitsune.utils.acra.MailSenderConfiguration;
import java.io.File;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Logs.init(this);
        ACRA.init(this, new CoreConfigurationBuilder()
                .withBuildConfigClass(BuildConfig.class)
                .withReportFormat(StringFormat.JSON)
                .withPluginConfigurations(
                        new MailSenderConfiguration.Builder()
                                .addEmail("kitsune.logs@gmail.com")
                                .setFormatter(new JSONFormatter(1))
                                .setFile("report.json")
                                .build()
                )
        );
        PreferenceManager.getDefaultSharedPreferences(this).edit().putString(Constants.saved_path, BookService.init(this)).apply();
        Updater.init(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        File[] listF=new File(BookService.getCacheDir()).listFiles();
        if(listF!=null){for(File f:listF){Utils.File.delete(f);}}
    }
}
