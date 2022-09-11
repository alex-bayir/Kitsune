package org.alex.kitsune.ui.reader;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.LruCache;

public class ImageCache extends LruCache <String, Bitmap> {
    /**
     * @param maxSize for caches that do not override {@link #sizeOf}, this is
     *                the maximum number of entries in the cache. For all other caches,
     *                this is the maximum sum of the sizes of the entries in this cache.
     */
    public ImageCache(int maxSize){super(maxSize);}
    public Bitmap getFromMemory(String key){return this.get(key);}
    public boolean addToMemory(String key,Bitmap bitmap){
        if(key!=null && bitmap!=null && getFromMemory(key)==null){
            this.put(key, bitmap);
            return true;
        }
        return false;
    }
    public boolean addToMemory(String key, Drawable drawable){
        return drawable instanceof BitmapDrawable && addToMemory(key, ((BitmapDrawable) drawable).getBitmap());
    }
    public BitmapDrawable getFromMemoryDrawable(String key){
        Bitmap bitmap=getFromMemory(key);
        return bitmap!=null ? new BitmapDrawable(bitmap) : null;
    }

}
