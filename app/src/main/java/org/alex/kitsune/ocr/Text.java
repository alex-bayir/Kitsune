package org.alex.kitsune.ocr;

import android.graphics.Rect;
import androidx.annotation.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Math.*;
import static java.lang.Math.max;

public class Text {
    private String text;
    private List<Group> groups;

    public Text(List<Group> groups){
        this(getText(groups),groups);
    }
    public Text(String hash,List<Group> groups){
        this.text=hash;
        this.groups=groups;
    }

    public String getText(){
        return text;
    }
    public List<Group> getGroups(){
        return groups;
    }
    public static Text create(List<com.google.mlkit.vision.text.Text.TextBlock> blocks){
        return new Text(Group.createGroups(clearBlocks(blocks)));
    }

    @Override
    public boolean equals(@Nullable @org.jetbrains.annotations.Nullable Object obj) {
        return obj instanceof Text && this.hashCode()==obj.hashCode();
    }

    @Override
    public int hashCode() {
        return text.hashCode();
    }

    public boolean isTranslated(){
        return groups.size()==0 || groups.stream().anyMatch(Group::isTranslated);
    }
    public static String getText(List<Group> blocks){
        return blocks.stream().map(Group::getText).collect(Collectors.joining("\n\n"));
    }
    public static class Group {
        private final String text;
        private String translated;
        private final List<com.google.mlkit.vision.text.Text.TextBlock> blocks;
        private final Rect boundingBox;
        public Group(String text, List<com.google.mlkit.vision.text.Text.TextBlock> blocks){
            this.text=text;
            this.blocks=blocks;
            boundingBox=calculateRect(blocks);
        }
        public Group(List<com.google.mlkit.vision.text.Text.TextBlock> blocks){
            this(getText(blocks),blocks);
        }
        public String getText(){
            return text;
        }
        public List<com.google.mlkit.vision.text.Text.TextBlock> getTextBlocks(){
            return blocks;
        }
        public Rect getBoundingBox(){
            return boundingBox;
        }

        public boolean isTranslated(){
            return translated!=null;
        }
        public String setTranslated(String translated){
            this.translated=translated; return translated;
        }
        public String getTranslated(){
            return translated;
        }
        public static List<Group> createGroups(List<com.google.mlkit.vision.text.Text.TextBlock> blocks){
            List<Group> groups=new LinkedList<>();
            if(blocks.size()>0){
                com.google.mlkit.vision.text.Text.TextBlock last=blocks.get(0);
                List<com.google.mlkit.vision.text.Text.TextBlock> group=new LinkedList<>();
                for(com.google.mlkit.vision.text.Text.TextBlock block:blocks){
                    Rect l=last.getBoundingBox();
                    Rect n=block.getBoundingBox();
                    assert l != null && n != null;
                    if (hypot((n.centerX()-l.centerX()),n.centerY()-l.centerY())>hypot(l.width()/2d,l.height())) {
                        if(group.size()>0){
                            groups.add(new Group(group));
                        }
                        group = new LinkedList<>();
                    }
                    group.add(block);
                    last=block;
                }
                if(group.size()>0){
                    groups.add(new Group(group));
                }
            }
            return groups;
        }

        public static String getText(List<com.google.mlkit.vision.text.Text.TextBlock> blocks){
            return blocks.stream().map(com.google.mlkit.vision.text.Text.TextBlock::getText).collect(Collectors.joining(" "));
        }
        public static Rect calculateRect(List<com.google.mlkit.vision.text.Text.TextBlock> blocks){
            Rect rect=new Rect(blocks.get(0).getBoundingBox());
            for(com.google.mlkit.vision.text.Text.TextBlock block:blocks){
                Rect br=block.getBoundingBox();
                rect.left=min(rect.left,br.left);
                rect.top=min(rect.top,br.top);
                rect.right=max(rect.right,br.right);
                rect.bottom=max(rect.bottom,br.bottom);
            }
            return rect;
        }
    }
    private static List<com.google.mlkit.vision.text.Text.TextBlock> clearBlocks(List<com.google.mlkit.vision.text.Text.TextBlock> blocks){
        return blocks.stream().filter(b->b.getText().length()>1).collect(Collectors.toList());
    }
}
