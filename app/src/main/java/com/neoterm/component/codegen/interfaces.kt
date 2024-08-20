package com.neoterm.component.codegen

abstract class CodeGenerator {
  abstract fun getGeneratorName(): String
  abstract fun generateCode(codeGenObject: CodeGenObject): String
}

interface CodeGenObject {
  fun getCodeGenerator(parameter: CodeGenParameter): CodeGenerator
}
