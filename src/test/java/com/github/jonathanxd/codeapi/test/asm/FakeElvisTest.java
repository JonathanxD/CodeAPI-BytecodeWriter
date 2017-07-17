/*
 *      CodeAPI-BytecodeWriter - Framework to generate Java code and Bytecode code. <https://github.com/JonathanxD/CodeAPI-BytecodeWriter>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2017 TheRealBuggy/JonathanxD (https://github.com/JonathanxD/ & https://github.com/TheRealBuggy/) <jonathan.scripter@programmer.net>
 *      Copyright (c) contributors
 *
 *
 *      Permission is hereby granted, free of charge, to any person obtaining a copy
 *      of this software and associated documentation files (the "Software"), to deal
 *      in the Software without restriction, including without limitation the rights
 *      to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *      copies of the Software, and to permit persons to whom the Software is
 *      furnished to do so, subject to the following conditions:
 *
 *      The above copyright notice and this permission notice shall be included in
 *      all copies or substantial portions of the Software.
 *
 *      THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *      IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *      FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *      AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *      LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *      OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *      THE SOFTWARE.
 */
package com.github.jonathanxd.codeapi.test.asm;

import com.github.jonathanxd.codeapi.base.TypeDeclaration;
import com.github.jonathanxd.codeapi.test.FakeElvisTest_;
import com.github.jonathanxd.codeapi.test.InstanceOf_;
import com.github.jonathanxd.iutils.annotation.Named;
import com.github.jonathanxd.iutils.exception.RethrowException;

import org.junit.Assert;
import org.junit.Test;

public class FakeElvisTest {

    @Test
    public void fakeElvisTest() {
        TypeDeclaration $ = FakeElvisTest_.$();
        @Named("Instance") Object test = CommonBytecodeTest.test(this.getClass(), $);
        try {
            FakeElvisTest_.TestClass testClass = (FakeElvisTest_.TestClass) test.getClass()
                    .getDeclaredMethod("test", String.class).invoke(test, (Object) null);

            FakeElvisTest_.TestClass testClass2 = (FakeElvisTest_.TestClass) test.getClass()
                    .getDeclaredMethod("test", String.class).invoke(test, "X");

            Assert.assertEquals("", testClass.getS());
            Assert.assertEquals("X", testClass2.getS());
        } catch (Exception e) {
            throw new RethrowException(e);
        }
    }

}
