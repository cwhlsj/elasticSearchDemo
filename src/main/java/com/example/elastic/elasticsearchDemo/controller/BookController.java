package com.example.elastic.elasticsearchDemo.controller;

import com.example.elastic.elasticsearchDemo.bean.Book;
import com.example.elastic.elasticsearchDemo.common.MyResponseListener;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/book")
public class BookController {

    @Resource
    private RestClient client;

    @Resource
    private Gson gson;


    @GetMapping(value = "/go")
    public ResponseEntity<String> go() {
        return new ResponseEntity<>("go", HttpStatus.OK);
    }

    /**
     * 同步执行HTTP请求
     * @return
     * @throws IOException
     */
    @GetMapping(value = "/es")
    public ResponseEntity<String> getEsInfo() throws IOException {
        // 构造HTTP请求，第一个参数是请求方法，第二个参数是服务器的端点，host默认是http://localhost:9200
        Request request = new Request("GET", "/");
        // 设置其他一些参数比如美化json
        request.addParameter("pretty", "true");
//        // 设置请求体
//        request.setEntity(new NStringEntity("{\"json\":\"text\"}", ContentType.APPLICATION_JSON));
//        // 还可以将其设置为String，默认为ContentType为application/json
//        request.setJsonEntity("{\"json\":\"text\"}");

        /*
        performRequest是同步的，将阻塞调用线程并在请求成功时返回Response，如果失败则抛出异常
        内部属性可以取出来通过下面的方法
         */
        Response response = client.performRequest(request);
//        // 获取请求行
//        RequestLine requestLine = response.getRequestLine();
//        // 获取host
//        HttpHost host = response.getHost();
//        // 获取状态码
//        int statusCode = response.getStatusLine().getStatusCode();
//        // 获取响应头
//        Header[] headers = response.getHeaders();
        // 获取响应体
        String responseBody = EntityUtils.toString(response.getEntity());
        return new ResponseEntity<>(responseBody, HttpStatus.OK);
    }
    /**
     * 异步执行HTTP请求
     * @return
     */
    @GetMapping("/es/asyn")
    public ResponseEntity<String> asynchronous() {
        Request request = new Request(
                "GET",
                "/");
        client.performRequestAsync(request, new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
                log.info("异步执行HTTP请求并成功");
            }

            @Override
            public void onFailure(Exception exception) {
                log.info("异步执行HTTP请求并失败");
            }
        });
        log.info("异步执行HTTP请求完成");
        return null;
    }

    /**
     * 添加ES对象, Book的ID就是ES中存储的document的ID，所以最好不要为空，自定义生成的ID太浮夸
     *
     * @return ResponseEntity
     * @throws IOException
     */
    @PostMapping(value = "/add")
    public ResponseEntity<String> add(@RequestBody Book book) throws IOException {
        // 构造HTTP请求，第一个参数是请求方法，第二个参数是服务器的端点，host默认是http://localhost:9200，
        // endpoint直接指定为index/type的形式
        Request request = new Request("POST", "/book/book/" + book.getId());
        // 设置其他一些参数比如美化json
        request.addParameter("pretty", "true");

//        Map<String,Object> map = new HashMap<String,Object>(book);
//        log.info(map.toString());
        // 设置请求体并指定ContentType，如果不指定默认为APPLICATION_JSON
        String json = gson.toJson(book);
        request.setJsonEntity(json);
//        request.setEntity(new NStringEntity(json, ContentType.APPLICATION_JSON));

        // 发送HTTP请求
        Response response = client.performRequest(request);

        // 获取响应体, id: AWXvzZYWXWr3RnGSLyhH
        String responseBody = EntityUtils.toString(response.getEntity());
        return new ResponseEntity<>(responseBody, HttpStatus.OK);
    }


    /**
     * 并行异步执行HTTP请求，批量增加
     */
    @PostMapping("/parallAddOrUpdate")
    public ResponseEntity<String> parallAddOrUpdate(@RequestBody Book[] documents) throws IOException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(documents.length);
        List<MyResponseListener> list=new ArrayList<>(documents.length);
        for (Book document : documents) {
            Request request = new Request("POST", "/book/book/" + document.getId());
            //let's assume that the documents are stored in an HttpEntity array
            request.setJsonEntity(gson.toJson(document));

            MyResponseListener myResponseListener = new MyResponseListener(latch);
            client.performRequestAsync(request, myResponseListener);
            list.add(myResponseListener);


        }
        log.info("wait");
        latch.await();
        log.info("done");
        List<String> collect = list.stream().map(item -> {
            try {
                return EntityUtils.toString(item.getResponse().getEntity());
            } catch (IOException e) {
                log.error("error",e);
                e.printStackTrace();
            }
            return "";
        }).collect(Collectors.toList());


        log.info(gson.toJson(collect));

        return new ResponseEntity<>(gson.toJson(collect), HttpStatus.OK);
    }

    /**
     * 根据id获取ES对象
     *
     * @param id
     * @return
     * @throws IOException
     */
    @GetMapping(value = "/{id}")
    public ResponseEntity<String> getBookById(@PathVariable("id") String id) throws IOException {
        Request request = new Request("GET", "/book/book/" + id);
        // 添加json返回优化
        request.addParameter("pretty", "true");
        Response response;
        String responseBody;
        response = client.performRequest(request);
        responseBody = EntityUtils.toString(response.getEntity());

        return new ResponseEntity<>(responseBody, HttpStatus.OK);
    }


    /**
     * 根据id更新Book
     *
     * @param id
     * @param book
     * @return
     */
    @PutMapping(value = "/{id}")
    public ResponseEntity<String> updateBook(@PathVariable("id") String id, @RequestBody Book book) throws IOException, JSONException {
        // 构造HTTP请求
        Request request = new Request("POST", "/book/book/" + id + "/_update");
        request.addParameter("pretty", "true");

        // 将数据丢进去，这里一定要外包一层“doc”，否则内部不能识别
        Map<String,Object> map = new HashMap<String,Object>();
        map.put("doc", book);
        request.setJsonEntity(gson.toJson(map));

        // 执行HTTP请求
        Response response = client.performRequest(request);

        // 获取返回的内容
        String responseBody = EntityUtils.toString(response.getEntity());

        return new ResponseEntity<>(responseBody, HttpStatus.OK);
    }

    /**
     * 使用脚本更新Book
     * @param id
     * @param
     * @return
     * @throws IOException
     */
    @PutMapping(value = "/update2/{id}")
    public ResponseEntity<String> updateBook2(@PathVariable("id") String id, @RequestParam("name") String name) throws IOException {
        // 构造HTTP请求
        Request request = new Request("POST", "/book/book/" + id + "/_update");
        request.addParameter("pretty", "true");

        Map<String,Object> map = new HashMap<>();
        // 创建脚本语言，如果是字符变量，必须加单引号
        StringBuilder op1 = new StringBuilder("ctx._source.name=").append("'").append(name).append("'");
        map.put("script", op1);

        request.setJsonEntity(gson.toJson(map));

        // 执行HTTP请求
        Response response = client.performRequest(request);

        // 获取返回的内容
        String responseBody = EntityUtils.toString(response.getEntity());

        return new ResponseEntity<>(responseBody, HttpStatus.OK);
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<String> deleteById(@PathVariable("id") String id) throws IOException {
        Request request = new Request("DELETE", "/book/book/" + id);
        request.addParameter("pretty", "true");
        // 执行HTTP请求
        Response response = client.performRequest(request);
        // 获取结果
        String responseBody = EntityUtils.toString(response.getEntity());

        return new ResponseEntity<>(responseBody, HttpStatus.OK);
    }
    @GetMapping("/query")
    public ResponseEntity<String> query() throws IOException {
        Request request = new Request("GET", "/book/book/_search");
        // 添加json返回优化
//        request.addParameter("pretty", "true");
//        request.
        Response response;
        String responseBody;
        response = client.performRequest(request);
        responseBody = EntityUtils.toString(response.getEntity());

        return new ResponseEntity<>(responseBody, HttpStatus.OK);
    }
    @GetMapping("/queryMatch")
    public ResponseEntity<String> queryMatch() throws IOException {
        Request request = new Request("GET", "/book/book/_search");
        // 添加json返回优化
        request.addParameter("from", "0");
        request.addParameter("size", "2");

//        request.

        String queryString="{" +
                "    \"query\": {" +
                "        \"match\": {" +
                "            \"name\": \"火\"" +
                "        }" +
                "    }" +
                "}";
        request.setJsonEntity(queryString);

        Response response;
        String responseBody;
        response = client.performRequest(request);
        responseBody = EntityUtils.toString(response.getEntity());

        return new ResponseEntity<>(responseBody, HttpStatus.OK);
    }
}
