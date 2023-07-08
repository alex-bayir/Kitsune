package org.alex.kitsune.utils.acra;

import android.content.Context;
import org.acra.config.CoreConfiguration;
import org.acra.plugins.HasConfigPlugin;
import com.google.auto.service.AutoService;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderFactory;
import org.jetbrains.annotations.NotNull;

@AutoService(ReportSenderFactory.class)
public class EmailIntentSenderFactory extends HasConfigPlugin implements ReportSenderFactory{
    public EmailIntentSenderFactory() {
        super(MailSenderConfiguration.class);
    }

    @Override
    public boolean enabled(@NotNull CoreConfiguration config) {
        return true;
    }

    @NotNull
    public ReportSender create(@NotNull Context context, @NotNull CoreConfiguration coreConfiguration) {
        return new EmailIntentSender(coreConfiguration);
    }
}
