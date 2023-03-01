package org.alex.kitsune.ocr;

import android.graphics.Rect;
import com.google.mlkit.vision.text.Text;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.hypot;

public class TextGroup {
    private final String text;
    private String translated;
    private final List<Text.TextBlock> blocks;
    private final Rect boundingBox;
    public TextGroup(String text, List<Text.TextBlock> blocks){
        this.text=text;
        this.blocks=blocks;
        boundingBox=calculateRect(blocks);
    }
    public TextGroup(List<Text.TextBlock> blocks){
        this(getText(blocks),blocks);
    }
    public String getText(){
        return text;
    }
    public List<Text.TextBlock> getTextBlocks(){
        return blocks;
    }
    public Rect getBoundingBox(){
        return boundingBox;
    }
    public String setTranslated(String translated){
        this.translated=translated; return translated;
    }
    public String getTranslated(){
        return translated;
    }
    public static List<TextGroup> createGroups(List<Text.TextBlock> blocks){
        List<TextGroup> groups=new LinkedList<>();
        Text.TextBlock last=blocks.get(0);
        List<Text.TextBlock> group=new LinkedList<>();
        for(Text.TextBlock block:blocks){
            Rect l=last.getBoundingBox();
            Rect n=block.getBoundingBox();
            assert l != null && n != null;
            if (hypot((n.centerX()-l.centerX()),n.centerY()-l.centerY())>hypot(l.width()/2d,l.height())) {
                if(group.size()>0){
                    groups.add(new TextGroup(group));
                }
                group = new LinkedList<>();
            }
            group.add(block);
            last=block;
        }
        if(group.size()>0){
            groups.add(new TextGroup(group));
        }
        return groups;
    }

    public static String getText(List<Text.TextBlock> blocks){
        return blocks.stream().map(Text.TextBlock::getText).collect(Collectors.joining(" "));
    }
    public static Rect calculateRect(List<Text.TextBlock> blocks){
        Rect rect=new Rect(blocks.get(0).getBoundingBox());
        for(Text.TextBlock block:blocks){
            Rect br=block.getBoundingBox();
            rect.left=min(rect.left,br.left);
            rect.top=min(rect.top,br.top);
            rect.right=max(rect.right,br.right);
            rect.bottom=max(rect.bottom,br.bottom);
        }
        return rect;
    }
}
