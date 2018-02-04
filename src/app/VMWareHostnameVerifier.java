package app;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;


public class VMWareHostnameVerifier implements HostnameVerifier {

    @Override
    public boolean verify(String hostname, SSLSession session) {

        return true;
    }
}