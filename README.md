# APromise
A simple promise library for android.

Install
-------
```build.gradle
dependencies {
    compile 'jp.rubi3.apromise:apromise:0.15.0'
}
```

Usage
-----
```Example.java
Promise<String> hola = new Promise<>(new Function<String>() {
    @Override
    public void function(@NonNull Resolver<String> resolver) throws Exception {
        resolver.fulfill("Hola!");
    }
}).thenCallback(new Callback<String>() {
    @Override
    public void callback(String result) throws Exception {
        Log.d(TAG, "callback: " + result);
    }
});

Promise<String> ciao = Promise.resolve("Ciao").thenCallback(new Callback<String>() {
    @Override
    public void callback(String result) throws Exception {
        Log.d(TAG, "callback: " + result);
        throw new Exception("You can throw Exception in callback.");
    }
}).catchPipe(new PipeNonNull<Exception, String>() {
    @Nullable
    @Override
    public Promise<String> pipe(@NonNull Exception result) throws Exception {
        return Promise.resolve("Bonjour!");
    }
});

Promise<List<String>> promise = Promise.all(Arrays.asList(hola, ciao));
promise.finallyCallback(new CallbackNonNull<Promise<List<String>>>() {
    @Override
    public void callback(@NonNull Promise<List<String>> result) throws Exception {
        List<String> hello = result.getResult();
        //noinspection ConstantConditions
        Log.d(TAG, "callback: " + hello.toString());
    }
});
```

License
-------

MIT License

Copyright (c) 2016 Ryo Kikuchi

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
