package app;

import com.vmware.vim25.*;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.*;
import javax.net.ssl.HttpsURLConnection;
import javax.xml.ws.BindingProvider;

public class App {

    static VimPortType vimPort;
    static ServiceContent serviceContent;
    static Scanner scan = new Scanner(System.in);

    public static void main(String[] args) throws UnrecoverableKeyException, KeyManagementException {

        String certificate = "D:/2017/virtualisation/esx.keystore";
        String certificatePwd = "password";
        String ip = "192.168.99.10";
        String login = "root";
        
        System.out.println("Saisir un mot de passe :");
        String password = scan.nextLine();

        KeyStore ks;
        try {
            ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(new FileInputStream(certificate), certificatePwd.toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("sunx509");
            kmf.init(ks, certificatePwd.toCharArray());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("sunx509");
            tmf.init(ks);

            SSLContext sc = SSLContext.getInstance("SSL");
            SSLSessionContext sslsc = sc.getServerSessionContext();
            sslsc.setSessionTimeout(0);
            sc.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            HttpsURLConnection.setDefaultHostnameVerifier(new VMWareHostnameVerifier());

            ManagedObjectReference serviceInstance = new ManagedObjectReference();
            serviceInstance.setType("ServiceInstance");
            serviceInstance.setValue("ServiceInstance");
            VimService vimService = new VimService();
            vimPort = vimService.getVimPort();

            Map<String, Object> ctxt = ((BindingProvider) vimPort).getRequestContext();
            ctxt.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, "https://" + ip +
                    "/sdk/vimService");
            ctxt.put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);

            serviceContent = vimPort.retrieveServiceContent(serviceInstance);
            vimPort.login(serviceContent.getSessionManager(), login, password, null);
            System.out.println(serviceContent.getAbout().getApiVersion());
            //HostSystem();
            VirtualMachine();


        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (RuntimeFaultFaultMsg runtimeFaultFaultMsg) {
            runtimeFaultFaultMsg.printStackTrace();
        } catch (InvalidLocaleFaultMsg invalidLocaleFaultMsg) {
            invalidLocaleFaultMsg.printStackTrace();
        } catch (InvalidLoginFaultMsg invalidLoginFaultMsg) {
            invalidLoginFaultMsg.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    private static ObjectSpec getObjectSpec(ManagedObjectReference mor) {
        ObjectSpec oSpec = new ObjectSpec();
        oSpec.setObj(mor);
        oSpec.setSkip(true);
        TraversalSpec tSpec = new TraversalSpec();
        tSpec.setName("traverseEntities");
        tSpec.setPath("view");
        tSpec.setSkip(false);
        tSpec.setType("ContainerView");
        oSpec.getSelectSet().add(tSpec);
        return oSpec;
    }

    private static List<ObjectContent> retrievePropertiesAllObjects(List<PropertyFilterSpec>
                                                                            listpfs) throws Exception {
        RetrieveOptions propObjectRetrieveOpts = new RetrieveOptions();
        propObjectRetrieveOpts.setMaxObjects(Integer.MAX_VALUE);
        List<ObjectContent> listobjcontent = new ArrayList<ObjectContent>();
        RetrieveResult rslts =
                vimPort.retrievePropertiesEx(serviceContent.getPropertyCollector(), listpfs,
                        propObjectRetrieveOpts);
        if (rslts != null && rslts.getObjects() != null && !rslts.getObjects().isEmpty()) {
            listobjcontent.addAll(rslts.getObjects());
        }
        String token = null;
        if (rslts != null && rslts.getToken() != null) {
            token = rslts.getToken();
        }
        while (token != null && !token.isEmpty()) {
            rslts =
                    vimPort.continueRetrievePropertiesEx(serviceContent.getPropertyCollector(), token);
            token = null;
            if (rslts != null) {
                token = rslts.getToken();
                if (rslts.getObjects() != null && !rslts.getObjects().isEmpty()) {
                    listobjcontent.addAll(rslts.getObjects());
                }
            }
        }
        return listobjcontent;
    }

    public static void HostSystem (){
        PropertySpec property = new PropertySpec();

        property.setType("HostSystem");
        property.getPathSet().add("summary");

        PropertyFilterSpec filter = new PropertyFilterSpec();

        try {
            filter.getObjectSet().add(getObjectSpec(vimPort.createContainerView(serviceContent.getViewManager(), serviceContent.getRootFolder(), Arrays.asList("HostSystem"), true)));
        } catch (RuntimeFaultFaultMsg runtimeFaultFaultMsg) {
            runtimeFaultFaultMsg.printStackTrace();
        }

        filter.getPropSet().add(property);

        List<PropertyFilterSpec> pFilterSpecArr = new ArrayList<PropertyFilterSpec>();
        pFilterSpecArr.add(filter);
        try {
            for (ObjectContent oc : retrievePropertiesAllObjects(pFilterSpecArr)) {
                HostListSummary hostSummary = null;
                String parentValue = null;
                List<DynamicProperty> dps = oc.getPropSet();
                if (dps != null) {
                    for (DynamicProperty dp : dps) {
                        if (dp.getName().equals("summary")) {
                            hostSummary = (HostListSummary) dp.getVal();
                            System.err.println(hostSummary.getConfig().getName());
                            System.err.println(hostSummary);
                            System.out.println(hostSummary.getConfig().getName());
                            System.out.println(hostSummary.getHost());
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void VirtualMachine() {

        PropertySpec property = new PropertySpec();

        property.setType("VirtualMachine");
        property.getPathSet().add("summary");
        property.getPathSet().add("parent");
        property.getPathSet().add("config");
        property.getPathSet().add("storage");

        PropertyFilterSpec filter = new PropertyFilterSpec();

        try {
            filter.getObjectSet().add(getObjectSpec(vimPort.createContainerView(serviceContent.getViewManager(), serviceContent.getRootFolder(), Arrays.asList("VirtualMachine"), true)));
        } catch (RuntimeFaultFaultMsg runtimeFaultFaultMsg) {
            runtimeFaultFaultMsg.printStackTrace();
        }

        filter.getPropSet().add(property);

        List<PropertyFilterSpec> pFilterSpecArr = new ArrayList<PropertyFilterSpec>();
        pFilterSpecArr.add(filter);
        try {
            for (ObjectContent oc : retrievePropertiesAllObjects(pFilterSpecArr)) {
                VirtualMachineSummary vmSummary = null;
                ManagedObjectReference vmParent = null;
                String parentValue = null;
                List<DynamicProperty> dps = oc.getPropSet();
                if (dps != null) {
                    for (DynamicProperty dp : dps) {
                        if (dp.getName().equals("summary")) {
                            vmSummary = (VirtualMachineSummary) dp.getVal();
                            System.out.println("VirtualMachine----------------");
                            System.out.println("OS : " + vmSummary.getConfig().getGuestFullName());
                            System.out.println("Etat : " + vmSummary.getRuntime().getPowerState());
                            System.out.println("ID : " + vmSummary.getVm().getValue());
                            //System.out.println("Storage : " +vmSummary.getStorage());
                            //System.out.println(vmSummary.getConfig());
                        }
                        if (dp.getName().equals("parent")) {
                            vmParent = (ManagedObjectReference) dp.getVal();
                            System.out.println("parent : " + vmParent.getValue());
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
