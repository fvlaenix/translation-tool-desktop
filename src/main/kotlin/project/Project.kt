package project

import java.nio.file.Path

data class Project(
  val name: String,
  val stringPath: String,
  val data: ProjectData,
) {
  val path: Path = Path.of(stringPath)
}