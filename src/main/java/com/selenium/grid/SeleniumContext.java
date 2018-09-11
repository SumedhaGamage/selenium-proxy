package com.selenium.grid;

import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.filters.RequestFilter;
import net.lightbody.bmp.mitm.CertificateAndKeySource;
import net.lightbody.bmp.mitm.KeyStoreFileCertificateSource;
import net.lightbody.bmp.mitm.manager.ImpersonatingMitmManager;
import net.lightbody.bmp.proxy.auth.AuthType;
import net.lightbody.bmp.util.HttpMessageContents;
import net.lightbody.bmp.util.HttpMessageInfo;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHeaders;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.File;
import java.net.*;
import java.util.Arrays;

public class SeleniumContext implements AutoCloseable{

    private BrowserMobProxyServer proxy;
    private WebDriver driver;

    public WebDriver getDriver(){

        String remoteHost = "http://localhost:4444/wd/hub";

        try {
            driver = new RemoteWebDriver(new URL(remoteHost), cap());
        } catch (UnknownHostException | MalformedURLException e) {
            e.printStackTrace();
        }
        return driver;
    }

    private ChromeOptions cap() throws UnknownHostException {
        ChromeOptions options = new ChromeOptions();

        DesiredCapabilities cap = new DesiredCapabilities();
        cap.setJavascriptEnabled(true);

        //Auto Accept SSL Certificates
        cap.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
        options.merge(cap);

        //Run Chrome in Headless
        options.setHeadless(true);

        //This will handle the driver maximize issue when running in Linux
        options.addArguments(Arrays.asList("--window-position=0,0"));
        options.addArguments(Arrays.asList("--window-size=1920,1080"));
        options.setProxy(initProxy());
        return options;
    }

    private Proxy initProxy() throws UnknownHostException {

        //Initializing BrowserMobProxy
        proxy = new BrowserMobProxyServer();

        //Setting up UpStream Enterprise proxy
        //Most of the Organizations use Proxy to monitor and filter internet traffic.
        //In Selenium Grid environment proxy has to set in order to access internet.

        String enterpriseProxyServerHost = "http.proxy.mydomain.com";
        int port = 8000;

        //Get the enterprise proxy server host address
        InetAddress ip = InetAddress.getByName(enterpriseProxyServerHost);
        InetSocketAddress chainSocketAddress = new InetSocketAddress(ip,port);

        //Setting the enterprise proxy as chained proxy
        proxy.setChainedProxy(chainSocketAddress);

        //#### Can set to trust all servers as follows
        //proxy.setTrustAllServers(true);


        //Start BrowserMob Proxy
        proxy.start(0);

        final String user = "dummyUser";
        String userPassword = "dummyPassword";

        //Setting chained proxy Authentication
        proxy.chainedProxyAuthorization(user,userPassword, AuthType.BASIC);

        //Setting up Certificate to pass through proxy
        String jksPath ="/tmp/path/to/dummy.jks";
        String password = "changeit";
        String alias = "alias";
        CertificateAndKeySource certificateAndKeySource = new KeyStoreFileCertificateSource(
                "JKS",
                new File(jksPath),
                alias,
                password
        );

        //Configure MitManager to use the custom Keystore source
        ImpersonatingMitmManager mitmManager = ImpersonatingMitmManager.builder()
                .rootCertificateSource(certificateAndKeySource)
                .build();

        proxy.setMitmManager(mitmManager);
        final String basicAuth = "Basic " + base64Encode(user + ":" + userPassword);


        //Manipulate selenium http calls using BrowserMobProxy

        proxy.addRequestFilter(new RequestFilter() {
            @Override
            public HttpResponse filterRequest(HttpRequest httpRequest, HttpMessageContents httpMessageContents, HttpMessageInfo httpMessageInfo) {
                httpRequest.headers().remove(HttpHeaders.USER_AGENT);
                httpRequest.headers().add(HttpHeaders.USER_AGENT, "Automation");
                httpRequest.headers().add(HttpHeaders.AUTHORIZATION, basicAuth);
                return null;
            }
        });


        //Setting BrowserMob proxy as Selenium Proxy
        Proxy seleniumProxy = ClientUtil.createSeleniumProxy(proxy,InetAddress.getLoopbackAddress());
        return seleniumProxy;
    }


    private String base64Encode(String value){
        Base64 base64 = new Base64();
        byte[] array = base64.encode(value.getBytes());
        return new String(array);
    }

    @Override
    public void close() throws Exception {
        try {
            Har har = this.proxy.getHar();
//            har.writeTo(new File("~/temp/ui.har"));
        } catch (Exception e) {
            throw new RuntimeException(e);

        } finally {
            this.proxy.stop();
            this.driver.close();
        }

    }
}
