package org.alex.kitsune.commons;

import android.view.View;

public interface HolderListener {
    void onItemClick(View v, int index);
    boolean onItemLongClick(View v, int index);
}
