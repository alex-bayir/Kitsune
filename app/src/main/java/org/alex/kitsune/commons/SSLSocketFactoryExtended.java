package org.alex.kitsune.commons;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

public class SSLSocketFactoryExtended extends SSLSocketFactory {
    private String[] mCiphers;
    private String[] mProtocols;
    private SSLContext mSSLContext;

    public SSLSocketFactoryExtended() {
        try{
            initSSLSocketFactoryEx();
        }catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }

    }

    public String[] getDefaultCipherSuites() {
        return this.mCiphers;
    }

    public String[] getSupportedCipherSuites() {
        return this.mCiphers;
    }

    @Override // javax.net.ssl.SSLSocketFactory
    public Socket createSocket(Socket socket, String str, int i, boolean z) throws IOException {
        SSLSocket sSLSocket = (SSLSocket) this.mSSLContext.getSocketFactory().createSocket(socket, str, i, z);
        sSLSocket.setEnabledProtocols(this.mProtocols);
        sSLSocket.setEnabledCipherSuites(this.mCiphers);
        return sSLSocket;
    }

    @Override // javax.net.SocketFactory
    public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress2, int i2) throws IOException {
        SSLSocket sSLSocket = (SSLSocket) this.mSSLContext.getSocketFactory().createSocket(inetAddress, i, inetAddress2, i2);
        sSLSocket.setEnabledProtocols(this.mProtocols);
        sSLSocket.setEnabledCipherSuites(this.mCiphers);
        return sSLSocket;
    }

    @Override // javax.net.SocketFactory
    public Socket createSocket(String str, int i, InetAddress inetAddress, int i2) throws IOException {
        SSLSocket sSLSocket = (SSLSocket) this.mSSLContext.getSocketFactory().createSocket(str, i, inetAddress, i2);
        sSLSocket.setEnabledProtocols(this.mProtocols);
        sSLSocket.setEnabledCipherSuites(this.mCiphers);
        return sSLSocket;
    }

    @Override // javax.net.SocketFactory
    public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
        SSLSocket sSLSocket = (SSLSocket) this.mSSLContext.getSocketFactory().createSocket(inetAddress, i);
        sSLSocket.setEnabledProtocols(this.mProtocols);
        sSLSocket.setEnabledCipherSuites(this.mCiphers);
        return sSLSocket;
    }

    @Override // javax.net.SocketFactory
    public Socket createSocket(String str, int i) throws IOException {
        SSLSocket sSLSocket = (SSLSocket) this.mSSLContext.getSocketFactory().createSocket(str, i);
        sSLSocket.setEnabledProtocols(this.mProtocols);
        sSLSocket.setEnabledCipherSuites(this.mCiphers);
        return sSLSocket;
    }

    private void initSSLSocketFactoryEx() throws NoSuchAlgorithmException, KeyManagementException {
        this.mSSLContext = SSLContext.getInstance("TLS");
        this.mSSLContext.init(null, null, null);
        this.mProtocols = GetProtocolList();
        this.mCiphers = GetCipherList();
    }


    public java.lang.String[] GetProtocolList() {
        try{
            String[] values=new String[]{"TLSv1","TLSv1.1","TLSv1.2","TLSv1.3"};
            SSLSocket s=(SSLSocket)mSSLContext.getSocketFactory().createSocket();
            String[] v=s.getSupportedProtocols();
            if(s!=null){s.close();}
            ArrayList<String> list=new ArrayList();
            for(int i=0;i<values.length;i++){
                if(java.util.Arrays.binarySearch(v, values[i])>=0){
                    list.add(values[i]);
                }
            }
            return list.toArray(new String[0]);
        }catch (Exception e){
            e.printStackTrace();
            return new String[]{"TLSv1"};
        }

    }

    /* access modifiers changed from: protected */
    public String[] GetCipherList() {
        String[] supportedCipherSuites = this.mSSLContext.getSocketFactory().getSupportedCipherSuites();
        ArrayList<String> arrayList = new ArrayList<>(Arrays.asList(supportedCipherSuites));
        return arrayList.toArray(new String[0]);
    }
}
