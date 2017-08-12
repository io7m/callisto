package com.io7m.callisto.tests.prototype0;

import org.apache.commons.io.output.CountingOutputStream;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public final class CoCompressionTest
{
  private CoCompressionTest()
  {

  }

  public static void main(
    final String[] args)
    throws IOException
  {
    final Random random = new Random();
    final List<String> lines =
      Files.readAllLines(Paths.get("/usr/share/dict/cracklib-small"));

    try (final CountingOutputStream cout_u =
           new CountingOutputStream(
             Files.newOutputStream(Paths.get("/tmp/data.bin")))) {

      try (final CountingOutputStream cout_c =
             new CountingOutputStream(
               Files.newOutputStream(Paths.get("/tmp/data.bin.gz")))) {

        try (final DataOutputStream out_u = new DataOutputStream(cout_u)) {
          final Deflater def = new Deflater(9, false);
          try (final DataOutputStream out_c =
                 new DataOutputStream(new DeflaterOutputStream(cout_c, def))) {
            final int i0 = random.nextInt(lines.size());
            for (int index = 0; index < 75; ++index) {
              final int i1 = random.nextInt(lines.size());
              final int i2 = random.nextInt(lines.size());
              final String sb =
                new StringBuilder(128)
                  .append(lines.get(i0))
                  .append(".")
                  .append(lines.get(i1))
                  .append(".")
                  .append(lines.get(i2))
                  .toString();
              out_u.writeInt(index);
              out_u.writeUTF(sb);

              out_c.writeInt(index);
              out_c.writeUTF(sb);
            }
          }
        }

        System.out.println("bytes uncompressed: " + cout_u.getByteCount());
        System.out.println("bytes compressed:   " + cout_c.getByteCount());
        final double ud = (double) cout_u.getByteCount();
        final double cd = (double) cout_c.getByteCount();
        System.out.println("ratio:              " + cd / ud);
      }


    }
  }
}
