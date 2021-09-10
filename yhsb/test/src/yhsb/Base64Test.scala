package yhsb

import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path

import org.apache.axis.encoding.Base64
import utest.TestSuite
import utest.Tests
import utest.test
import yhsb.base.run.process
import yhsb.base.util.Config

object Base64Test extends TestSuite {
  def tests =
    Tests {
      test("str2img") {
        val imgStr  = """iVBORw0KGgoAAAANSUhEUgAAAHgAAAB4CAIAAAC2BqGFAAABiklEQVR42u3ayRHDIAwAQPpvOinB\nY6MDNKtvYgaWB0Jo/URJLASgQQvQoEGLRuhVFQ+Te/PnV0PVrwg0aNCgQV8OHXn4bozc9W3IyKBB\ngwYNehb0zikciBWYwBSsCDRo0KBBg/4EHXgVBg0aNGjQoC+BPuRX0KBBgwYN+tR6dGACo/APGjRo\n0KBfQpf19RySV8xvCQMNGjRo0M/QXZH3sNu/NNCgQYMGPQo6sGMoMEXp6p8CDRo0aNBDoTPuoEdV\nnHe2EDRo0KBBg+57UT0kRfm2ItCgQYMGfTl016Tz3mrrcxLQoEGDBn0bdF55Ou98L5OtK/yDBg0a\nNOhroLsK0HlYGRV20KBBgwZ9G/Sqiry7b9fDLmjQoEGDHgrdlVfkUZbtN2jQoEGDHgqdl1cEYh2y\nK6BBgwYNGnTJgR5erQ7svQINGjRo0KAze20L2osChwINGjRo0LOgu+rRedf3vOdm0KBBgwY9Bbp+\nWvvpTdnjbGTWARo0aNCgD4IW4QEaNGgBGjRoBKBHxR9evn83MX61BQAAAABJRU5ErkJggg=="""

        val imgBytes = Base64.decode(imgStr.replace("\\n", ""))

        val file = Files.createTempFile("yhsb_login", ".jpg")
        Files.write(file, imgBytes)

        val cmd =
          s"""${Config.load("cmd").getString("open.img")} $file"""

        println(cmd)
        process.execute(
          cmd,
          OutputStream.nullOutputStream(),
          OutputStream.nullOutputStream()
        )
        
        //Console.in.readLine()
      }
    }
}
