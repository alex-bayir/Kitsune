package org.alex.kitsune.ui.main.scripts;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;
import org.alex.kitsune.R;
import org.alex.kitsune.commons.HolderMenuItemClickListener;
import org.alex.kitsune.scripts.Script;
import org.alex.kitsune.ui.main.Constants;
import org.alex.kitsune.commons.HolderListener;
import java.io.File;

public class ScriptHolder extends RecyclerView.ViewHolder{
    public static final int REMOVE=0;
    EditText name;
    TextView status,domain,version,date;
    ImageView img;
    View menu;
    private final PopupMenu popupMenu;
    public ScriptHolder(ViewGroup parent,View.OnFocusChangeListener l,HolderListener holderListener, HolderMenuItemClickListener menuListener) {
        super(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_script,parent,false));
        name=itemView.findViewById(R.id.name);
        status=itemView.findViewById(R.id.status);
        domain=itemView.findViewById(R.id.domen);
        version=itemView.findViewById(R.id.version);
        date=itemView.findViewById(R.id.date);
        img=itemView.findViewById(R.id.img);

        menu=itemView;

        popupMenu=new PopupMenu(menu.getContext(), menu,Gravity.START,R.style.Widget_AppCompat_PopupMenu, R.style.Widget_AppCompat_PopupMenu);
        popupMenu.getMenu().add(0,REMOVE, Menu.NONE,R.string.remove).setIcon(R.drawable.ic_bookmark_remove_black).getIcon().setTint(0xffffffff);
        popupMenu.setOnMenuItemClickListener(item -> menuListener!=null && menuListener.onMenuItemClick(getBindingAdapterPosition(),item));
        popupMenu.setForceShowIcon(true);

        //popupMenu.setGravity(Gravity.END);
        itemView.setOnClickListener(v -> holderListener.onItemClick(v,getBindingAdapterPosition()));
        itemView.setOnLongClickListener(v-> {if(v==menu){popupMenu.show();} return holderListener.onItemLongClick(v,getBindingAdapterPosition());});
        name.setOnEditorActionListener((v, actionId, event) -> {
            if(actionId==EditorInfo.IME_ACTION_DONE){v.clearFocus();}
            return true;
        });
        name.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after){}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count){}
            @Override public void afterTextChanged(Editable s){
                name.setError(Script.checkSuffix(s.toString()) ? null:"The script needs a right suffix");
            }
        });
        name.setOnFocusChangeListener(l);

    }

    public void bind(File file){
        date.setText(getDate(file.lastModified()));
        try{
            Script script=Script.getInstance(file);
            name.setText(script.getName());
            domain.setText("Domain: "+script.getString(Constants.source,"?"));
            status.setText("Status: work");
            img.setImageDrawable(img.getContext().getDrawable(script.getLanguageIconId()));
            version.setText("Version: "+script.getString(Constants.version,"?"));
        }catch (Throwable e){
            name.setText(file.getName());
            status.setText("Status: Have compilation errors");
            domain.setText("Domain: ?");
            version.setText("Version: ?");
            img.setImageDrawable(img.getContext().getDrawable(R.drawable.ic_code_file));
        }
    }
    public static String getDate(long date){return date>0 ? new java.text.SimpleDateFormat("dd.MM.yyyy").format(new java.util.Date(date)) : null;}
}
