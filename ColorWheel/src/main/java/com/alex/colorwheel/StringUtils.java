package com.alex.colorwheel;

public class StringUtils{
    public static String parsehexstr(String str){
        for(int i=0;i<str.length();i++){
            char c=str.charAt(i);
            if(!(('0'<=c && c<='9')||('a'<=c && c<='f')||('A'<=c && c<='F'))){str=str.replace(""+c,"");}
        }
    return str;}

    public static int HexStringToInt(String s){
        int result=0; char c=0; int k=0; s=s.replace("0x","");
        for(int i=0;i<s.length() && k<8;i++){
            c=s.charAt(i);
            if(c>='0' && c<='9'){c-='0';}else{if(c>='a' && c<='f'){c+=0xa-'a';}else{if(c>='A' && c<='F'){c+=0xa-'A';}else{c=0xFF;}}}
            if(c!=0xFF){result=(result<<4)+c;k++;}
        }
        while(k<8){result=result<<4; k++;}
    return result;}

    public static String toHexString(int value, String prefix){
        if(prefix==null){prefix="";} char c;
        for(int i=0;i<8;i++){c=(char)((value>>((7-i)<<2))&0x0F); prefix+=(char)(c+((c<0xa) ? '0' : ('A'-10)));}
    return prefix;}
}
