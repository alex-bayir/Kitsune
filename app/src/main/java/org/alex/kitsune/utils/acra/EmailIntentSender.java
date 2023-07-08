package org.alex.kitsune.utils.acra;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.util.Pair;
import androidx.annotation.ChecksSdkIntAtLeast;
import org.acra.ReportField;
import org.acra.attachment.AcraContentProvider;
import org.acra.attachment.DefaultAttachmentProvider;
import org.acra.config.CoreConfiguration;
import org.acra.data.CrashReportData;
import org.acra.data.StringFormat;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;
import org.acra.util.InstanceCreator;
import org.alex.kitsune.logs.Logs;
import org.alex.kitsune.utils.Utils;
import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import static org.acra.util.IOUtils.writeStringToFile;


public class EmailIntentSender implements ReportSender {

    public interface Formatter{
        String format(CrashReportData data, List<ReportField> order, String mainJoiner, String subJoiner, Boolean urlEncode) throws Exception;
    }
    protected final CoreConfiguration config;
    protected final MailSenderConfiguration mail_config;

    protected Formatter formatter;

    public EmailIntentSender(CoreConfiguration config) {
        this.config=config;
        this.mail_config=(MailSenderConfiguration) config.getPluginConfigurations().stream().filter(plugin->plugin instanceof MailSenderConfiguration).findFirst().orElse(null);
        formatter=mail_config!=null && mail_config.formatter!=null ? mail_config.formatter:StringFormat.JSON::toFormattedString;
    }

    @Override
    public void send(@NotNull Context context, @NotNull CrashReportData error) throws ReportSenderException {
        Logs.saveLog(System.currentTimeMillis(),error.getString(ReportField.STACK_TRACE),true);
        String subject = buildSubject(error);
        String report;
        try {
            report = formatter.format(error, config.getReportContent(), "\n", "\n  ", false);
        } catch (Exception e) {
            throw new ReportSenderException("Failed to convert Report to text", e);
        }
        List<Uri> attachments;
        String body;
        Pair<String, List<Uri>> bodyAndAttachments = getBodyAndAttachments(context, report);
        body=bodyAndAttachments.first+"\n\nStack Trace:\n"+error.getString(ReportField.STACK_TRACE);
        attachments = bodyAndAttachments.second;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            sendWithSelector(subject, body, attachments, context);
        } else {
            resolveAndSend(subject, body, attachments, context);
        }
    }
    protected String buildSubject(CrashReportData error){
        return Utils.group(error.getString(ReportField.STACK_TRACE),"([^.]+): ");
    }

    private void sendWithSelector(String subject, String body, List<Uri> attachments, Context context) throws ReportSenderException {
        Intent intent=attachments.size()==1?
                buildSingleAttachmentIntent(subject, body, attachments.get(0))
                :
                buildAttachmentIntent(subject, body, attachments);
        intent.setSelector(buildResolveIntent());

        grantPermission(context, intent, null, attachments);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            try {
                e.printStackTrace();
                resolveAndSend(subject, body, attachments, context);
            }catch (ActivityNotFoundException e2) {
                throw new ReportSenderException("No email client found", e2);
            }
        }
    }
    private void resolveAndSend(String subject, String body, List<Uri> attachments, Context context) throws ReportSenderException {
        PackageManager pm = context.getPackageManager();
        //we have to resolve with sendto, because send is supported by non-email apps
        Intent resolveIntent = buildResolveIntent();
        ComponentName resolveActivity = resolveIntent.resolveActivity(pm);
        if (resolveActivity != null) {
            if (attachments.isEmpty()) {
                //no attachments, send directly
                context.startActivity(buildFallbackIntent(subject, body));
            } else {
                Intent attachmentIntent = buildAttachmentIntent(subject, body, attachments);
                Intent altAttachmentIntent = new Intent(attachmentIntent).setType("*/*"); // gmail will only match with type set
                List<Intent> initialIntents = buildInitialIntents(pm, resolveIntent, attachmentIntent);
                String packageName = getPackageName(resolveActivity, initialIntents);
                attachmentIntent.setPackage(packageName);
                altAttachmentIntent.setPackage(packageName);
                if(packageName==null){
                    for (Intent intent : initialIntents) {
                        grantPermission(context, intent, intent.getPackage(), attachments);
                    }
                    showChooser(context, initialIntents);
                }else if(attachmentIntent.resolveActivity(pm) != null){
                    grantPermission(context, attachmentIntent, packageName, attachments);
                    context.startActivity(attachmentIntent);
                }else if(altAttachmentIntent.resolveActivity(pm) != null){
                    grantPermission(context, altAttachmentIntent, packageName, attachments);
                    context.startActivity(altAttachmentIntent);
                }else{
                    Log.w("Warning","No email client supporting attachments found. Attachments will be ignored");
                    context.startActivity(buildFallbackIntent(subject, body));
                }
            }
        } else {
            throw new ReportSenderException("No email client found");
        }
    }

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
    @Override
    public boolean requiresForeground() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    }

    private void showChooser(Context context, List<Intent> initialIntents) {
        context.startActivity(
                new Intent(Intent.ACTION_CHOOSER)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra(Intent.EXTRA_INTENT, initialIntents.remove(0))
                        .putExtra(Intent.EXTRA_INITIAL_INTENTS, initialIntents.toArray(new Intent[0]))
        );
    }

    private void grantPermission(Context context, Intent intent, String packageName, List<Uri> attachments) {
        if (packageName == null) {
            for (ResolveInfo resolveInfo : queryDefaultActivities(context.getPackageManager(),intent)) {
                grantPermission(context, intent, resolveInfo.activityInfo.packageName, attachments);
            }
        } else {
            for (Uri uri : attachments) {
                context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
        }
    }

    /**
     * Finds the package name of the default email client supporting attachments
     *
     * @param resolveActivity the resolved activity
     * @param initialIntents  a list of intents to be used when
     * @return package name of the default email client, or null if more than one app match
     */
    private String getPackageName(ComponentName resolveActivity, List<Intent> initialIntents){
        String packageName = resolveActivity.getPackageName();
        if (Objects.equals(packageName, "android")) {
            if (initialIntents.size() > 1) {
                //multiple activities support the intent and no default is set
                packageName = null;
            } else if (initialIntents.size() == 1) {
                //only one of them supports attachments, use that one
                packageName = initialIntents.get(0).getPackage();
            }
        }
        return packageName;
    }

    /**
     * Builds an email intent with attachments
     *
     * @param subject         the message subject
     * @param body            the message body
     * @param attachments     the attachments
     * @return email intent
     */
    protected Intent buildAttachmentIntent(String subject, String body, List<Uri> attachments){
        return new Intent(Intent.ACTION_SEND_MULTIPLE)
        .putExtra(Intent.EXTRA_EMAIL, mail_config.emails)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        .putExtra(Intent.EXTRA_SUBJECT, subject)
        .putParcelableArrayListExtra(Intent.EXTRA_STREAM, new ArrayList<>(attachments))
        .putExtra(Intent.EXTRA_TEXT, body);
    }

    /**
     * Builds an email intent with one attachment
     *
     * @param subject         the message subject
     * @param body            the message body
     * @param attachment     the attachment
     * @return email intent
     */
    protected Intent buildSingleAttachmentIntent(String subject, String body, Uri attachment) {
        return new Intent(Intent.ACTION_SEND)
        .putExtra(Intent.EXTRA_EMAIL, mail_config.emails)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        .putExtra(Intent.EXTRA_SUBJECT, subject)
        .putExtra(Intent.EXTRA_STREAM, attachment)
        .putExtra(Intent.EXTRA_TEXT, body);
    }

    /**
     * Builds an intent used to resolve email clients
     *
     * @return email intent
     */
    protected Intent buildResolveIntent() {
        return new Intent(Intent.ACTION_SENDTO).setData(Uri.parse("mailto:")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    protected Intent buildFallbackIntent(String subject, String body) {
        return new Intent(Intent.ACTION_SENDTO)
        .setData(Uri.parse("mailto:"+mail_config.emails[0]+"?subject="+Uri.encode(subject)+"&body="+Uri.encode(body)))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        .putExtra(Intent.EXTRA_SUBJECT, subject)
        .putExtra(Intent.EXTRA_TEXT, body);
    }

    private List<Intent> buildInitialIntents(PackageManager pm, Intent resolveIntent,Intent emailIntent) {
        List<ResolveInfo> resolveInfoList = queryDefaultActivities(pm,resolveIntent);
        ArrayList<Intent> initialIntents = new ArrayList<>();
        for (ResolveInfo info:resolveInfoList) {
            Intent packageSpecificIntent = new Intent(emailIntent);
            packageSpecificIntent.setPackage(info.activityInfo.packageName);
            if (packageSpecificIntent.resolveActivity(pm) != null) {
                initialIntents.add(packageSpecificIntent);
                continue;
            }
            packageSpecificIntent.setClassName(info.activityInfo.packageName, info.activityInfo.name);
            if(packageSpecificIntent.resolveActivity(pm) != null) {
                initialIntents.add(packageSpecificIntent);
            }
        }
        return initialIntents;
    }


    protected Pair<String, List<Uri>> getBodyAndAttachments(Context context, String report) {
        String prefix = mail_config.body;
        String body=(prefix!=null && !prefix.isEmpty() ? prefix + "\n" : "")+(mail_config.asFile()?"":report);
        List<Uri> attachments = new ArrayList<>(InstanceCreator.create(config.getAttachmentUriProvider(), DefaultAttachmentProvider::new).getAttachments(context, config));
        if (mail_config.asFile()) {
            Uri file = createAttachmentFromString(context, mail_config.file, report);
            if (file != null) {
                attachments.add(file);
            }
        }
        return new Pair<>(body, attachments);
    }
    protected Uri createAttachmentFromString(Context context, String name, String content) {
        File cache = new File(context.getCacheDir(), name);
        try {
            writeStringToFile(cache, content);
            return AcraContentProvider.getUriForFile(context, cache);
        } catch (IOException ignored) {
            return null;
        }
    }

    private static List<ResolveInfo> queryDefaultActivities(PackageManager pm, Intent intent){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY));
        } else {
            return pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        }
    }
}

