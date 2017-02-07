# APromise
A simple promise library for android.

Usage
-----
    new Promise<>(new Function<String>() {
        @Override
        public void function(Resolver<String> resolver) throws Exception {
            resolver.fulfill("Hola!");
        }
    }).onThen(new Callback<String>() {
        @Override
        public void callback(String result) throws Exception {
            Log.d(TAG, "callback: " + result);
        }
    });

    Promise.resolve("Ciao").onThen(new Callback<String>() {
        @Override
        public void callback(String result) throws Exception {
            throw new Exception("You can throw Exception at callback.");
        }
    }).onCatch(new Callback<Exception>() {
        @Override
        public void callback(Exception result) throws Exception {
            Log.d(TAG, "callback: " + result);
        }
    });

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
