package kotlinx.atomicfu.transformer

import org.mozilla.javascript.*
import org.mozilla.javascript.ast.*
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileReader
import java.lang.String.format

class AtomicFUTransformerJS(
    var inputDir: File,
    var outputDir: File
) {
    private val p = Parser(CompilerEnvirons())

    private operator fun File.div(child: String) =
        File(this, child)

    private fun File.toOutputFile(): File =
        outputDir / relativeTo(inputDir).toString()

    private var logger = LoggerFactory.getLogger(AtomicFUTransformer::class.java)

    private fun info(message: String, sourceInfo: SourceInfo? = null) {
        logger.info(format(message, sourceInfo))
    }

    private fun File.mkdirsAndWrite(outBytes: ByteArray) {
        parentFile.mkdirs()
        writeBytes(outBytes) // write resulting bytes
    }

    fun transform() {
        var inpFilesTime = 0L
        var outFilesTime = 0L
        inputDir.walk().filter { it.isFile }.forEach { file ->
            inpFilesTime = inpFilesTime.coerceAtLeast(file.lastModified())
            //if (file.isClassFile()) analyzeFile(file)
            outFilesTime = outFilesTime.coerceAtLeast(file.toOutputFile().lastModified())
        }
        if (inpFilesTime > outFilesTime || outputDir == inputDir) {
            // perform transformation
            info("Transforming to $outputDir")
            inputDir.walk().filter { it.isFile }.forEach { file ->
                val bytes = file.readBytes()
                val outBytes = transformFile(file, bytes)
                val outFile = file.toOutputFile()
                outFile.mkdirsAndWrite(outBytes)
            }
        } else {
            info("Nothing to transform -- all classes are up to date")
        }
    }

    private fun transformFile(file: File, bytes: ByteArray): ByteArray {
        val root = p.parse(FileReader(file), null, 0)
        val tv = TransformVisitor()
        root.visit(tv)
        return root.toSource().toByteArray()
    }

    inner class TransformVisitor : NodeVisitor {

        override fun visit(node: AstNode): Boolean {
            //clear atomic constructors from classes fields
            if (node is FunctionCall) {
                val functionName = node.target.toSource()
                //TODO determine atomic call
                if (functionName.length > 5 && functionName.substring(0, 6) == "atomic") {
                    if (node.parent is Assignment) {
                        val valueNode = node.arguments[0]
                        (node.parent as InfixExpression).setRight(valueNode)
                    }
                    return true
                }
            }

            if (node.type == Token.GETPROP) {
                if ((node as PropertyGet).property.toSource() == "kotlinx\$atomicfu\$value") {
                    // this.field.value
                    if (node.target.type == Token.GETPROP) {
                        val clearField = node.target as PropertyGet
                        val targetNode = clearField.target
                        val clearProperety = clearField.property
                        node.setLeftAndRight(targetNode, clearProperety)
                    }
                    // other cases -- poor
                    else if (node.parent is InfixExpression) {
                        val parent = node.parent as InfixExpression
                        if (parent.left == node) {
                            parent.left = node.target
                        } else if (parent.right == node) {
                            parent.right = node.target
                        }
                    } else if (node.parent is ReturnStatement) {
                        (node.parent as ReturnStatement).returnValue = node.target
                    } else if (node.parent is VariableInitializer) {
                        (node.parent as VariableInitializer).initializer = node.target
                    }
                }
            }
            return true
        }
    }
}

fun main(args: Array<String>) {
//    if (args.size !in 1..2) {
//        println("Usage: AtomicFUTransformerKt <dir> [<output>]")
//        return
//    }
    val t = AtomicFUTransformerJS (File("/home/jetbrains/kotlinx.atomicfu/atomicfu-js/build/classes/kotlin/test/atomicfu-js_test.js"), File("/home/jetbrains/kotlinx.atomicfu/atomicfu-js/build/classes/kotlin/test/atomicfu-js_testTransformed.js"))
    //if (args.size > 1) t.outputDir = File(args[1])
    t.transform()
}