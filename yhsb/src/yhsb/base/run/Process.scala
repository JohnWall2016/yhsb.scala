package yhsb.base.run

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.ByteArrayOutputStream
import java.io.BufferedInputStream

import yhsb.base.io.AutoClose.use

object process {
  private class ByteBumper(input: InputStream, output: OutputStream)
    extends Runnable {
    val in = new BufferedInputStream(input)
    val out = output

    override def run(): Unit = {
      val buf = new Array[Byte](8192)
      var continue = true
      while (continue) {
        val len = in.read(buf)
        if (len != -1) {
          out.write(buf, 0, len)
        } else {
          continue = false
        }
      }
    }
  }

  implicit class Extension(val process: Process) extends AnyVal {
    def consumeProcessOutputStream(output: OutputStream): Thread = {
      val thread = new Thread(new ByteBumper(process.getInputStream(), output))
      thread.start()
      thread
    }

    def consumeProcessErrorStream(err: OutputStream): Thread = {
      val thread = new Thread(new ByteBumper(process.getErrorStream(), err))
      thread.start()
      thread
    }

    def closeStreams() = {
      try {
        process.getErrorStream.close()
      } catch {
        case _: IOException =>
      }
      try {
        process.getInputStream.close()
      } catch {
        case _: IOException =>
      }
      try {
        process.getOutputStream.close()
      } catch {
        case _: IOException =>
      }
    }

    def waitForProcessOutput(output: OutputStream, error: OutputStream) = {
      val tout: Thread = consumeProcessOutputStream(output)
      val terr: Thread = consumeProcessErrorStream(error)
      var interrupted = false
      try {
        try {
          tout.join()
        } catch {
          case _: InterruptedException => interrupted = true
        }
        try {
          terr.join()
        } catch {
          case _: InterruptedException => interrupted = true
        }
        try {
          process.waitFor()
        } catch {
          case _: InterruptedException => interrupted = true
        }
        closeStreams()
      } finally {
        if (interrupted) Thread.currentThread().interrupt()
      }
    }
  }

  def execute(
      cmd: String,
      out: OutputStream = System.out,
      err: OutputStream = System.err
  ) = {
    Runtime.getRuntime().exec(cmd).waitForProcessOutput(out, err)
  }
}
