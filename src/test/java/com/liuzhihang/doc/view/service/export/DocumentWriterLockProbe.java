package com.liuzhihang.doc.view.service.export;

import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Separate JVM used to hold the writer lock, without an IDE or shared static state. */
public final class DocumentWriterLockProbe {
    public static void main(String[] args) throws Exception {
        try (var channel = FileChannel.open(Path.of(args[0]), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             var lock = channel.lock()) {
            System.out.println("READY");
            System.out.flush();
            System.in.read();
        }
    }
}
