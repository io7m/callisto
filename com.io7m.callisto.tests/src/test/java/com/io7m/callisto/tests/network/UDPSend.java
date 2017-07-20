package com.io7m.callisto.tests.network;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class UDPSend
{
  private UDPSend()
  {
  }

  public static void main(
    final String[] args)
    throws Exception
  {
    final byte[] data =
      Files.readAllBytes(Paths.get("/tmp/hello.txt"));

    try (final DatagramSocket s = new DatagramSocket()) {
      for (int index = 0; index < 100; ++index) {
        s.send(new DatagramPacket(
          data,
          0,
          data.length,
          new InetSocketAddress("::1", 9999)));
      }
    }
  }
}
