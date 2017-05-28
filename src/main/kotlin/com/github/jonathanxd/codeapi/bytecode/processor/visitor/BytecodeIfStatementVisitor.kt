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
package com.github.jonathanxd.codeapi.bytecode.processor.visitor

import com.github.jonathanxd.codeapi.CodePart
import com.github.jonathanxd.codeapi.Types
import com.github.jonathanxd.codeapi.base.IfExpr
import com.github.jonathanxd.codeapi.base.IfGroup
import com.github.jonathanxd.codeapi.bytecode.common.MethodVisitorHelper
import com.github.jonathanxd.codeapi.bytecode.util.CodePartUtil
import com.github.jonathanxd.codeapi.bytecode.util.IfUtil
import com.github.jonathanxd.codeapi.bytecode.util.OperatorUtil
import com.github.jonathanxd.codeapi.factory.cast
import com.github.jonathanxd.codeapi.literal.Literals
import com.github.jonathanxd.codeapi.operator.Operator
import com.github.jonathanxd.codeapi.operator.Operators
import com.github.jonathanxd.codeapi.processor.CodeProcessor
import com.github.jonathanxd.iutils.data.TypedData
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes

fun visit(expressions: List<CodePart>,
          ifStart: Label,
          ifBody: Label,
          outOfIf: Label,
          isWhile: Boolean,
          data: TypedData,
          codeProcessor: CodeProcessor<*>,
          mvHelper: MethodVisitorHelper,
          nextIsOr: Boolean = false) {

    val visitor = mvHelper.methodVisitor

    var index = 0

    fun hasOr() =
            index + 1 < expressions.size
                    && expressions.slice((index + 1)..(expressions.size - 1)).takeWhile { it !is IfGroup }.any { it is Operator && it.name == Operators.OR.name }

    fun nextIsOr() =
            if (index + 1 < expressions.size)
                (expressions[index + 1]).let { it is Operator && it.name == Operators.OR.name }
            else nextIsOr // fix for ifGroup

    var orLabel: Label? = null

    while (index < expressions.size) {
        val expr = expressions[index]

        val inverse = !nextIsOr() || isWhile

        val jumpLabel = if (hasOr() && !nextIsOr()) {
            if (orLabel == null) {
                orLabel = Label()
            }
            orLabel
        } else if (isWhile) ifStart else if (inverse) outOfIf else ifBody

        if (expr is IfExpr) {
            if (index - 1 > 0 && expressions[index - 1].let { it is Operator && it.name == Operators.OR.name })
                orLabel?.let { visitor.visitLabel(it) }

            val expr1 = expr.expr1
            val operation = expr.operation
            val expr2 = expr.expr2

            genBranch(expr1, expr2, operation, jumpLabel, inverse, data, codeProcessor, mvHelper)
        }

        if (expr is IfGroup) {
            visit(expr.expressions, ifStart, ifBody, outOfIf, isWhile, data, codeProcessor, mvHelper, nextIsOr())
        }

        ++index
    }

}

fun genBranch(expr1_: CodePart, expr2_: CodePart, operation: Operator.Conditional,
              target: Label, inverse: Boolean, data: TypedData, codeProcessor: CodeProcessor<*>,
              mvHelper: MethodVisitorHelper) {

    var expr1 = expr1_
    var expr2 = expr2_

    val expr1Type = CodePartUtil.getType(expr1)
    val expr2Type = CodePartUtil.getType(expr2)

    val expr1Primitive = CodePartUtil.isPrimitive(expr1)
    val expr2Primitive = CodePartUtil.isPrimitive(expr2)

    val firstIsBoolean = expr1Primitive && CodePartUtil.isBoolean(expr1)
    val secondIsBoolean = expr2Primitive && CodePartUtil.isBoolean(expr2)

    if (firstIsBoolean || secondIsBoolean) {
        val operatorIsEq = operation === Operators.EQUAL_TO
        val value = if (firstIsBoolean) CodePartUtil.getBooleanValue(expr1) else CodePartUtil.getBooleanValue(expr2)
        var opcode = IfUtil.getIfNeEqOpcode(value)

        if (!operatorIsEq)
            opcode = IfUtil.invertIfNeEqOpcode(opcode)

        if (inverse)
            opcode = IfUtil.invertIfNeEqOpcode(opcode)

        if (firstIsBoolean) {
            codeProcessor.process(expr2::class.java, expr2, data)
            mvHelper.methodVisitor.visitJumpInsn(opcode, target)
        } else {
            codeProcessor.process(expr1::class.java, expr1, data)
            mvHelper.methodVisitor.visitJumpInsn(opcode, target)
        }

    } else {
        // Old Code ->
        // TODO: Rewrite

        if (expr1Primitive != expr2Primitive) {

            if (expr2Primitive) {
                expr1 = cast(expr1Type, expr2Type, expr1)
            } else {
                expr2 = cast(expr2Type, expr1Type, expr2)
            }
        }

        codeProcessor.process(expr1::class.java, expr1, data)

        if (expr2 === Literals.NULL) {
            mvHelper.methodVisitor.visitJumpInsn(OperatorUtil.nullCheckToAsm(operation, inverse), target)
        } else if (CodePartUtil.isPrimitive(expr1) && CodePartUtil.isPrimitive(expr2)) {
            codeProcessor.process(expr2::class.java, expr2, data)

            val firstType = CodePartUtil.getType(expr1)
            val secondType = CodePartUtil.getType(expr2)

            if (!firstType.`is`(secondType))
                throw IllegalArgumentException("'$expr1' and '$expr2' have different types, cast it to correct type.")

            var generateCMPCheck = false

            if (expr1Type.`is`(Types.LONG)) {
                mvHelper.methodVisitor.visitInsn(Opcodes.LCMP)
                generateCMPCheck = true
            } else if (expr1Type.`is`(Types.DOUBLE)) {
                mvHelper.methodVisitor.visitInsn(Opcodes.DCMPG)
                generateCMPCheck = true
            } else if (expr1Type.`is`(Types.FLOAT)) {
                mvHelper.methodVisitor.visitInsn(Opcodes.FCMPG)
                generateCMPCheck = true
            }

            var check = OperatorUtil.primitiveToAsm(operation, inverse)

            if (generateCMPCheck) {
                check = OperatorUtil.convertToSimpleIf(check)
            }

            mvHelper.methodVisitor.visitJumpInsn(check, target)
        } else {
            codeProcessor.process(expr2::class.java, expr2, data)

            mvHelper.methodVisitor.visitJumpInsn(OperatorUtil.referenceToAsm(operation, inverse), target)
        }
    }
}
