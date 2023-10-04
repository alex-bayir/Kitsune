package org.alex.kitsune.book.views;

import org.alex.kitsune.book.Book;

import java.util.Comparator;
import static org.alex.kitsune.commons.DiffCallback.hashcode;

public class BookData {
    public final Book book;
    public final String title;
    public final String subtitle;
    public final String genres;
    public final double rating;
    public final String status;
    public final String source;
    public final String description;
    public final long size;
    public final int saved;
    public final int checked_new,not_checked_new;
    private final int hashcode;
    public BookData(Book book){
        this(book,false);
    }
    public BookData(Book book, boolean calculate_size_downloaded){
        this.book=book;
        title=book.getName();
        subtitle=book.getNameAlt();
        genres=book.getGenres();
        rating=book.getRating();
        status=book.getStatus();
        source=book.getSource();
        description=book.getDescription();
        size=calculate_size_downloaded?book.calculateImagesSize():book.getImagesSize();
        saved=book.countSaved();
        checked_new=book.getCheckedNew();
        not_checked_new=book.getNotCheckedNew();
        long tmp=(long)hashcode(title)<<2^hashcode(subtitle)>>2^hashcode(genres)^(long)rating<<4^hashcode(status)^hashcode(source)^hashcode(description)^size^(long)saved<<2^(long)checked_new<<4^(long)not_checked_new<<6;
        hashcode=(int)(tmp>>32)^(int)tmp;
    }
    public static Comparator<BookData> convert(Comparator<Book> comparator){
        return (d1, d2)->comparator.compare(d1.book,d2.book);
    }
    public BookData update(){return new BookData(book);}
    @Override
    public boolean equals(@org.jetbrains.annotations.Nullable Object obj) {
        return obj instanceof BookData data && this.book.hashCode()==data.book.hashCode();
    }
    @Override
    public int hashCode(){
        return hashcode;
    }
}
