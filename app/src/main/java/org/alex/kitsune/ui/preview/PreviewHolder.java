package org.alex.kitsune.ui.preview;

import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.alex.kitsune.manga.Manga;
import org.jetbrains.annotations.NotNull;

public abstract class PreviewHolder extends RecyclerView.ViewHolder {
    public PreviewHolder(@NonNull @NotNull View itemView) {super(itemView);}
    public abstract void bind(Manga manga);
}
