//val baseN = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
val baseN = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
// val baseN = "0123456789"

fun Int.toBase58(): String {
    val max = baseN.length
    return if (this < max) {
        "${baseN[this % max]}"
    } else {
        "${((this / max) - 1).toBase58()}${baseN[this % max]}"
    }
}

fun String.exec(): List<Int> {
    // println(this)
    return lines().filterNot { it.isBlank() }.map {
        ProcessBuilder("/bin/sh", "-c", this)
                .redirectErrorStream(true)
                .inheritIO()
                .start()
                .waitFor()
    }
}

fun qrCleanup(filename: String) {
    """
rm 'qr$filename.png'
rm 'text$filename.png'
""".exec()
}

fun qrGenerator(text: String, filename: String) {
    val realHeight = 21 * 12        // 300dpi = 118px each cm = 12px each mm
    val realWidth = (38 + 2) * 12   // 300dpi = 118px each cm = 12px each mm

    val imageHeightPx = realHeight

    val qrSize = "${imageHeightPx}x$imageHeightPx"

    val textBorderPx = imageHeightPx / 10
    val textWidthPx = imageHeightPx * 1
    val textHeightPx = imageHeightPx * 1
    val textSize = "${textWidthPx}x$textHeightPx"

    val imageFinalSize = "${realWidth}x$realHeight"

    println("QR generated: $text")

    """
qrencode -o 'qr$filename.png' -s 5 -m 4 "https://jackl.dev/home/$text"
convert 'qr$filename.png' -resize $qrSize 'qr$filename.png'
convert -size $textSize -gravity center label:'NoExp for Home' label:'$text' label:'jackl.dev/home/$text\n' -trim -bordercolor white -border $textBorderPx -append -resize $textSize -gravity center -extent $textSize 'text$filename.png'
convert +append 'text$filename.png' 'qr$filename.png' -gravity center '$filename.png'
convert '$filename.png' -resize $imageFinalSize -gravity center -extent $imageFinalSize '$filename.png'
""".exec()
}

fun qrPages(allTexts: List<String>) {
    allTexts.chunked(65).forEachIndexed { pageId, textsPage ->
        val pageName = "page_${pageId}_${textsPage.first()}"
        println("Page $pageId: $pageName")
        textsPage.chunked(5).forEachIndexed { colId, textsRow ->
            fun buildRow(rowName: String) {
                textsRow.forEachIndexed { rowId, text ->
                    if (rowId == 0) {
                        qrGenerator(text, rowName)
                    } else {
                        qrGenerator(text, "tmpRow")
                        "convert +append '$rowName.png' 'tmpRow.png' -gravity center '$rowName.png'".exec()
                    }
                }
            }

            if (colId == 0) {
                buildRow(pageName)
            } else {
                buildRow("lastRow")
                "convert -append '$pageName.png' 'lastRow.png' -gravity center '$pageName.png'".exec()
            }
        }

        qrCleanup(pageName)
    }
    qrCleanup("tmpRow")
    qrCleanup("lastRow")
    """
rm 'lastRow.png'
rm 'tmpRow.png'
""".exec()
}

if (args.size != 1) {
    System.err.println("Usage: qr4food <id>")
    kotlin.system.exitProcess(-1)
}

val qrArg = args[0]

//qrGenerator(qrArg, qrArg)
//qrCleanup(qrArg)

val allTexts = (0..6499).map { it.toBase58() }
println(allTexts.joinToString())
qrPages(allTexts)

