/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl

import cloud.orbit.dsl.ast.*
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

class OrbitFileParser : OrbitBaseVisitor<Any>() {
    private val enums = mutableListOf<EnumDeclaration>()
    private val data = mutableListOf<DataDeclaration>()
    private val actors = mutableListOf<ActorDeclaration>()

    // This code is not thread safe. Need to find a way to pass the context into the visitor
    fun parse(input: String, packageName: String): CompilationUnit {

        val lexer = OrbitLexer(CharStreams.fromString(input))
        val parser = OrbitParser(CommonTokenStream(lexer))

        actors.clear()
        data.clear()

        visitFile(parser.file())

        return CompilationUnit(
            packageName,
            enums,
            data,
            actors
        )
    }

    override fun visitEnumDeclaration(ctx: OrbitParser.EnumDeclarationContext?) = enums.add(
        EnumDeclaration(
            ctx!!.name.text,
            ctx.children
                .asSequence()
                .filterIsInstance(OrbitParser.EnumMemberContext::class.java)
                .map { EnumMember(it.name.text, it.index.text.toInt()) }
                .toList()))

    override fun visitDataDeclaration(ctx: OrbitParser.DataDeclarationContext?) = data.add(
        DataDeclaration(
            ctx!!.name.text,
            ctx.children
                .asSequence()
                .filterIsInstance(OrbitParser.DataFieldContext::class.java)
                .map { DataField(it.name.text, Type(it.type.text), it.index.text.toInt()) }
                .toList()))

    override fun visitActorDeclaration(ctx: OrbitParser.ActorDeclarationContext?) = actors.add(
        ActorDeclaration(
            ctx!!.name.text,
            ctx.children
                .asSequence()
                .filterIsInstance(OrbitParser.ActorMethodContext::class.java)
                .map { m ->
                    ActorMethod(
                        name = m.name.text,
                        returnType = Type(m.returnType.text),
                        params = m.children
                            .asSequence()
                            .filterIsInstance(OrbitParser.MethodParamContext::class.java)
                            .map { p -> MethodParameter(p.name.text, Type(p.type.text)) }
                            .toList())
                }
                .toList()))
}