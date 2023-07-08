package org.alex.kitsune.utils.acra;

import org.acra.config.Configuration;

import java.util.ArrayList;

public class MailSenderConfiguration implements Configuration {
    public final boolean enabled;
    public final String[] emails;

    public final String subject;
    public final String body;
    public final String file;
    public final EmailIntentSender.Formatter formatter;
    public MailSenderConfiguration(boolean enabled, String[] emails, String subject, String body, String file, EmailIntentSender.Formatter formatter){
        this.enabled=enabled;
        this.emails=emails;
        this.subject=subject;
        this.body=body;
        this.file=file;
        this.formatter=formatter;
    }

    public boolean asFile(){
        return file!=null && file.length()>0;
    }
    @Override
    public boolean enabled() {
        return enabled;
    }

    public static class Builder{
        private boolean enabled=true;
        private final ArrayList<String> emails=new ArrayList<>();
        private String subject=null;
        private String body=null;
        private String file=null;
        private EmailIntentSender.Formatter formatter=null;
        public Builder setEnabled(boolean enabled){
            this.enabled=enabled; return this;
        }
        public Builder addEmail(String email){
            this.emails.add(email); return this;
        }
        public Builder setSubject(String subject){
            this.subject=subject; return this;
        }
        public Builder setBody(String body){
            this.body=body; return this;
        }
        public Builder setFile(String file){
            this.file=file; return this;
        }
        public Builder setFormatter(EmailIntentSender.Formatter formatter){
            this.formatter=formatter; return this;
        }
        public MailSenderConfiguration build(){
            return new MailSenderConfiguration(enabled,emails.toArray(new String[0]),subject,body,file,formatter);
        }
    }
}
