package org.alex.kitsune.utils.acra;

import com.alex.json.java.JSON;
import org.acra.ReportField;
import org.acra.data.CrashReportData;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

public class JSONFormatter implements EmailIntentSender.Formatter, Serializable {

    int spaces;
    public JSONFormatter(int spaces){
        this.spaces=spaces;
    }
    @Override
    public String format(CrashReportData data, List<ReportField> order, String mainJoiner, String subJoiner, Boolean urlEncode) throws Exception {
        JSON.Object json=new JSON.Object();
        for(ReportField field:order){
            try{
                json.put(field.name(),JSON.json(data.getString(field)));
            }catch (IOException|NumberFormatException e){
                json.put(field.name(),data.getString(field));
            }
        }
        return json.json(spaces);
    }
}
