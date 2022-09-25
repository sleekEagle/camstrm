package com.example.camstrm;

public class ImageData {
    int seq,height,width,byte_len;
    float fdist;
    byte[] data;
    public ImageData(int seq,int height,int width, int byte_len, float fdist, byte[] data){
        this.seq=seq;
        this.height=height;
        this.width=width;
        this.byte_len=byte_len;
        this.data=data;
        this.fdist=fdist;
    }
}
