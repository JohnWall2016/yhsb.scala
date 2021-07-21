package yhsb.base

import org.zeroturnaround.zip.ZipUtil
import java.io.File

import yhsb.base.io.Path._

package object zip {
  def packDir[T : PathConvertible, S : PathConvertible](dir: T, zipFile: S) = {
    ZipUtil.pack(dir.toFile(), zipFile.toFile())
  }

  def addFile[T : PathConvertible, S : PathConvertible](zipFile: T, file: S) = {
    ZipUtil.packEntry(file.toFile(), zipFile.toFile())
  }
}

