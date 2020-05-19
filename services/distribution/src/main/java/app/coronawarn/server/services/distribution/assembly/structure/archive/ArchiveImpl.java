package app.coronawarn.server.services.distribution.assembly.structure.archive;

import static app.coronawarn.server.services.distribution.assembly.structure.functional.CheckedConsumer.uncheckedConsumer;

import app.coronawarn.server.services.distribution.assembly.structure.Writable;
import app.coronawarn.server.services.distribution.assembly.structure.directory.Directory;
import app.coronawarn.server.services.distribution.assembly.structure.directory.DirectoryImpl;
import app.coronawarn.server.services.distribution.assembly.structure.file.File;
import app.coronawarn.server.services.distribution.assembly.structure.file.FileImpl;
import app.coronawarn.server.services.distribution.assembly.structure.util.ImmutableStack;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArchiveImpl extends FileImpl implements Archive {

  private static final Logger logger = LoggerFactory.getLogger(ArchiveImpl.class);

  private static final String TEMPORARY_DIRECTORY_NAME = "temporary";

  private Directory tempDirectory;

  public ArchiveImpl(String name) {
    super(name, new byte[0]);
    try {
      tempDirectory = new DirectoryImpl(
          Files.createTempDirectory(TEMPORARY_DIRECTORY_NAME).toFile());
    } catch (IOException e) {
      logger.error("Failed to create temporary directory for zip archive {}", this.getFileOnDisk());
      throw new RuntimeException(e);
    }
  }

  @Override
  public void setParent(Directory parent) {
    super.setParent(parent);
    tempDirectory.setParent(parent);
  }

  @Override
  public void addWritable(Writable writable) {
    this.tempDirectory.addWritable(writable);
  }

  @Override
  public Set<Writable> getWritables() {
    return this.tempDirectory.getWritables();
  }

  @Override
  public void prepare(ImmutableStack<Object> indices) {
    this.tempDirectory.prepare(indices);
  }

  @Override
  public byte[] getBytes() {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream);
    this.getWritables().stream()
        .filter(writable -> writable instanceof File)
        .map(file -> (File) file)
        .forEach(uncheckedConsumer(file -> {
          String pathInZip = file.getName();
          zipOutputStream.putNextEntry(new ZipEntry(pathInZip));
          byte[] bytes = file.getBytes();
          zipOutputStream.write(bytes, 0, bytes.length);
        }));
    try {
      zipOutputStream.close();
      byteArrayOutputStream.close();
    } catch (IOException e) {
      logger.error("Failed to close zip archive output stream.");
      throw new RuntimeException(e);
    }
    return byteArrayOutputStream.toByteArray();
  }

  @Override
  public void setBytes(byte[] bytes) {
    throw new RuntimeException("Can not set bytes on an archive.");
  }
}