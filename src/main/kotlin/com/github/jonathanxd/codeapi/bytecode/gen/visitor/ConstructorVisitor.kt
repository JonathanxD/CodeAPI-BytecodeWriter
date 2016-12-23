/*
 *      CodeAPI-BytecodeWriter - Framework to generate Java code and Bytecode code. <https://github.com/JonathanxD/CodeAPI-BytecodeWriter>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2016 TheRealBuggy/JonathanxD (https://github.com/JonathanxD/ & https://github.com/TheRealBuggy/) <jonathan.scripter@programmer.net>
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
package com.github.jonathanxd.codeapi.bytecode.gen.visitor

import com.github.jonathanxd.codeapi.CodeAPI
import com.github.jonathanxd.codeapi.CodeSource
import com.github.jonathanxd.codeapi.common.CodeParameter
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass
import com.github.jonathanxd.codeapi.bytecode.util.ConstructorUtil
import com.github.jonathanxd.codeapi.gen.visit.Visitor
import com.github.jonathanxd.codeapi.gen.visit.VisitorGenerator
import com.github.jonathanxd.codeapi.interfaces.*
import com.github.jonathanxd.codeapi.util.source.CodeSourceUtil
import com.github.jonathanxd.iutils.data.MapData

object ConstructorVisitor : Visitor<ConstructorDeclaration, BytecodeClass, Any?> {

    override fun visit(t: ConstructorDeclaration, extraData: MapData, visitorGenerator: VisitorGenerator<BytecodeClass>, additional: Any?): Array<BytecodeClass> {
        var constructorDeclaration: ConstructorDeclaration = t

        val outerFields = extraData.getAllAsList(TypeVisitor.OUTER_FIELD_REPRESENTATION)

        if (!outerFields.isEmpty()) {
            val typeDeclaration = extraData.getRequired(TypeVisitor.CODE_TYPE_REPRESENTATION, "Cannot find CodeClass. Register 'TypeVisitor.CODE_TYPE_REPRESENTATION'!")

            val parameters = ArrayList<CodeParameter>(constructorDeclaration.parameters)
            var source = CodeSource.fromIterable(constructorDeclaration.body.orElse(CodeSource.empty()))

            for (outerField in outerFields) {
                parameters.add(0, CodeParameter(outerField.name, outerField.variableType))

                source = CodeSourceUtil.insertAfterOrEnd(
                        { part -> part is MethodInvocation && ConstructorUtil.isInitForThat(part) },
                        CodeAPI.sourceOfParts(
                                CodeAPI.setThisField(outerField.variableType, outerField.name,
                                        CodeAPI.accessLocalVariable(outerField.variableType, outerField.name))
                        ),
                        source)
            }

            constructorDeclaration = constructorDeclaration.setParameters(parameters).setBody(source)
        }

        visitorGenerator.generateTo(MethodDeclaration::class.java, constructorDeclaration, extraData, null, null)

        return emptyArray()
    }

}