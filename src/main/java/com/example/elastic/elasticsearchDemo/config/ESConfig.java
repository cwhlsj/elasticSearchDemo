package com.example.elastic.elasticsearchDemo.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.Node;
import org.elasticsearch.client.NodeSelector;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Data
@Slf4j
@Configuration
@ConfigurationProperties("es")
public class ESConfig {


    private String host;

    private int port=9200;

    @Bean
    public RestClient getClient() {
        // 如果有多个从节点可以持续在内部new多个HttpHost，参数1是ip,参数2是HTTP端口，参数3是通信协议
        RestClientBuilder clientBuilder = RestClient.builder(new HttpHost(this.host, this.port, "http"));
        // 1.设置请求头
        Header[] defaultHeaders = {new BasicHeader("Content-Type", "application/json")};
        clientBuilder.setDefaultHeaders(defaultHeaders);
        //
        //2. 设置超时时间，多次尝试同一请求时应该遵守的超时。默认值为30秒，与默认套接字超时相同。若自定义套接字超时，则应相应地调整最大重试超时
        clientBuilder.setMaxRetryTimeoutMillis(60000);
        /**
         *3.设置失败监听器，
         *每次节点失败都可以监听到，可以作额外处理
         */
        clientBuilder.setFailureListener(new RestClient.FailureListener() {
            @Override
            public void onFailure(Node node) {
                super.onFailure(node);
                log.error(node.getName() + "==>节点失败了");
            }
        });
        /** 4.配置节点选择器，客户端以循环方式将每个请求发送到每一个配置的节点上，
         *发送请求的节点，用于过滤客户端，将请求发送到这些客户端节点，默认向每个配置节点发送，
         *这个配置通常是用户在启用嗅探时向专用主节点发送请求（即只有专用的主节点应该被HTTP请求命中）
         */
        clientBuilder.setNodeSelector(NodeSelector.SKIP_DEDICATED_MASTERS);
        /**
         *5. 配置异步请求的线程数量，Apache Http Async Client默认启动一个调度程序线程，以及由连接管理器使用的许多工作线程
         *（与本地检测到的处理器数量一样多，取决于Runtime.getRuntime().availableProcessors()返回的数量）。线程数可以修改如下,
         *这里是修改为1个线程，即默认情况
         */
        clientBuilder.setHttpClientConfigCallback(httpAsyncClientBuilder -> httpAsyncClientBuilder.setDefaultIOReactorConfig(
                IOReactorConfig.custom().setIoThreadCount(Runtime.getRuntime().availableProcessors() / 2).build()
        ));


        /**
         *6. 配置连接超时和套接字超时
         *配置请求超时，将连接超时（默认为1秒）和套接字超时（默认为30秒）增加，
         *这里配置完应该相应地调整最大重试超时（默认为30秒），即上面的setMaxRetryTimeoutMillis，一般于最大的那个值一致即60000
         */
        clientBuilder.setRequestConfigCallback(requestConfigBuilder -> {
            // 连接5秒超时，套接字连接60s超时
            return requestConfigBuilder.setConnectTimeout(5000).setSocketTimeout(60000);
        });


        /**
         *         ES配置通信加密（2018年ElasticSearch全球遭黑客攻击，ES官方解说重要数据需要配置加密）
         */
//        KeyStore truststore = KeyStore.getInstance("jks");
//        try (InputStream is = Files.newInputStream(keyStorePath)) {
//            truststore.load(is, keyStorePass.toCharArray());
//        }
//        SSLContextBuilder sslBuilder = SSLContexts.custom().loadTrustMaterial(truststore, null);
//        final SSLContext sslContext = sslBuilder.build();
//        clientBuilder.setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
//            @Override
//            public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
//                return httpClientBuilder.setSSLContext(sslContext);
//            }
//        });

        /**
         * RestClientBuilder中的自定义节点选择器的需求配置（–>大数据中是要求配置机架感知的）
         */
        // 进行详细的配置
//        clientBuilder.setNodeSelector(new NodeSelector() {
//            // 设置分配感知节点选择器，允许选择本地机架中的节点（如果有），否则转到任何机架中的任何其他节点。
//            @Override
//            public void select(Iterable<Node> nodes) {
//                boolean foundOne = false;
//                for (Node node: nodes) {
//                    String rackId = node.getAttributes().get("rack_id").get(0);
//                    if ("rack_one".equals(rackId)) {
//                        foundOne = true;
//                        break;
//                    }
//                }
//                if (foundOne) {
//                    Iterator<Node> nodesIt = nodes.iterator();
//                    while (nodesIt.hasNext()) {
//                        Node node = nodesIt.next();
//                        String rackId = node.getAttributes().get("rack_id").get(0);
//                        if ("rack_one".equals(rackId) == false) {
//                            nodesIt.remove();
//                        }
//                    }
//                }
//            }
//        });

        /**
         * 配置ES安全认证（如果你的ES集群设置了密码的话，或者密钥的话）
         */
        /**如果ES设置了密码，那这里也提供了一个基本的认证机制，下面设置了ES需要基本身份验证的默认凭据提供程序*/
//        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
//        credentialsProvider.setCredentials(AuthScope.ANY,
//                new UsernamePasswordCredentials("user", "password"));
//        clientBuilder.setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
//            @Override
//            public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
//                return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
//            }
//        });
//        /**上面采用异步机制实现抢先认证，这个功能也可以禁用，这意味着每个请求都将在没有授权标头的情况下发送，然后查看它是否被接受，
//         并且在收到HTTP 401响应后，它再使用基本认证头重新发送完全相同的请求，这个可能是基于安全、性能的考虑*/
//        clientBuilder.setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
//            @Override
//            public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
//                // 禁用抢先认证的方式
//                httpClientBuilder.disableAuthCaching();
//                return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
//            }
//        });

        // 最后配置好的clientBuilder再build一下即可得到真正的Client
        return clientBuilder.build();
    }
}
