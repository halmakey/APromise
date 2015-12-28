package jp.rubi3.apromise;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.test.ApplicationTestCase;

import java.util.concurrent.CountDownLatch;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
    public ApplicationTest() {
        super(Application.class);
    }

    public void testThen() throws Throwable {
        final StringBuilder builder = new StringBuilder();
        final CountDownLatch latch = new CountDownLatch(1);

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Promise.resolved("ABC").then(new Callback<String>() {
                    @Override
                    public void callback(String result) {
                        builder.append(result);
                    }
                }).then(new Filter<String, Integer>() {
                    @Override
                    public Integer filter(String result) {
                        return result.length();
                    }
                }).then(new Pipe<Integer, StringBuilder>() {
                    @Override
                    public Promise<StringBuilder> pipe(Integer result) {
                        builder.append(result);
                        return Promise.resolved(builder);
                    }
                }).then(new Callback<StringBuilder>() {
                    @Override
                    public void callback(StringBuilder result) {
                        result.append(result.toString());
                    }
                }).then(new Callback<StringBuilder>() {
                    @Override
                    public void callback(StringBuilder result) {
                        result.append("1");
                    }
                }).catche(new Callback<Exception>() {
                    @Override
                    public void callback(Exception result) {
                        builder.append("X");
                    }
                }).all(new Callback<Object>() {
                    @Override
                    public void callback(Object result) {
                        latch.countDown();
                    }
                });
            }
        });


        latch.await();

        assertEquals("ABC3ABC31", builder.toString());

    }

    public void testSuccessCallback() throws Exception {
        final StringBuilder builder = new StringBuilder();
        final CountDownLatch latch = new CountDownLatch(1);

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Promise.dispatch(new Dispatch<StringBuilder>() {
                    @Override
                    public StringBuilder dispatch() throws Exception {
                        return builder.append('1');
                    }
                }).then(new Callback<StringBuilder>() {
                    @Override
                    public void callback(StringBuilder result) {
                        builder.append('2');
                    }
                }).then(null, new Callback<Exception>() {
                    @Override
                    public void callback(Exception result) throws Exception {
                        builder.append(result.getClass().getSimpleName());
                    }
                }).then(new Callback<StringBuilder>() {
                    @Override
                    public void callback(StringBuilder result) {
                        builder.append('3');
                        latch.countDown();
                    }
                });
            }
        });
        latch.await();

        assertEquals("123", builder.toString());
    }

    public void testRejected() throws Throwable {
        final StringBuilder builder = new StringBuilder();
        final CountDownLatch latch = new CountDownLatch(1);

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Promise.rejected(new Exception("Hello!")).then(new Callback() {
                    @Override
                    public void callback(Object result) {
                        builder.append("X");
                    }
                }).catche(new Callback<Throwable>() {
                    @Override
                    public void callback(Throwable result) {
                        builder.append(result.getMessage());
                        result.printStackTrace();
                    }
                }).all(new Callback<Object>() {
                    @Override
                    public void callback(Object result) {
                        latch.countDown();
                    }
                });
            }
        });


        latch.await();
        assertEquals("Hello!", builder.toString());

    }

    public void testThrow() throws Throwable {
        final StringBuilder builder = new StringBuilder();
        final CountDownLatch latch = new CountDownLatch(1);

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Promise.resolved(null).then(new Callback<Object>() {
                    @Override
                    public void callback(Object result) {
                        throw new RuntimeException("RuntimeException!");
                    }
                }).catche(new Callback<Exception>() {
                    @Override
                    public void callback(Exception result) {
                        builder.append(result.getMessage());
                        latch.countDown();
                    }
                });
            }
        });

        latch.await();
        assertEquals("RuntimeException!", builder.toString());
    }

}