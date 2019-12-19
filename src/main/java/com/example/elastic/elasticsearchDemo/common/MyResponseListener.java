package com.example.elastic.elasticsearchDemo.common;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;

import java.util.concurrent.CountDownLatch;
@Slf4j
@Data
public class MyResponseListener implements ResponseListener {

    private Response response;

    private CountDownLatch latch ;

    public MyResponseListener(CountDownLatch latch) {
        this.latch = latch;
    }
    public MyResponseListener(){

    }

    @Override
    public void onSuccess(Response response) {
        this.response=response;
        log.info("onSuccess: {}",response.getEntity());
        latch.countDown();
    }

    @Override
    public void onFailure(Exception e) {
        log.error("onFailure",e);
        latch.countDown();

    }
}
