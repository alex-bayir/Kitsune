package org.alex.kitsune.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ScrambledInterceptor implements Interceptor {
    final int RC4_WIDTH = 256;
    private final int PIECE_SIZE = 200;
    private final HashMap<Integer,int[]> memo = new HashMap<>();

    @NotNull
    public Response intercept(@NotNull Interceptor.Chain chain) throws IOException {
        Request request = chain.request();
        Response response = chain.proceed(request);
        ResponseBody body = response.body();
        if (body!=null) {
            try{
                byte[] image = this.descramble(body.byteStream());
                body = ResponseBody.Companion.create(image, MediaType.Companion.get("image/jpeg"));
                return response.newBuilder().body(body).build();
            }catch (ShortBufferException e){
                e.printStackTrace();
            }
        }
        return response;
    }
    private byte[] descramble(InputStream is) throws ShortBufferException {
        Bitmap bitmap=BitmapFactory.decodeStream(is);
        int width=bitmap.getWidth();
        int height=bitmap.getHeight();
        Bitmap result=Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(result);
        List<Rect> pieces=new ArrayList<>();
        for(int y=0;y<height;y+=PIECE_SIZE){
            for(int x=0;x<width;x+=PIECE_SIZE){
                pieces.add(new Rect(x, y, Math.min(x+PIECE_SIZE, width), Math.min(y+PIECE_SIZE, height)));
            }
        }
        Map<Integer,List<Rect>> groups=pieces.stream().collect(Collectors.groupingBy(piece -> piece.width()<<16|piece.height()));
        for(List<Rect> group: groups.values()){
            int size=group.size();
            int[] permutation=memo.get(size);
            if(permutation==null){
                SeedRandom random=new SeedRandom("staystay");
                List<Integer> indexes=IntStream.range(0,size).boxed().collect(Collectors.toList());
                permutation=new int[size];
                for(int i=0;i<size;i++){
                    permutation[i]=indexes.remove((int)(random.nextDouble()*indexes.size()));
                }
                memo.put(size,permutation);
            }
            for(int i=0;i<permutation.length;i++){
                canvas.drawBitmap(bitmap,group.get(i),group.get(permutation[i]),null);
            }
        }
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        result.compress(Bitmap.CompressFormat.JPEG,90,output);
        return output.toByteArray();
    }
    private static class Piece{
        public int x;
        public int y;
        public int w;
        public int h;
        public Piece(int x,int y,int w,int h){
            this.x=x;
            this.y=y;
            this.w=w;
            this.h=h;
        }
    }
    private class SeedRandom{
        private final byte[] input=new byte[RC4_WIDTH];
        private final byte[] buffer=new byte[RC4_WIDTH];
        private int pos=RC4_WIDTH;
        private final Cipher rc4;
        public SeedRandom(String key){
            try{
                rc4=Cipher.getInstance("RC4");
                rc4.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key.getBytes(),"RC4"));
                rc4.update(input,0,RC4_WIDTH,buffer); // RC4-drop[256]
            }catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | ShortBufferException e){
                throw new RuntimeException(e);
            }
        }
        public Double nextDouble() throws ShortBufferException {
            long num=nextByte();
            int exp=8;
            while(num<1L<<52){
                num=num<<8|nextByte();
                exp+=8;
            }
            while(num>=1L<<53){
                num=num>>1;
                exp--;
            }
            return Math.scalb((double) num,-exp);
        }
        public long nextByte() throws ShortBufferException {
            if(pos==RC4_WIDTH){
                rc4.update(input,0,RC4_WIDTH,buffer);
                pos=0;
            }
            return (long)buffer[pos++] & 0xFF;
        }
    }
}
