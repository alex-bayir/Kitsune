package org.alex.kitsune.commons;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public class ListSet<E> implements List<E>,Set<E> {
    final List<Map.Entry<Integer,E>> map;

    public ListSet(List<Map.Entry<Integer,E>> map){this.map=map; map.clear();}
    public ListSet(List<Map.Entry<Integer,E>> map, Collection<E> c){this(map); addAll(c);}
    public ListSet(Class<? extends List> listClass) throws IllegalAccessException,InstantiationException{
        this.map=listClass.newInstance();
    }
    public ListSet(Class<? extends List<E>> listClass,Collection<E> c) throws IllegalAccessException,InstantiationException{
        this(listClass);
        addAll(c);
    }


    @Override
    public int size(){return map.size();}
    @Override
    public boolean isEmpty(){return map.size()==0;}
    @Override
    public boolean contains(@Nullable @org.jetbrains.annotations.Nullable Object o){return get(o)!=null;}

    private boolean equals(Object o1,Object o2){return o1==null ? o2==null : (o1==o2 || o1.equals(o2));}
    private boolean equals(Object o,Map.Entry<Integer,E> entry){return o==null ? entry.getValue()==null : entry.getKey().equals(o.hashCode());}

    public Map.Entry<Integer,E> get(Object o){
        for(Map.Entry<Integer,E> entry:map){
            if(equals(o,entry)){return entry;}
        }
        return null;
    }

    @NonNull
    @NotNull
    @Override
    public Iterator<E> iterator() {
        return new Itr();
    }
    @Override
    public E get(int index) {
        return map.get(index).getValue();
    }
    @Override
    public boolean add(E e) {
         return add(e,size(),false)==-1;
    }
    @Override
    public void add(int index, E e) {
        add(e,index,true);
    }

    public int add(E e,boolean moveIfExist){return add(size(),e,moveIfExist);}
    public int add(int index,E e,boolean moveIfExist){
        return e!=null ? add(e, index, moveIfExist) : -1;
    }
    public int add(E e,int index,boolean moveIfExist){
        if(e!=null){
            Map.Entry<Integer,E> entry=get(e);
            int old=entry!=null ? map.indexOf(entry) : -1;
            if(entry!=null){
                entry.setValue(e);
                if(moveIfExist && index!=old){map.remove(entry); map.add(index,entry);}
            }else{
                map.add(index,new AbstractMap.SimpleEntry<>(e.hashCode(), e));
            }
            return old;
        }else{
            throw new NullPointerException("Element can't be null");
        }
    }

    @Override
    public E set(int index, E e) {
        return set(index,e,false);
    }
    public E set(int index,E e,boolean moveIfExist){
        if(e!=null){
            Map.Entry<Integer,E> entry=get(e);
            E old=moveIfExist ? entry.getValue() : map.get(index).getValue();
            if(entry!=null){
                map.remove(entry);
                if(moveIfExist){
                    map.add(index,entry);
                }else{
                    map.set(index,entry);
                }
            }else{
                map.set(index,new AbstractMap.SimpleEntry<>(e.hashCode(),e));
            }
            return old;
        }
        return null;
    }

    @Override public boolean remove(@Nullable @org.jetbrains.annotations.Nullable Object o) {
        return map.remove(get(o));
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public E remove(int index) {
        return map.remove(index).getValue();
    }

    @Override
    public int indexOf(@Nullable @org.jetbrains.annotations.Nullable Object o) {
        ListIterator<E> it=listIterator(0);
        while (it.hasNext()){
            if(equals(o,it.next())){return it.nextIndex()-1;}
        }
        return -1;
    }

    @Override
    public int lastIndexOf(@Nullable @org.jetbrains.annotations.Nullable Object o) {
        ListIterator<E> it=listIterator(size());
        while (it.hasPrevious()){
            if(equals(o,it.previous())){return it.previousIndex()+1;}
        }
        return -1;
    }

    @NonNull
    @NotNull
    @Override
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(new Itr(),size(),Spliterator.DISTINCT);
    }

    @Override
    public boolean addAll(int index, @NonNull @NotNull Collection<? extends E> c) {
        boolean changed=false;
        for(E e:c){
            if(add(index,e,false)==-1){index++; changed=true;}
        }
        return changed;
    }

    @Override
    public boolean containsAll(@NonNull @NotNull Collection<?> c) {
        for(Object o:c){
            if(!contains(o)){return false;}
        }
        return true;
    }

    @Override
    public boolean addAll(@NonNull @NotNull Collection<? extends E> c) {
        return addAll(size(),c);
    }

    @Override
    public boolean retainAll(@NonNull @NotNull Collection<?> c) {
        return removeIf(e -> !c.contains(e));
    }

    @Override
    public boolean removeAll(@NonNull @NotNull Collection<?> c) {
        return removeIf(c::contains);
    }

    @Override
    public void replaceAll(@NonNull @NotNull UnaryOperator<E> operator) {
        for(Map.Entry<Integer,E> entry:map){
            entry.setValue(operator.apply(entry.getValue()));
        }
    }

    @Override
    public void sort(@Nullable @org.jetbrains.annotations.Nullable Comparator<? super E> c) {
        map.sort(c==null ? null : (o1, o2) -> c.compare(o1.getValue(), o2.getValue()));
    }

    @NonNull
    @NotNull
    @Override
    public ListIterator<E> listIterator() {
        return new Itr();
    }

    @NonNull
    @NotNull
    @Override
    public ListIterator<E> listIterator(int index) {
        return new Itr(index);
    }

    @NonNull
    @NotNull
    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        ListSet<E> list=new ListSet<>(new ArrayList<>(Math.abs(toIndex-fromIndex)));
        ListIterator<E> it=listIterator(fromIndex);
        while (it.hasNext() && it.nextIndex()<=toIndex){
            list.add(it.next());
        }
        return list;
    }

    @NonNull
    @NotNull
    @Override
    public Object[] toArray() {
        Object[] arr=new Object[size()];
        Iterator<E> it=iterator();
        for(int i=0;i<arr.length;i++){
            if(!it.hasNext()){return Arrays.copyOf(arr,i);}
            arr[i]=it.next();
        }
        return arr;
    }

    @NonNull
    @NotNull
    @Override
    public <T> T[] toArray(@NonNull @NotNull T[] a) {
        int size=size();
        T[] arr = a.length >= size ? a : (T[])java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
        Iterator<E> it=iterator();
        for(int i=0;i<arr.length;i++){
            if(!it.hasNext()){
                if(a==arr){
                    arr[i]=null;
                }else if(a.length<i){
                    return Arrays.copyOf(arr,i);
                }else{
                    System.arraycopy(arr, 0, a, 0, i);
                    if (a.length > i) {
                        a[i] = null;
                    }
                }
                return a;
            }
            arr[i]=(T)it.next();
        }
        return arr;
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    @NonNull
    @NotNull
    @Override
    public String toString() {
        Iterator<E> it = iterator();
        if (!it.hasNext()){
            return "[]";
        }

        StringBuilder sb = new StringBuilder().append('[');
        for (;;) {
            E e=it.next();
            sb.append(e == this ? "(this Collection)" : e);
            if (! it.hasNext()){
                return sb.append(']').toString();
            }
            sb.append(',').append(' ');
        }
    }

    @Override
    public boolean equals(@Nullable @org.jetbrains.annotations.Nullable Object obj) {
        if(obj instanceof List){
            return equals((List<E>) obj);
        }else if(obj instanceof Set){
            return equals((Set<E>) obj);
        }
        return false;
    }
    public boolean equals(List<E> list){
        if(list!=null){
            if(this==list){return true;}
            Iterator<E> it1=iterator(),it2=(list).iterator();
            while (it1.hasNext() && it2.hasNext()){
                if(!equals(it1.next(),it2.next())){return false;}
            }
            return !(it1.hasNext() || it2.hasNext());
        }
        return false;
    }
    public boolean equals(Set<E> set){
        if(set!=null){
            return this==set || (size()==set.size() && containsAll(set));
        }
        return false;
    }
    public boolean equals(Map<Integer,E> map){
        if(map!=null){
            return size()==map.size() && containsAll(map.entrySet());
        }
        return false;
    }

    private class Itr implements ListIterator<E>{
        int cursor;
        int size=size();
        public Itr(){this(0);}
        public Itr(int start){
            this.cursor=start-1;
        }

        @Override
        public boolean hasNext() {
            return cursor<size-1;
        }

        @Override
        public E next() {
            return ListSet.this.get(++cursor);
        }

        @Override
        public void remove() {
            if(cursor>=0){
                ListSet.this.remove(cursor--);
                size--;
            }
        }

        @Override
        public void forEachRemaining(@NonNull @NotNull Consumer<? super E> action) {
            ListIterator.super.forEachRemaining(action);
        }

        @Override
        public boolean hasPrevious() {
            return cursor>0;
        }

        @Override
        public E previous() {
            return map.get(--cursor).getValue();
        }

        @Override
        public int nextIndex() {
            return cursor+1;
        }

        @Override
        public int previousIndex() {
            return cursor-1;
        }

        public int currentIndex(){return cursor;}

        @Override
        public void set(E e) {
            ListSet.this.set(cursor,e,true);
        }

        @Override
        public void add(E e) {
            size+=ListSet.this.add(cursor+1,e,true)==-1 ? 1:0;
        }
    }
}
