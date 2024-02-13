package br.dev.contrib.gov.sus.opendata.jobs

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, FileUtil, Path}

import java.net.URI

object FileOps {

  def copy(src: URI, dst: URI, conf: Configuration): Boolean =
    copy(src, dst, false, conf)

  def copyOverwriting(src: URI, dst: URI, conf: Configuration): Boolean =
    copy(src, dst, true, conf)

  def copy(
      src: URI,
      dst: URI,
      overwrite: Boolean,
      conf: Configuration
  ): Boolean = {
    val srcPath = new Path(src)
    val srcFS = srcPath.getFileSystem(conf)

    val dstPath = new Path(dst)
    val dstFS = dstPath.getFileSystem(conf)

    FileUtil.copy(
      srcFS,
      srcPath,
      dstFS,
      dstPath,
      false,
      overwrite,
      conf
    )
  }

  def exists(src: URI, conf: Configuration): Boolean = {
    val srcPath = new Path(src)
    srcPath.getFileSystem(conf).exists(srcPath)
  }
}
