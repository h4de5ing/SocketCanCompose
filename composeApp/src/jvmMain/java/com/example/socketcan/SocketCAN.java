package com.example.socketcan;

import java.io.File;

public class SocketCAN {
    static {
        String libName = "libsocketcan.so";
        String tempDir = System.getProperty("java.io.tmpdir");
        File soFile = new File(tempDir, libName);
        System.out.println("加载库" + soFile.getAbsolutePath() + "," + soFile.exists());
        System.load(soFile.getAbsolutePath());
    }

    public static boolean isSupported() {
        boolean isSupported = false;
        String osName = System.getProperty("os.name").toLowerCase();
        String osArch = System.getProperty("os.arch").toLowerCase();
        if (osName.contains("linux")) {
            if (osArch.contains("aarch64")) {
                isSupported = true;
            } else if (osArch.contains("amd64")) {
                isSupported = true;
            }
        }
        return isSupported;
    }

    public static native int socketcanOpen(String can_interface);

    public static native int socketcanClose(int socket_fd);

    public static native int socketcanWrite(int socket_fd, long can_id, boolean is_extended, boolean is_remote, int data_length, byte[] data_array);

    public static native long[] socketcanRead(int socket_fd);
}