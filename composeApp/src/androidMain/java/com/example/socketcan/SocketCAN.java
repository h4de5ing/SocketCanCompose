package com.example.socketcan;

public class SocketCAN {
    static {
        System.loadLibrary("socketcan");
    }

    public static native int socketcanOpen(String can_interface);

    public static native int socketcanClose(int socket_fd);

    public static native int socketcanWrite(int socket_fd, long can_id, boolean is_extended, boolean is_remote, int data_length, byte[] data_array);

    public static native long[] socketcanRead(int socket_fd);
}